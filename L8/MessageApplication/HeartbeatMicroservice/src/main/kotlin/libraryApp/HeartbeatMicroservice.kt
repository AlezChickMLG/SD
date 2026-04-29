package libraryApp

import com.sun.security.ntlm.Server
import jdk.internal.util.xml.impl.Input
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

class HeartbeatMicroservice {
    private val socketMap: ConcurrentHashMap<String, Socket> = ConcurrentHashMap()
    private val heartbeatSocket: ServerSocket = ServerSocket(HEARTBEAT_PORT)

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val socketMapMutex = Mutex()

    companion object Constants {
        const val HEARTBEAT_PORT = 1900
    }

    private suspend fun processInitMessage(initMessage: String, socket: Socket) {
        try {
            val (messageType, microservice) = initMessage.split(":")

            if (messageType != "Init") {
                throw IllegalArgumentException()
            }

            when {
                microservice.startsWith("student") -> {
                    socketMapMutex.withLock {
                        socketMap[microservice] = socket
                    }
                }

                microservice.startsWith("teacher") -> {
                    socketMapMutex.withLock {
                        socketMap[microservice] = socket
                    }
                }

                microservice.startsWith("messageManager") -> {
                    socketMapMutex.withLock {
                        socketMap[microservice] = socket
                    }
                }
            }
        } catch (e: IllegalArgumentException) {
            println("Mesaj incorect de initializare")
        }
        catch (e: Exception) {
            println("Eroare la procesarea mesajului de initializare")
        }
    }

    fun run() {
        while (true) {
            println("Astept microservicii...")
            val clientConn = heartbeatSocket.accept()
            println("S-a conectat un microserviciu")

            coroutineScope.launch {
                try {
                    val reader = BufferedReader(InputStreamReader(clientConn.inputStream))
                    val initMessage = reader.readLine()

                    processInitMessage(initMessage, clientConn)
                } catch (e: Exception) {
                    println("A fost inchis microserviciul")
                }
            }
        }
    }
}

fun main() {
    val heartbeat = HeartbeatMicroservice()
    heartbeat.run()
}