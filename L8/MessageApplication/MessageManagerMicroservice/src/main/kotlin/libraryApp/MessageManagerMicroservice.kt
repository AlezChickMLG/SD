package libraryApp

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class MessageManagerMicroservice {
    private val subscribers: HashMap<String, Socket>
    private lateinit var messageManagerSocket: ServerSocket

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

    private fun broadcastMessageToStudents(message: String) {
        subscribers.forEach {
            it.takeIf { it.key.startsWith("student") }
                ?.value?.getOutputStream()?.write((message + "\n").toByteArray())
        }
    }

    private fun sendMessageToTeacher(message: String) {
        subscribers.get("teacher")
            ?.getOutputStream()?.write((message + "\n").toByteArray())
    }

    private fun respondToTeacher(message: String) {
        subscribers.get("teacher")?.getOutputStream()?.write((message + "\n").toByteArray())
    }

    private fun respondToStudent(student: String, message: String) {
        subscribers.get(student)?.getOutputStream()?.write((message + "\n").toByteArray())
    }

    private fun processInitMessage(initMessage: String?, clientConnection: Socket): String {
        try {
            if (initMessage == null) {
                throw Exception()
            }

            val (messageType, entityType) = initMessage.split(":")
            if (messageType != "Init")
                throw IllegalArgumentException("Mesaj ciudat")

            println("Subscriber conectat: ${clientConnection.inetAddress.hostAddress}:${clientConnection.port}: $entityType")

            synchronized(subscribers) {
                subscribers[entityType] = clientConnection
            }

            return entityType
        } catch (e: Exception) {
            println("Eroare la procesarea mesajului de initializare: $e")
            return ""
        }
    }

    private fun listenToRequests(clientConnection: Socket, entityType: String) {
        val reader = BufferedReader(InputStreamReader(clientConnection.inputStream))

        while (true) {
            try {
                val request = reader.readLine()

                println("Primit mesaj: $request")

                // daca se primeste un mesaj gol (NULL), atunci inseamna ca cealalta parte a socket-ului a fost inchisa
                if (request == null) {
                    // deci subscriber-ul respectiv a fost deconectat
                    println("$entityType a fost deconectat.")
                    synchronized(subscribers) {
                        subscribers.remove(entityType)
                    }
                    reader.close()
                      clientConnection.close()
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
                                respondToTeacher(request)
                            }
                            entityType == "teacher" -> {
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
            thread {
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