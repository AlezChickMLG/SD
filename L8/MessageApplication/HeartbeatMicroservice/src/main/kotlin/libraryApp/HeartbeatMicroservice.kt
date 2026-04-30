package libraryApp

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

class HeartbeatMicroservice {
    private val socketMap: ConcurrentHashMap<String, Socket> = ConcurrentHashMap()
    private val heartbeatSocket: ServerSocket = ServerSocket(HEARTBEAT_PORT)

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val socketMapMutex = Mutex()

    private val pongMap = ConcurrentHashMap<String, CompletableDeferred<String>>()
    private val pongMapMutex = Mutex()

    private val restartCommands = HashMap<String, List<String>>()
    private val directories = HashMap<String, String>()
    private val restartMutex = Mutex()

    companion object Constants {
        const val HEARTBEAT_PORT = 1900
        const val PING_TIMEOUT = 6000
        const val PARENT_PATH = "/home/alex26/Documents/Laboratoare Materii/Sem2/SD/L8/MessageApplication/"
    }

    private suspend fun processInitMessage(initMessage: String, socket: Socket): Boolean {
        try {
            val (messageType, microservice) = initMessage.split(":")

            if (messageType != "Init") {
                throw IllegalArgumentException()
            }

            socketMap[microservice]?.isClosed?.let {
                if (!it) {
                    socketMap[microservice]?.isConnected?.let {
                        throw IllegalStateException()
                    }
                }
            }

            val validPrefixes = listOf("student", "teacher", "messageManager")
            if (validPrefixes.none { microservice.startsWith(it) }) return false

            socketMapMutex.withLock {
                socketMap[microservice] = socket

                when {
                    microservice.startsWith("student") -> {
                        restartCommands[microservice] = listOf("java", "-jar", "target/StudentMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar")
                        directories[microservice] = PARENT_PATH + "StudentMicroservice"
                    }
                    microservice.startsWith("teacher") -> {
                        restartCommands[microservice] = listOf("java", "-jar", "target/TeacherMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar")
                        directories[microservice] = PARENT_PATH + "TeacherMicroservice"
                    }
                    microservice.startsWith("messageManager") -> {
                        restartCommands[microservice] = listOf("java", "-jar", "target/MessageManagerMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar")
                        directories[microservice] = PARENT_PATH + "MessageManagerMicroservice"
                    }
                }
            }

            return true
        } catch (e: IllegalArgumentException) {
            println("Mesaj incorect de initializare")
            return false
        } catch (e: IllegalStateException) {
            println("Microserviciul deja exista si este activ")
            return false
        } catch (e: Exception) {
            println("Eroare la procesarea mesajului de initializare")
            return false
        }
    }

    private suspend fun processRequest(request: String) {
        try {
            val (messageType, microservice) = request.split(":")

            when {
                messageType.startsWith("Pong") -> {
                    val deferred = pongMapMutex.withLock {
                        pongMap[microservice]
                    }
                    deferred?.complete("Pong")
                }
            }
        } catch (e: Exception) {
            println("Eroare la procesarea requestului: $request")
        }
    }

    private suspend fun getMicroserviceName(socket: Socket): String? {
        socketMapMutex.withLock {
            return socketMap.entries.find {
                it.value == socket
            }?.key
        }
    }

    private suspend fun waitForPong(
        microservice: String,
        deferred: CompletableDeferred<String>,
        shutdownSignal: Channel<Unit>
    ) {
        val response = withTimeoutOrNull(PING_TIMEOUT.toLong() / 2) {
            deferred.await()
        }

        if (response == null) {
            println("Nu am primit pong de la $microservice in timp")
            shutdownSignal.trySend(Unit)
        } else {
            println("Am primit pong de la $microservice in timp")
        }

        pongMapMutex.withLock {
            pongMap.remove(microservice)
        }
    }

