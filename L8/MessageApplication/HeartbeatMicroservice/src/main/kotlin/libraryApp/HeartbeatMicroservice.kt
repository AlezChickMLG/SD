package libraryApp

import com.sun.security.ntlm.Server
import jdk.internal.util.xml.impl.Input
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.nio.Buffer
import java.util.concurrent.ConcurrentHashMap

class HeartbeatMicroservice {
    private val socketMap: ConcurrentHashMap<String, Socket> = ConcurrentHashMap()
    private val heartbeatSocket: ServerSocket = ServerSocket(HEARTBEAT_PORT)

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val socketMapMutex = Mutex()

    companion object Constants {
        const val HEARTBEAT_PORT = 1900
        const val PING_TIMEOUT = 4000
    }

    private suspend fun processInitMessage(initMessage: String, socket: Socket): Boolean {
        try {
            val (messageType, microservice) = initMessage.split(":")

            if (messageType != "Init") {
                throw IllegalArgumentException()
            }

            when {
                microservice.startsWith("student") -> {
                    socketMapMutex.withLock {
                        socketMap[microservice] = socket
                        return true
                    }
                }

                microservice.startsWith("teacher") -> {
                    socketMapMutex.withLock {
                        socketMap[microservice] = socket
                        return true
                    }
                }

                microservice.startsWith("messageManager") -> {
                    socketMapMutex.withLock {
                        socketMap[microservice] = socket
                        return true
                    }
                }
            }

            return false
        } catch (e: IllegalArgumentException) {
            println("Mesaj incorect de initializare")
            return false
        }
        catch (e: Exception) {
            println("Eroare la procesarea mesajului de initializare")
            return false
        }
    }

    private suspend fun getMicroserviceName(socket: Socket): String? {
        socketMapMutex.withLock {
            return socketMap.entries.find {
                it.value == socket
            }?.key
        }
    }

    private fun processRequest(request: String) {

    }

    private suspend fun listenToMicroservice(socket: Socket) {
        val reader = BufferedReader(InputStreamReader(socket.inputStream))
        val microserviceName = getMicroserviceName(socket)

        while (true) {
            val request = withContext(Dispatchers.IO) {
                reader.readLine()
            }

            println("Request from $microserviceName: $request")

            coroutineScope.launch {
                processRequest(request)
            }
        }
    }

    private suspend fun sendPingToMicroservice(socket: Socket) {
        val microserviceName = getMicroserviceName(socket)

        while (true) {
            //Delay time pentru ping
            delay(PING_TIMEOUT.toLong())

            withContext(Dispatchers.IO) {
                socket.getOutputStream().write("Ping:heartbeat\n".toByteArray())
                println("Am trimis un ping catre $microserviceName")
            }
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

                    if (processInitMessage(initMessage, clientConn)) {
                        println("Procesare corecta a mesajului de initializare")

                        //incep sa trimit pinguri catre microservicii
                        launch {
                            sendPingToMicroservice(clientConn)
                        }

                        launch {
                            listenToMicroservice(clientConn)
                        }
                    }
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