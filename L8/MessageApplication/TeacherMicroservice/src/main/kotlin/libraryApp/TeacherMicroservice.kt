package libraryApp

import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.net.*
import java.nio.Buffer
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class TeacherMicroservice {
    private lateinit var messageManagerSocket: Socket
    private lateinit var teacherMicroserviceServerSocket: ServerSocket

    private lateinit var questionDatabase: MutableList<Pair<String, String>>
    private val responseQueue: BlockingQueue<String> = LinkedBlockingQueue()

    companion object Constants {
        // pentru testare, se foloseste localhost. pentru deploy, server-ul socket (microserviciul MessageManager) se identifica dupa un "hostname"
        // acest hostname poate fi trimis (optional) ca variabila de mediu
        val MESSAGE_MANAGER_HOST = System.getenv("MESSAGE_MANAGER_HOST") ?: "localhost"
        const val MESSAGE_MANAGER_PORT = 1500
        const val TEACHER_PORT = 1600
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
                responseQueue.add(request)
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
            }
        }
    }

    fun run() {
        // microserviciul se inscrie in lista de "subscribers" de la MessageManager prin conectarea la acesta
        subscribeToMessageManager()

        thread {
            listenToRequests()
        }

        // se porneste un socket server TCP pe portul 1600 care asculta pentru conexiuni
        teacherMicroserviceServerSocket = ServerSocket(TEACHER_PORT)

        println("TeacherMicroservice se executa pe portul: ${teacherMicroserviceServerSocket.localPort}")
        println("Se asteapta cereri (intrebari)...")

        while (true) {
            // se asteapta conexiuni din partea clientilor ce doresc sa puna o intrebare
            // (in acest caz, din partea aplicatiei client GUI)
            val clientConnection = teacherMicroserviceServerSocket.accept()

            // se foloseste un thread separat pentru tratarea fiecarei conexiuni client
            thread {
                println("S-a primit o cerere de la: ${clientConnection.inetAddress.hostAddress}:${clientConnection.port}")

                // se citeste intrebarea dorita
                val clientBufferReader = BufferedReader(InputStreamReader(clientConnection.inputStream))
                val receivedQuestion = clientBufferReader.readLine()

                // intrebarea este redirectionata catre microserviciul MessageManager
                println("Trimit catre MessageManager: ${"intrebare teacher $receivedQuestion\n"}")
                messageManagerSocket.getOutputStream().write(("intrebare teacher $receivedQuestion\n").toByteArray())

                val worker = Thread.currentThread()

                val timerThread = thread {
                    try {
                        Thread.sleep(3000)
                        worker.interrupt()
                    } catch (e: Exception) {}
                }

                try {
                    println("Astept raspuns la intrebarea: $receivedQuestion...")
                    val response = responseQueue.take()
                    println("Am primit raspunsul la intrebarea: $receivedQuestion\n${response.split(" ").last()}")
                    timerThread.interrupt()
                } catch (e: InterruptedException) {
                    println("Nu am primit un raspuns in timp")
                }
            }
        }
    }
}

fun main() {
    val teacherMicroservice = TeacherMicroservice()
    teacherMicroservice.run()
}