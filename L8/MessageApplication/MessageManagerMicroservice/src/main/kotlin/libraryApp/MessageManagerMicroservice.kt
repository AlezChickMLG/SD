package libraryApp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class MessageManagerMicroservice {
    private val subscribers: HashMap<String, Socket>
    private lateinit var messageManagerSocket: ServerSocket

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val subscriberMutex = Mutex()

    companion object Constants {
        const val MESSAGE_MANAGER_PORT = 1500
    }

    init {
        subscribers = hashMapOf()
    }

    private fun listAll() {
        subscribers.forEach {
            println(it.key)
        }
    }

    private suspend fun broadcastMessageToStudents(message: String) {
        subscriberMutex.withLock {
            subscribers.forEach {
                it.takeIf { it.key.startsWith("student") }
                    ?.value?.getOutputStream()?.write((message + "\n").toByteArray())
            }
        }
    }

    private suspend fun sendMessageToTeacher(message: String) {
        subscriberMutex.withLock {
            subscribers.get("teacher")
                ?.getOutputStream()?.write((message + "\n").toByteArray())
        }
    }

    private suspend fun respondToTeacher(message: String) {
        subscriberMutex.withLock {
            subscribers.get("teacher")?.getOutputStream()?.write((message + "\n").toByteArray())
        }
    }

    private suspend fun respondToStudent(student: String, message: String) {
        subscriberMutex.withLock {
            subscribers.get(student)?.getOutputStream()?.write((message + "\n").toByteArray())
        }
    }

    private suspend fun processInitMessage(initMessage: String?, clientConnection: Socket): String {
        try {
            if (initMessage == null) {
                throw Exception()
            }

            val (messageType, entityType) = initMessage.split(":")
            if (messageType != "Init")
                throw IllegalArgumentException("Mesaj ciudat")

            println("Subscriber conectat: ${clientConnection.inetAddress.hostAddress}:${clientConnection.port}: $entityType")

            subscriberMutex.withLock {
                subscribers[entityType] = clientConnection
            }

            return entityType
        } catch (e: Exception) {
            println("Eroare la procesarea mesajului de initializare: $e")
            return ""
        }
    }

    private suspend fun listenToRequests(clientConnection: Socket, entityType: String) {
        val reader = BufferedReader(InputStreamReader(clientConnection.inputStream))

        while (true) {
            try {
                val request = withContext(Dispatchers.IO) {
                    reader.readLine()
                }

                println("Primit mesaj: $request")

                // daca se primeste un mesaj gol (NULL), atunci inseamna ca cealalta parte a socket-ului a fost inchisa
                if (request == null) {
                    // deci subscriber-ul respectiv a fost deconectat
                    println("$entityType a fost deconectat.")
                    subscriberMutex.withLock {
                        subscribers.remove(entityType)
                    }
                    withContext(Dispatchers.IO) {
                        reader.close()
                    }
                    withContext(Dispatchers.IO) {
                        clientConnection.close()
                    }
                    break
                }

                val (messageType, messageDestination, messageBody) = request.split(" ", limit = 3)

                when {
                    messageType.startsWith("intrebare") -> {
                        // tipul mesajului de tip intrebare este de forma:
                        // intrebare <DESTINATIE_RASPUNS> <CONTINUT_INTREBARE>
                        when {
                            entityType == "teacher" -> {
                                broadcastMessageToStudents(request)
                            }
                            entityType.startsWith("student") -> {
                                sendMessageToTeacher(request)
                            }
                        }
                    }

                    messageType.startsWith("raspuns") -> {
                        when {
                            entityType.startsWith("student") -> {
                                delay(1500)
                                respondToTeacher(request)
                            }
                            entityType == "teacher" -> {
                                delay(1500)
                                respondToStudent(messageDestination, request)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("Eroare la listenToRequests: $entityType")
            }
        }
    }

    public fun run() {
        // se porneste un socket server TCP pe portul 1500 care asculta pentru conexiuni
        messageManagerSocket = ServerSocket(MESSAGE_MANAGER_PORT)
        println("MessageManagerMicroservice se executa pe portul: ${messageManagerSocket.localPort}")
        println("Se asteapta conexiuni si mesaje...")

        while (true) {
            // se asteapta conexiuni din partea clientilor subscriberi
            val clientConnection = messageManagerSocket.accept()

            // se porneste un thread separat pentru tratarea conexiunii cu clientul
            coroutineScope.launch {
                val bufferReader = BufferedReader(InputStreamReader(clientConnection.inputStream))
                val initMessage = bufferReader.readLine()

                //procesarea subscriberului prin mesajul initial
                val entityType = processInitMessage(initMessage, clientConnection)

                if (entityType != "")
                    listenToRequests(clientConnection, entityType)
            }
        }
    }
}

fun main() {
    val messageManagerMicroservice = MessageManagerMicroservice()
    messageManagerMicroservice.run()
}