package libraryApp

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.Exception
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.concurrent.timer
import kotlin.system.exitProcess

class StudentMicroservice {
    // intrebarile si raspunsurile sunt mentinute intr-o lista de perechi de forma:
    // [<INTREBARE 1, RASPUNS 1>, <INTREBARE 2, RASPUNS 2>, ... ]
    private lateinit var questionDatabase: MutableList<Pair<String, String>>
    private lateinit var messageManagerSocket: Socket
    private lateinit var heartbeatSocket: Socket

    //id-ul studentului
    private lateinit var studentID: Number
    private var assistantID: Int = -1

    //server socket pentru gui pe port: 2000 + ID-ul studentului
    private lateinit var guiSocket: ServerSocket
    private lateinit var guiClient: Socket

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val questionID = AtomicInteger(0)

    //response queue
    private val responseQueue = ConcurrentHashMap<Int, CompletableDeferred<String>>()

    init {
        val databaseLines: List<String> = File("questions_database.txt").readLines()
        questionDatabase = mutableListOf()

        /*
         "baza de date" cu intrebari si raspunsuri este de forma:

         <INTREBARE_1>\n
         <RASPUNS_INTREBARE_1>\n
         <INTREBARE_2>\n
         <RASPUNS_INTREBARE_2>\n
         ...
         */
        for (i in 0..(databaseLines.size - 1) step 2) {
            questionDatabase.add(Pair(databaseLines[i], databaseLines[i + 1]))
        }

        studentID = try {
            System.getenv("STUDENT_ID").toInt()
        } catch (e: kotlin.Exception) {
            println("Eroare la gasirea variabilei de mediu STUDENT_ID")
            print("Student ID:")
            readln().toInt()
        }

        loadId()
    }

    companion object Constants {
        // pentru testare, se foloseste localhost. pentru deploy, server-ul socket (microserviciul MessageManager) se identifica dupa un "hostname"
        // acest hostname poate fi trimis (optional) ca variabila de mediu
        val MESSAGE_MANAGER_HOST = System.getenv("MESSAGE_MANAGER_HOST") ?: "localhost"
        val HEARTBEAT_HOST = System.getenv("HEARTBEAT_HOST") ?: "localhost"
        const val MESSAGE_MANAGER_PORT = 1500
        const val HEARTBEAT_PORT = 1900
    }

    private fun addId(id: Int) {
        val file = File("id_database.txt")
        file.appendText("student$studentID:$id\n")
        println("Am adaugat in id_database: student$studentID:$id")
    }

    private fun loadId() {
        val file = File("id_database.txt")
        val ids = file.readLines()

        for (line in ids) {
            if (line.split(":").first() == "student$studentID") {
                assistantID = line.split(":").last().toInt()
                println("Am incarcat id-ul: $assistantID")
            }
        }
    }

    private fun sendInitMessage() {
        messageManagerSocket.getOutputStream().write("Init:student$studentID\n".toByteArray())
    }

    private fun connectToHeartbeat() {
        heartbeatSocket = Socket(HEARTBEAT_HOST, HEARTBEAT_PORT)
    }

    private fun sendInitMessageToHeartbeat() {
        heartbeatSocket.getOutputStream().write("Init:student$studentID\n".toByteArray())
    }

