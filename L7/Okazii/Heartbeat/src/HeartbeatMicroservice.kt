import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class UnknownMessageException(message: String) : Exception(message)
class NullMessageException(message: String) : Exception(message)

class HeartbeatMicroservice {
    private var heartbeatSocket: ServerSocket = ServerSocket(HEARTBEAT_PORT)
    private var auctioneerSocket: Socket? = null
    private var biddingProcessorSocket: Socket? = null
    private var messageProcessorSocket: Socket? = null
    private var bidderSockets = mutableListOf<Pair<Socket, Int>>()
    private var exceptionLog = mutableListOf<String>()

    private lateinit var connectionsObservable: Observable<Pair<Socket, String>>
    private var socketObservableList = mutableListOf<Observable<String>>()
    private val subscriptions = CompositeDisposable()

    private var respondedToPing = false

    private val socketToService = mutableMapOf<Socket, List<String>>()

    companion object Constants{
        const val HEARTBEAT_PORT = 1800
        const val PING_TIME_SECONDS = 4
    }

    private fun addToLog(message: String) = exceptionLog.add(message)

    private fun log() {
        try {
            val file = File("HeartbeatLog")
            if (!file.exists())
                throw FileNotFoundException("Nu a fost gasit log-ul pentru Heartbeat")
            for (string in exceptionLog)
                file.appendText(string + "\n")
            exceptionLog.clear()
        } catch (e: FileNotFoundException) {
            println("Eroare: $e")
        }
    }

    init {
            try {
                connectionsObservable = Observable.create { emitter ->
                    while (!emitter.isDisposed) {
                        println("Astept sa se conecteze microservicii...")
                        val socket = heartbeatSocket.accept()

                        println("S-a conectat un microserviciu")
                        addToLog("S-a conectat un microserviciu")

                        val bufferReader = BufferedReader(InputStreamReader(socket.inputStream))

                        val message = bufferReader.readLine()
                        println("Message received: $message")

                        addToLog("Init message: $message")

                        if (message == null) {
                            socket.close()
                            emitter.onError(Exception("Mesaj null primit"))
                        }

                        emitter.onNext(Pair(socket, message))
                    }
                }
            } catch (e: Exception) {
                println("Eroare: $e")
                log()
            }
        log()
    }

    private fun receiveConnections() {
        val connSubs = connectionsObservable
            .subscribeBy (
                onNext = { (socket, message)->
                    try {
                        if (!listOf("Init", "End", "Start").contains(message.split(":").first()))
                            throw UnknownMessageException("Mesaj")

                        val messageType = message.split(":").first()

                        if (messageType == "Init") {
                            processInitMessage(socket, message)

                            println("Se incepe un nou thread de citire pentru ${message.split(":").last()}")
                            addToLog("Se incepe un nou thread de citire pentru ${message.split(":").last()}")

                            println("Se incepe un nou thread de scriere pentru ${message.split(":").last()}")
                            addToLog("Se incepe un nou thread de scriere pentru ${message.split(":").last()}")

                            val readObs = readObservable(socket).share()

                            readSubscribe(readObs)
                            writeSubscribe(socket)
                            listenToResponses(readObs)
                        }

                    } catch (e: Exception) {
                        println("Eroare: $e")
                        log()
                    }
                },

                onError = {
                    println("Eroare: $it")
                }
            )
        subscriptions.add(connSubs)
    }

    private fun readObservable(socket: Socket): Observable<String> {
        val socketObservableVal = Observable.create<String> { emitter ->
            val reader = BufferedReader(InputStreamReader(socket.inputStream))

            try {
                while (!emitter.isDisposed) {
                    val message = reader.readLine() ?: break
                    emitter.onNext(message)
                }
                emitter.onComplete()
            } catch (e: Exception) {
                emitter.onError(e)
            } finally {
                socket.close()
            }
        }
        socketObservableList.add(socketObservableVal)
        return socketObservableVal
    }