    private suspend fun listenToMicroservice(socket: Socket, shutdownSignal: Channel<Unit>) {
        val reader = BufferedReader(InputStreamReader(socket.inputStream))
        val microserviceName = getMicroserviceName(socket)

        // corutina care asteapta semnalul de shutdown si inchide socket-ul
        val shutdownJob = coroutineScope.launch {
            shutdownSignal.receive()
            println("Am primit semnal de shutdown pentru $microserviceName, inchid socket-ul")
            withContext(Dispatchers.IO) {
                socket.close()
            }
        }

        while (true) {
            val request = try {
                withContext(Dispatchers.IO) {
                    reader.readLine()
                }
            } catch (e: Exception) {
                println("$microserviceName socket inchis fortat")
                break
            }

            if (request == null) {
                println("$microserviceName a fost inchis")
                withContext(Dispatchers.IO) {
                    socket.close()
                    reader.close()
                }
                break
            }

            coroutineScope.launch {
                processRequest(request)
            }
        }

        shutdownJob.cancel()
    }

    private suspend fun sendPingToMicroservice(socket: Socket, shutdownSignal: Channel<Unit>) {
        val microserviceName = getMicroserviceName(socket)

        if (microserviceName == null) {
            println("Microserviciu necunoscut")
            return
        }

        while (true) {
            delay(PING_TIMEOUT.toLong())

            val deferred = CompletableDeferred<String>()
            pongMapMutex.withLock {
                pongMap[microserviceName] = deferred
            }

            try {
                withContext(Dispatchers.IO) {
                    socket.getOutputStream().write("Ping:heartbeat\n".toByteArray())
                    println("Am trimis un ping catre $microserviceName")
                }
            } catch (e: Exception) {
                println("Eroare la trimiterea pingului catre $microserviceName")
                shutdownSignal.trySend(Unit)
                break
            }

            coroutineScope.launch {
                waitForPong(microserviceName, deferred, shutdownSignal)
            }
        }
    }

    private suspend fun restartService(microservice: String) {
        restartMutex.withLock {
            val restartCommand = restartCommands[microservice] ?: run {
                println("Nu exista comanda de restart pentru $microservice")
                return
            }
            restartCommands.remove(microservice)

            val directory = directories[microservice] ?: run {
                println("Nu exista directorul pentru $microservice")
                return
            }
            directories.remove(microservice)
            socketMap.remove(microservice)

            try {
                ProcessBuilder(listOf("gnome-terminal", "--") + restartCommand)
                    .directory(File(directory))
                    .start()
                println("Microserviciu restartat: $microservice")
            } catch (e: Exception) {
                println("Eroare la restartarea microserviciului $microservice: ${e.message}")
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

                    if (!processInitMessage(initMessage, clientConn)) return@launch

                    println("Procesare corecta a mesajului de initializare")

                    // FIX: salvam numele ACUM, cat socket-ul e inca in map
                    val microservice = getMicroserviceName(clientConn) ?: run {
                        println("Nu pot identifica microserviciul")
                        return@launch
                    }

                    // FIX: Channel.CONFLATED — nu retine Unit-uri vechi din cicluri anterioare
                    val shutdownSignal = Channel<Unit>(capacity = Channel.CONFLATED)
                    val connectionJob = Job(coroutineScope.coroutineContext[Job])

                    val listenJob = launch(connectionJob) {
                        listenToMicroservice(clientConn, shutdownSignal)
                    }

                    val pingJob = launch(connectionJob) {
                        sendPingToMicroservice(clientConn, shutdownSignal)
                    }

                    // lifecycle: asteapta disconnect, apoi restart
                    launch {
                        listenJob.join()
                        pingJob.cancelAndJoin()

                        println("Restartez serviciul $microservice")
                        restartService(microservice)
                    }

                } catch (e: Exception) {
                    println("Eroare la conectarea microserviciului: ${e.message}")
                }
            }
        }
    }
}

fun main() {
    val heartbeat = HeartbeatMicroservice()
    heartbeat.run()
}