    private fun sendVerifyMessage() {
        messageManagerSocket.getOutputStream().write("Verificare:student$studentID:$assistantID\n".toByteArray())
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
                        heartbeatSocket.getOutputStream().write("Pong:student$studentID\n".toByteArray())
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

     private fun listenToGUI() {
         val guiPort = 2000 + studentID.toInt()

         guiSocket = ServerSocket(guiPort)
         println("Am creat un server pe portul $guiPort")

         while (true) {
             guiClient = guiSocket.accept()
             println("S-a conectat GUI-ul")
             //println("S-a conectat un client pe portul: ${guiClient.port}")

             val reader = BufferedReader(InputStreamReader(guiClient.inputStream))

             while (true) {
                 val question = reader.readLine()

                 if (question == null) {
                     println("Conectiunea cu GUI a fost inchisa")
                     guiClient.close()
                     break
                 }

                 coroutineScope.launch {
                     println("Intrebare de la GUI: $question")

                     val id = questionID.incrementAndGet()

                     messageManagerSocket.getOutputStream()
                         .write("intrebare$id student$studentID student$studentID $question\n".toByteArray())

                     println("Am trimis catre message manager: intrebare$id student$studentID student$studentID $question")

                     val deferred = CompletableDeferred<String>()

                     //astept completarea atunci cand primesc raspunsul
                     responseQueue[id] = deferred

                     val response = withTimeoutOrNull(3000) {
                         deferred.await()
                     }

                     if (response == null) {
                         println("Nu am primit raspuns la intrebarea $id")
                     } else {
                         println("Am primit raspuns la intrebarea: $question\n${response.split(" ").last()}")
                     }

                     responseQueue.remove(id)
                 }
             }
         }
     }

    private fun subscribeToMessageManager() {
        try {
            messageManagerSocket = Socket(MESSAGE_MANAGER_HOST, MESSAGE_MANAGER_PORT)
            sendInitMessage()
            sendVerifyMessage()
            println("M-am conectat la MessageManager!")
        } catch (e: Exception) {
            println("Nu ma pot conecta la MessageManager!")
            exitProcess(1)
        }
    }

    private fun respondToQuestion(question: String): String? {
        questionDatabase.forEach {
            // daca se gaseste raspunsul la intrebare, acesta este returnat apelantului
            if (it.first == question) {
                return it.second
            }
        }
        return null
    }

    private fun listenToRequests() {
        val bufferReader = BufferedReader(InputStreamReader(messageManagerSocket.inputStream))

        while (true) {
            // se asteapta intrebari trimise prin intermediarul "MessageManager"
            val request = bufferReader.readLine()

            println("Request de procesat: $request")

            if (request == null) {
                // daca se primeste un mesaj gol (NULL), atunci inseamna ca cealalta parte a socket-ului a fost inchisa
                println("Microserviciul MessageService (${messageManagerSocket.port}) a fost oprit.")
                bufferReader.close()
                messageManagerSocket.close()
                break
            }

            // se foloseste un thread separat pentru tratarea intrebarii primite
            coroutineScope.launch {
                processRequest(request)
            }
        }
    }

    private suspend fun processRequest(request: String) {
        if (!request.contains(":")) {
            val (messageType, messageSource, messageDestination, messageBody) = request.split(" ", limit = 4)

            when {
                // tipul mesajului cunoscut de acest microserviciu este de forma:
                // intrebare <DESTINATIE_RASPUNS> <CONTINUT_INTREBARE>
                messageType.startsWith("intrebare") -> {
                    println("Am primit o intrebare de la $messageDestination: \"${messageBody}\"")
                    var responseToQuestion = respondToQuestion(messageBody)
                    val id = messageType.substring("intrebare".length).toInt()

                    if (responseToQuestion != null) {
                        responseToQuestion = "raspuns$id student$studentID $messageDestination $responseToQuestion"
                        println("Trimit raspunsul: \"${responseToQuestion}\"")
                        withContext(Dispatchers.IO) {
                            messageManagerSocket.getOutputStream().write((responseToQuestion + "\n").toByteArray())
                        }
                    } else {
                        responseToQuestion = "raspuns$id student$studentID $messageDestination Nu stiu"
                        println("Trimit raspunsul: \"$responseToQuestion\"")
                        withContext(Dispatchers.IO) {
                            messageManagerSocket.getOutputStream().write((responseToQuestion + "\n").toByteArray())
                        }
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

        else {
            val (messageType, result) = request.split(":")

            when {
                messageType.startsWith("Inregistrare") -> {
                    addId(result.toInt())
                    assistantID = result.toInt()
                }
            }
        }
    }

    public fun run() {
        // microserviciul se inscrie in lista de "subscribers" de la MessageManager prin conectarea la acesta
        subscribeToMessageManager()

        //gestionarea heartbeatului
//        connectToHeartbeat()
//        sendInitMessageToHeartbeat()
//        listenToHeartbeat()

        coroutineScope.launch {
            listenToGUI()
        }

        println("StudentMicroservice se executa pe portul: ${messageManagerSocket.localPort}")
        println("Se asteapta mesaje...")

        listenToRequests()
    }
}

fun main() {
    val studentMicroservice = StudentMicroservice()
    studentMicroservice.run()
}