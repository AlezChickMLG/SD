package libraryApp

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.net.*
import java.nio.Buffer
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class TeacherMicroservice {
    private lateinit var messageManagerSocket: Socket
    private lateinit var teacherMicroserviceServerSocket: ServerSocket
    private lateinit var heartbeatSocket: Socket
    private lateinit var databaseSocket: Socket

    private lateinit var questionDatabase: MutableList<Pair<String, String>>

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val questionID = AtomicInteger(0)

    //response queue
    private val responseQueue = ConcurrentHashMap<Int, MutableList<String>>()
    private val responseQueueMutex = Mutex()

    //database queue
    private val databaseQueue = LinkedBlockingQueue<String>()
    private val databaseQueueMutex = Mutex()

    companion object Constants {
        // pentru testare, se foloseste localhost. pentru deploy, server-ul socket (microserviciul MessageManager) se identifica dupa un "hostname"
        // acest hostname poate fi trimis (optional) ca variabila de mediu
        val MESSAGE_MANAGER_HOST = System.getenv("MESSAGE_MANAGER_HOST") ?: "localhost"
        val HEARTBEAT_HOST = System.getenv("HEARTBEAT_HOST") ?: "localhost"
        const val MESSAGE_MANAGER_PORT = 1500
        const val TEACHER_PORT = 1600
        const val HEARTBEAT_PORT = 1900
        const val DATABASE_PORT = 2100
    }

    init {
        val databaseLines: List<String> = File("questions_database.txt").readLines()
        questionDatabase = mutableListOf()

        for (i in 0..(databaseLines.size - 1) step 2) {
            questionDatabase.add(Pair(databaseLines[i], databaseLines[i + 1]))
        }
    }

    private fun subscribeToMessageManager() {
        try {
            messageManagerSocket = Socket(MESSAGE_MANAGER_HOST, MESSAGE_MANAGER_PORT)
            sendInitMessage()
            println("M-am conectat la MessageManager!")
        } catch (e: Exception) {
            println("Nu ma pot conecta la MessageManager!")
            exitProcess(1)
        }
    }

    private fun sendInitMessage() {
        messageManagerSocket.getOutputStream().write("Init:teacher\n".toByteArray())
    }

    private fun connectToHeartbeat() {
        heartbeatSocket = Socket(HEARTBEAT_HOST, HEARTBEAT_PORT)
    }

    private fun sendInitMessageToHeartbeat() {
        heartbeatSocket.getOutputStream().write("Init:teacher\n".toByteArray())
    }

    private fun connectToDatabase() {
        databaseSocket = Socket("localhost", DATABASE_PORT)
    }

    private fun sendMeanRequest(studentName: String) {
        val request = "getAll:$studentName"
        databaseQueue.add(request)
    }

    private fun listenToHeartbeat() {
        val reader = BufferedReader(InputStreamReader(heartbeatSocket.inputStream))

        coroutineScope.launch {
            while (true) {
                try {
                    val ping = reader.readLine() ?: withContext(Dispatchers.IO) {
                        heartbeatSocket.close()
                        reader.close()
                        exitProcess(1)
                    }

                    val (messageType, microservice) = ping.split(":")
                    if (messageType == "Ping") {
                        heartbeatSocket.getOutputStream().write("Pong:teacher\n".toByteArray())
                        //println("Am raspuns heartbeatului")
                    }

                } catch (e: java.lang.Exception) {
                    println("Eroare la procesarea pingului")
                    withContext(Dispatchers.IO) {
                        heartbeatSocket.close()
                        reader.close()
                        exitProcess(1)
                    }
                }
            }
        }
    }

    private fun listenToDatabase() {
        val reader = BufferedReader(InputStreamReader(databaseSocket.inputStream))

        while (true) {
            val result = reader.readLine()

            if (result == null) {
                println("Database a fost inchis")
                databaseSocket.close()
                break
            }

            coroutineScope.launch {
                processDatabaseResult(result)
            }
        }
    }

    private suspend fun processDatabaseResult(result: String) {
        try {
            val (messageType, student, result) = result.split(":")

            when {
                messageType.startsWith("getAll") -> {
                    if (result.toFloatOrNull() != null)
                        println("$student are media $result")
                    else
                        println("$student nu are inca note")
                }
            }
        } catch (e: Exception) {
            println("Eroare la procesarea rezultatului: $result")
        }
    }

    private suspend fun processRequest(request: String) {
        println("Request received: $request")
        val (messageType, messageSource, messageDestination, messageBody) = request.split(" ", limit = 4)
        when {
            messageType.startsWith("intrebare") -> {
                val response = questionDatabase.find {
                    it.first == messageBody
                }

                val id = messageType.substring("intrebare".length)

                if (response != null) {
                    withContext(Dispatchers.IO) {
                        messageManagerSocket.getOutputStream()
                            .write("raspuns$id $messageDestination ${response.second}\n".toByteArray())
                    }
                    println("Am trimis catre $messageDestination raspunsul: ${response.second}")
                }
            }

            messageType.startsWith("raspuns") -> {
                try {
                    val question_id = messageType.substring("raspuns".length).toInt()
                    responseQueueMutex.withLock {
                        responseQueue[question_id]?.add(request)
                    }
                } catch (e: Exception) {
                    println("Eroare la adaugarea raspunsului in coada: $e")
                }
            }

            messageType.startsWith("terminare") -> {
                try {
                    val studentName = messageSource
                    println("$studentName a fost terminat")
                    sendMeanRequest(studentName)

                } catch (e: Exception) {
                    println("Eroare la procesarea mesajului de tip terminare")
                }
            }
        }
    }

    private suspend fun processResponses(question: String, id: Int) {
        val responses = responseQueueMutex.withLock {
            responseQueue[id]
        }

        if (responses == null) {
            println("Nimeni nu a raspuns la intrebarea: $question")
        }

        else {
            responses.forEach {
                val (_, studentName, _, response) = it.split(" ", limit = 4)
                val grade = if (response != "Nu stiu")
                    10
                else
                    4

                val addRequest = "add:$studentName | $question | $grade"

                databaseQueueMutex.withLock {
                    databaseQueue.add(addRequest)
                }
            }
        }

        responseQueueMutex.withLock {
            responseQueue.remove(id)
        }
    }

    private fun listenToRequests() {
        val reader = BufferedReader(InputStreamReader(messageManagerSocket.inputStream))
        while (true) {
            try {
                val request = reader.readLine()

                if (request == null) {
                    messageManagerSocket.close()
                    println("Message manager s-a deconectat")
                    break
                }

                coroutineScope.launch {
                    processRequest(request)
                }
            } catch (e: Exception) {
                println("Eroare la listenToRequest")
                break
            }
        }
    }

    private fun sendRequestsToDatabase() {
        while (true) {
            val request = databaseQueue.poll(500, TimeUnit.MILLISECONDS)

            if (request != null) {
                databaseSocket.getOutputStream().write("$request\n".toByteArray())
                println("Am trimis catre baza de date cererea: $request")
            }
        }
    }

    fun run() {
        // microserviciul se inscrie in lista de "subscribers" de la MessageManager prin conectarea la acesta
        subscribeToMessageManager()

        //heartbeat
        try {
            connectToHeartbeat()
            sendInitMessageToHeartbeat()
            listenToHeartbeat()
        } catch (e: Exception) {
            println("Eroare la conectarea / trimiterea de mesaje catre heartbeat")
        }

        //database
        try {
            connectToDatabase()

            //trimite cereri catre baza de date
            coroutineScope.launch {
                sendRequestsToDatabase()
            }

            //primim rezultatele cerute
            coroutineScope.launch {
                listenToDatabase()
            }
        } catch (e: Exception) {
            println("Eroare la conectarea / trimiterea / ascultarea bazei de date")
        }

        coroutineScope.launch {
            listenToRequests()
        }

        // se porneste un socket server TCP pe portul 1600 care asculta pentru conexiuni
        teacherMicroserviceServerSocket = ServerSocket(TEACHER_PORT)

        println("TeacherMicroservice se executa pe portul: ${teacherMicroserviceServerSocket.localPort}")
        println("Se asteapta cereri (intrebari)...")

        while (true) {
            val clientConnection = teacherMicroserviceServerSocket.accept()
            println("S-a conectat GUI-ul")

            val clientBufferReader = BufferedReader(InputStreamReader(clientConnection.inputStream))

            while (true) {
                // se asteapta conexiuni din partea clientilor ce doresc sa puna o intrebare
                // (in acest caz, din partea aplicatiei client GUI)

                val receivedQuestion = clientBufferReader.readLine()

                if (receivedQuestion == null) {
                    println("Conexiunea cu GUI a fost inchisa")
                    clientConnection.close()
                    break
                }

                // se foloseste un thread separat pentru tratarea fiecarei conexiuni client
                coroutineScope.launch {
                    println("S-a primit o cerere de la gui")

                    val id = questionID.incrementAndGet()

                    println("Trimit catre MessageManager: ${"intrebare teacher teacher $receivedQuestion\n"}")
                    messageManagerSocket.getOutputStream()
                        .write(("intrebare$id teacher teacher $receivedQuestion\n").toByteArray())

                    responseQueue[id] = mutableListOf()

                    //asteptam sa vina raspunsurile
                    delay(3000)

                    //procesam toate raspunsurile
                    processResponses(receivedQuestion, id)

                    responseQueue.remove(id)
                }
            }
        }
    }
}

fun main() {
    val teacherMicroservice = TeacherMicroservice()
    teacherMicroservice.run()
}