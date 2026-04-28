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

    private fun processInitMessage(bufferReader: BufferedReader, clientConnection: Socket): String {
        val initMessage = bufferReader.readLine()
        println("Init message: $initMessage")

        try {
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

                //procesarea subscriberului prin mesajul initial
                val entityType = processInitMessage(bufferReader, clientConnection)

                while (true) {
                    // se citeste raspunsul de pe socketul TCP
                    val receivedMessage = bufferReader.readLine()

                    // daca se primeste un mesaj gol (NULL), atunci inseamna ca cealalta parte a socket-ului a fost inchisa
                    if (receivedMessage == null) {
                        // deci subscriber-ul respectiv a fost deconectat
                        println("Subscriber-ul ${clientConnection.port} a fost deconectat.")
                        synchronized(subscribers) {
                            subscribers.remove(entityType)
                        }
                        bufferReader.close()
                        clientConnection.close()
                        break
                    }

                    println("Primit mesaj: $receivedMessage")
                    val (messageType, messageDestination, messageBody) = receivedMessage.split(" ", limit = 3)

                    when (messageType) {
                        "intrebare" -> {
                            // tipul mesajului de tip intrebare este de forma:
                            // intrebare <DESTINATIE_RASPUNS> <CONTINUT_INTREBARE>
                            when {
                                entityType == "teacher" -> broadcastMessageToStudents("intrebare teacher $messageBody")
                                entityType.startsWith("student") -> sendMessageToTeacher("intrebare $entityType $messageBody")
                            }
                        }
                        "raspuns" -> {
                            when {
                                entityType.startsWith("student") -> respondToTeacher(messageBody)
                                entityType == "teacher" -> respondToStudent(messageDestination, messageBody)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun main() {
    val messageManagerMicroservice = MessageManagerMicroservice()
    messageManagerMicroservice.run()
}