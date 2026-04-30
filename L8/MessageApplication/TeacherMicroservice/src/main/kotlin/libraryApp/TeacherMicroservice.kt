package libraryApp

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class TeacherMicroservice {
    private lateinit var messageManagerSocket: Socket
    private lateinit var teacherMicroserviceServerSocket: ServerSocket
    private lateinit var heartbeatSocket: Socket

    private lateinit var questionDatabase: MutableList<Pair<String, String>>

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val questionID = AtomicInteger(0)

    //response queue
    private val responseQueue = ConcurrentHashMap<Int, CompletableDeferred<String>>()

    companion object Constants {
        // pentru testare, se foloseste localhost. pentru deploy, server-ul socket (microserviciul MessageManager) se identifica dupa un "hostname"
        // acest hostname poate fi trimis (optional) ca variabila de mediu
        val MESSAGE_MANAGER_HOST = System.getenv("MESSAGE_MANAGER_HOST") ?: "localhost"
        val HEARTBEAT_HOST = System.getenv("HEARTBEAT_HOST") ?: "localhost"
        const val MESSAGE_MANAGER_PORT = 1500
        const val TEACHER_PORT = 1600
        const val HEARTBEAT_PORT = 1900
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
                        println("Am raspuns heartbeatului")
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

    private fun processRequest(request: String) {
        println("Request received: $request")
        val (messageType, messageDestination, messageBody) = request.split(" ", limit = 3)
        when {
            messageType.startsWith("intrebare") -> {
                val response = questionDatabase.find {
                    it.first == messageBody
                }

                val id = messageType.substring("intrebare".length)

                if (response != null) {
                    messageManagerSocket.getOutputStream()
                        .write("raspuns$id $messageDestination ${response.second}\n".toByteArray())
                    println("Am trimis catre $messageDestination raspunsul: ${response.second}")
                }
            }
            messageType.startsWith("raspuns") -> {
                try {
                    val question_id = messageType.substring("raspuns".length).toInt()
                    responseQueue[question_id]?.complete(messageBody)
                } catch (e: Exception) {
                    println("Eroare la completarea deferrul-ui raspuns: $e")
                }
            }
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

                processRequest(request)
            } catch (e: Exception) {
                println("Eroare la listenToRequest")
                break
            }
        }
    }

    fun run() {
        // microserviciul se inscrie in lista de "subscribers" de la MessageManager prin conectarea la acesta
        subscribeToMessageManager()

        //heartbeat
        connectToHeartbeat()
        sendInitMessageToHeartbeat()
        listenToHeartbeat()

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

                    println("Trimit catre MessageManager: ${"intrebare teacher $receivedQuestion\n"}")
                    messageManagerSocket.getOutputStream()
                        .write(("intrebare$id teacher $receivedQuestion\n").toByteArray())

                    val deferred = CompletableDeferred<String>()
                    responseQueue[id] = deferred

                    val response = withTimeoutOrNull(3000) {
                        deferred.await()
                    }

                    if (response == null)
                        println("Nu am primit raspuns la intrebarea $id")
                    else
                        println("Am primit raspuns la intrebarea $receivedQuestion\n$response")

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