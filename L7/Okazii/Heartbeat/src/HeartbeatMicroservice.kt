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
import java.awt.Composite
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class UnknownMessageException(message: String) : Exception(message)

class HeartbeatMicroservice {
    private var heartbeatSocket: ServerSocket = ServerSocket(HEARTBEAT_PORT)
    private var auctioneerSocket: Socket? = null
    private var biddingProcessorSocket: Socket? = null
    private var messageProcessorSocket: Socket? = null
    private var bidderSockets = mutableListOf<Pair<Socket, Int>>()
    private var exceptionLog = mutableListOf<String>()

    private lateinit var connectionsObservable: Observable<Pair<Socket, String>>
    private var socketObservableList = mutableListOf<Observable<String>>()

    private var respondedToPing = mutableListOf(false, false, false)
    private var bidderRespondedToPing = mutableListOf<Pair<Int, Boolean>>()

    private val subscriptions = mutableMapOf<Socket, CompositeDisposable>()
    private val receiveConnSub = CompositeDisposable()
    private val socketToService = mutableMapOf<Socket, List<String>>()

    private val endedPeacefully = mutableMapOf<Socket, Boolean>()

    companion object Constants{
        const val HEARTBEAT_PORT = 1800
        const val PING_TIME_SECONDS = 4

        const val AUCTIONEER_PING = 0
        const val MESSAGE_PROCESSOR_PING = 1
        const val BIDDING_PROCESSOR_PING = 2
        const val BIDDER_PING = 3
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

    private fun clearLog() {
        try {
            val file = File("HeartbeatLog")
            if (!file.exists())
                throw FileNotFoundException("Nu a fost gasit log-ul pentru Heartbeat")
            file.writeText("")
        } catch (e: FileNotFoundException) {
            println("Eroare: $e")
        }
    }

    private fun getSocketPosition(socket: Socket): Int {
        return when {
            socket == auctioneerSocket -> AUCTIONEER_PING
            socket == messageProcessorSocket -> MESSAGE_PROCESSOR_PING
            socket == biddingProcessorSocket -> BIDDING_PROCESSOR_PING
            bidderSockets.any { it.first == socket } -> BIDDER_PING
            else -> -1
        }
    }

    private fun getSocketName(socket: Socket): String {
        return when {
            socket == auctioneerSocket -> "auctioneer"
            socket == messageProcessorSocket -> "messageProcessor"
            socket == biddingProcessorSocket -> "biddingProcessor"
            bidderSockets.any { it.first == socket } -> {
                val bidder = bidderSockets.find { (itSocket, port) ->
                    itSocket == socket
                }
                "bidder-${bidder?.second}"
            }
            else -> "IDK"
        }
    }

    private fun assignRespondedToPing(socket: Socket, socketPosition: Int, booleanValue: Boolean) {
        if (socketPosition != BIDDER_PING) {
            respondedToPing[socketPosition] = booleanValue
        }
        else {
            val bidder = bidderRespondedToPing.find { itBidder ->
                itBidder.first == bidderSockets.find { it2 -> it2.first == socket }?.second
            }
            bidderRespondedToPing.remove(bidder)
            bidderRespondedToPing.add(Pair(bidder!!.first, booleanValue))
        }
    }

    private fun restartService(socket: Socket) {
        val command = socketToService[socket] ?: run {
            println("socketul curent nu exista")
            return
        }

        try {
            when {
                socket == auctioneerSocket -> {
                    auctioneerSocket!!.close()
                    auctioneerSocket = null
                }
                socket == biddingProcessorSocket -> {
                    biddingProcessorSocket!!.close()
                    biddingProcessorSocket = null
                }
                socket == messageProcessorSocket -> {
                    messageProcessorSocket!!.close()
                    messageProcessorSocket = null
                }

                bidderSockets.any {bidder -> bidder.first == socket} -> {
                    val indexOfBidder = bidderSockets.indexOf(bidderSockets.find { bidder -> bidder.first == socket })
                    bidderSockets[indexOfBidder].first.close()
                    bidderSockets.removeAt(indexOfBidder)
                }

                else -> throw IllegalStateException("Nu stiu ce socket este")
            }

            ProcessBuilder(listOf("gnome-terminal", "--") + command)
                .directory(File("/home/alex26/Documents/Laboratoare Materii/Sem2/SD/L7/Okazii"))
                .start()
            println("Proces restartat: $command")
            addToLog("Proces restartat: $command")
        } catch (e: Exception) {
            println("Eroare la restartul procesului: $e")
            addToLog("Eroare la restartul procesului: $e")
            log()
        }
    }

    init {
        clearLog()
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

                            println("[${message.split(":").last()}]: Se incepe un nou thread de citire")
                            addToLog("[${message.split(":").last()}]: Se incepe un nou thread de citire")

                            println("[${message.split(":").last()}]: Se incepe un nou thread de scriere")
                            addToLog("[${message.split(":").last()}]: Se incepe un nou thread de scriere")

                            val socketSub = CompositeDisposable()
                            subscriptions[socket] = socketSub
                            endedPeacefully[socket] = false

                            val readObs = readObservable(socket).share()

                            readSubscribe(readObs, socketSub)
                            writeSubscribe(socket, socketSub)
                            listenToResponses(socket, readObs, socketSub)
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
        receiveConnSub.add(connSubs)
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

    private fun writeSubscribe(socket: Socket, socketSub: CompositeDisposable) {
        val socketPosition = getSocketPosition(socket)
        if (socketPosition == -1) {
            println("Pozitia socketului e gresita")
            return
        }

        val writeSocketObservable = Observable
            .interval(PING_TIME_SECONDS.toLong(), TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .subscribeBy (
                onNext = {
                    try {
                        socket.getOutputStream().write("Ping:heartbeatMicroservice\n".toByteArray())
                        println("[${getSocketName(socket)}]: Am trimis un ping")
                        addToLog("[${getSocketName(socket)}]: Am trimis un ping")

                        Thread.sleep((PING_TIME_SECONDS / 2L) * 1000)

                        val isDead = if (socketPosition == BIDDER_PING) {
                            println("Socket viu - a raspuns in timp (port: ${getSocketName(socket)})")
                            val port = bidderSockets.find { it.first == socket }?.second
                            bidderRespondedToPing.find { it.first == port }?.second == false
                        } else {
                            !respondedToPing[socketPosition]
                        }

                        if (isDead) {
                            println("Socket mort - nu a raspuns in 2 secunde (port: ${getSocketName(socket)})")
                            addToLog("Socket mort: ${socket.port}")
                            throw TimeoutException("Nu a raspuns la timp")
                        }

                        assignRespondedToPing(socket, socketPosition, false)

                    } catch (e: Exception) {
                        endedPeacefully[socket]?.let { it1 ->
                            if (!it1)
                                println("Eroare la trimiterea pingului: $e")
                            log()
                            throw e
                        }
                    }
                },
                onError = {
                    endedPeacefully[socket]?.let { it1 ->
                        if (!it1)
                            println("S-a terminat fluxul: $it")
                        restartService(socket)
                    }
                }
            )
        socketSub.add(writeSocketObservable)
    }

    private fun listenToResponses(socket: Socket, readObs: Observable<String>, socketSub: CompositeDisposable) {
        val socketPosition = getSocketPosition(socket)
        if (socketPosition == -1) {
            println("Pozitia socketului e gresita")
            return
        }

        val sub = readObs
            .subscribeOn(Schedulers.io())
            .subscribeBy(
                onNext = {
                    if (it.startsWith("Response")) {
                        println("[${getSocketName(socket)}]: Am primit raspuns la ping: $it")
                        assignRespondedToPing(socket, socketPosition, true)
                    }
                },
                onError = {
                    println("Eroare la shared reader: $it")
                },
                onComplete = {
                    println("Gata")
                }
            )
        socketSub.add(sub)
    }

    private fun readSubscribe(socketObs: Observable<String>, socketSub: CompositeDisposable) {
        val sub = socketObs
            .subscribeOn(Schedulers.io())
            .subscribeBy (
                onNext = { message ->
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
        socketSub.add(sub)
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
                bidderRespondedToPing.addLast(Pair(localPort, false))

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
                endedPeacefully[auctioneerSocket as Socket] = true

                auctioneerSocket?.close()
                auctioneerSocket = null
                println("auctioneerMicroservice s-a terminat")
                addToLog("auctioneerMicroservice s-a terminat")

                subscriptions[auctioneerSocket]?.dispose()
            }

            "biddingProcessorMicroservice" -> {
                endedPeacefully[biddingProcessorSocket as Socket] = true

                biddingProcessorSocket?.close()
                biddingProcessorSocket = null
                println("biddingProcessorMicroservice s-a terminat")
                addToLog("biddingProcessorMicroservice s-a terminat")

                subscriptions[biddingProcessorSocket]?.dispose()
            }

            "messageProcessorMicroservice" -> {
                endedPeacefully[messageProcessorSocket as Socket] = true

                messageProcessorSocket?.close()
                messageProcessorSocket = null
                println("messageProcessorMicroservice s-a terminat")
                addToLog("messageProcessorMicroservice s-a terminat")

                subscriptions[messageProcessorSocket]?.dispose()
            }

            "bidderMicroservice" -> {
                try {
                    val localPort = message.split(":").last().split("-").last().toInt()

                    val bidder = bidderSockets.find { it.second == localPort }
                        ?: throw IllegalStateException("Bidder inexistent: $localPort")

                    endedPeacefully[bidder.first] = true

                    bidder.first.close()
                    bidderSockets.remove(bidder)
                    bidderRespondedToPing.remove(
                        bidderRespondedToPing.find {
                            it.first == localPort
                        }
                    )

                    subscriptions[bidder.first]?.dispose()

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
        subscriptions.forEach {
            it.value.dispose()
        }
    }
}

public fun connectToHeartbeat(heartbeatSocket: Socket, sourceSocket: String) {
    heartbeatSocket.getOutputStream().write("Init:$sourceSocket\n".toByteArray())
}

public fun respondToPing(heartbeatSocket: Socket, sourceSocket: String) {
    heartbeatSocket.getOutputStream().write("Response:$sourceSocket\n".toByteArray())
}

public fun endHeartbeatConnection(heartbeatSocket: Socket, sourceSocket: String) {
    heartbeatSocket.getOutputStream().write("End:$sourceSocket\n".toByteArray())
    heartbeatSocket.close()
}

fun main() {
    val heartbeatMicroservice = HeartbeatMicroservice()
    heartbeatMicroservice.run()
}