    private fun writeSubscribe(socket: Socket) {
        val writeSocketObservable = Observable
            .interval(PING_TIME_SECONDS.toLong(), TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .subscribeBy (
                onNext = {
                    try {
                        socket.getOutputStream().write("Ping:heartbeatMicroservice\n".toByteArray())
                        println("Am trimis un ping catre PORT:${socket.localPort}")
                        addToLog("Am trimis un ping catre PORT:${socket.localPort}")

                        Thread.sleep((PING_TIME_SECONDS / 2L) * 1000)

                        if (!respondedToPing) {
                            println("Socket mort - nu a raspuns in 2 secunde (port: ${socket.port})")
                            addToLog("Socket mort: ${socket.port}")
                            throw TimeoutException("Nu a raspuns la timp")
                            // aici apelezi restartService() când e implementat
                        } else {
                            println("Socket viu - a raspuns in timp (port: ${socket.port})")
                        }

                        respondedToPing = false

                    } catch (e: Exception) {
                        println("Eroare la trimiterea pingului: $e")
                        log()
                        throw e
                    }
                },
                onError = {
                    println("S-a terminat fluxul: $it")
                    restartService(socket)
                }
            )
        subscriptions.add(writeSocketObservable)
    }

    private fun restartService(socket: Socket) {
        val command = socketToService[socket] ?: run {
            println("socketul curent nu exista")
            return
        }

        socketToService.remove(socket)

        try {
            ProcessBuilder(command)
                .directory(File("/home/alex26/Documents/Laboratoare Materii/Sem2/SD/L7/Okazii"))
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
            println("Proces restartat: $command")
            addToLog("Proces restartat: $command")
        } catch (e: Exception) {
            println("Eroare la restartul procesului: $e")
            addToLog("Eroare la restartul procesului: $e")
            log()
        }
    }

    private fun listenToResponses(readObs: Observable<String>) {
        val sub = readObs
            .subscribeOn(Schedulers.io())
            .subscribeBy(
                onNext = {
                    if (it.startsWith("Response")) {
                        println("Am primit raspuns la ping: $it")
                        respondedToPing = true
                    }
                },
                onError = {
                    println("Eroare la shared reader: $it")
                },
                onComplete = {
                    println("Gata")
                }
            )
        subscriptions.add(sub)
    }

    private fun readSubscribe(socketObs: Observable<String>) {
        val sub = socketObs
            .subscribeOn(Schedulers.io())
            .subscribeBy (
                onNext = { message ->
                    println("Readul initial: $message")
                    addToLog(message)

                    val messageType = message.split(":").first()
                    
                    if (messageType == "End")
                        processEndMessage(message)
                },
                onError = {
                    println("[Socket thread]: $it")
                },
                onComplete = {
                    println("[Socket thread]: Closed properly")
                }
        )
        subscriptions.add(sub)
    }

    private fun processInitMessage(socket: Socket, message: String) {
        var whichMicroservice = message.split(":").last()
        if (whichMicroservice.contains("-"))
            whichMicroservice = whichMicroservice.split("-").first()

        when (whichMicroservice) {
            "auctioneerMicroservice" -> {
                if (auctioneerSocket == null) {
                    auctioneerSocket = socket
                    socketToService[socket] = listOf("java", "-jar", "out/artifacts/AuctioneerMicroservice_jar/AuctioneerMicroservice.jar")
                    println("S-a conectat auctioneer")
                    addToLog("A fost actualizat auctioneerSocket")
                } else {
                    println("AuctioneerSocket deja exista")
                    addToLog("AuctioneerSocket deja exista")
                }
            }

            "messageProcessorMicroservice" -> {
                if (messageProcessorSocket == null) {
                    messageProcessorSocket = socket
                    socketToService[socket] = listOf("java", "-jar", "out/artifacts/MessageProcessorMicroservice_jar/MessageProcessorMicroservice.jar")
                    println("S-a conectat messageProcessor")
                    addToLog("A fost actualizat messageProcessorSocket")
                } else {
                    println("MessageProcessorSocket deja exista")
                    addToLog("AuctioneerSocket deja exista")
                }
            }

            "biddingProcessorMicroservice" -> {
                if (biddingProcessorSocket == null) {
                    biddingProcessorSocket = socket
                    socketToService[socket] = listOf("java", "-jar", "out/artifacts/BiddingProcessorMicroservice_jar/BiddingProcessorMicroservice.jar")
                    println("S-a conectat biddingProcessor")
                    addToLog("A fost actualizat biddingProcessorSocket")
                } else {
                    println("BiddingProcessorSocket deja exista")
                    addToLog("BiddingProcessorSocket deja exista")
                }
            }

            "bidderMicroservice" -> {
                val localPort = message.split(":").last().split("-").last().toInt()
                socketToService[socket] = listOf("java", "-jar", "out/artifacts/BidderMicroservice_jar/BidderMicroservice.jar")

                bidderSockets.add(Pair(socket, localPort))
                println("S-a conectat un bidder:$localPort")
                addToLog("A fost adaugat un bidder")
            }
        }
    }

    private fun processEndMessage(message: String) {
        var whichSocket = message.split(":").last()
        if (whichSocket.contains("-"))
            whichSocket = whichSocket.split("-").first()

        when (whichSocket) {
            "auctioneerMicroservice" -> {
                auctioneerSocket?.close()
                auctioneerSocket = null
                println("auctioneerMicroservice s-a terminat")
                addToLog("auctioneerMicroservice s-a terminat")
            }

            "biddingProcessorMicroservice" -> {
                biddingProcessorSocket?.close()
                biddingProcessorSocket = null
                println("biddingProcessorMicroservice s-a terminat")
                addToLog("biddingProcessorMicroservice s-a terminat")
            }

            "messageProcessorMicroservice" -> {
                messageProcessorSocket?.close()
                messageProcessorSocket = null
                println("messageProcessorMicroservice s-a terminat")
                addToLog("messageProcessorMicroservice s-a terminat")
            }

            "bidderMicroservice" -> {
                try {
                    val localPort = message.split(":").last().split("-").last().toInt()

                    val bidder = bidderSockets.find { it.second == localPort }
                        ?: throw IllegalStateException("Bidder inexistent: $localPort")

                    bidder.first.close()
                    bidderSockets.remove(bidder)

                    println("bidderMicroservice-$localPort s-a terminat")
                    addToLog("bidderMicroservice-$localPort s-a terminat")
                } catch (e: Exception) {
                    println("Eroare la End, BidderMicroservice: $e")
                }
            }
        }

        if (
            auctioneerSocket == null &&
            biddingProcessorSocket == null &&
            messageProcessorSocket == null &&
            bidderSockets.isEmpty()
        ) {
            shutdown()
        }
    }

    fun shutdown() {
        println("Se inchide heartbeatul")
        addToLog("Se inchide heartbeatul")

        try {
            auctioneerSocket?.close()
            biddingProcessorSocket?.close()
            messageProcessorSocket?.close()
            bidderSockets.forEach {
                it.first.close()
            }

        } catch (e: Exception) {
            println("Eroare la inchiderea socketurilor")
        }
    }

    fun run() {
        receiveConnections()
        subscriptions.dispose()
    }
}

fun main() {
    val heartbeatMicroservice = HeartbeatMicroservice()
    heartbeatMicroservice.run()
}

