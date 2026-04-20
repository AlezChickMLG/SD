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
import javax.annotation.processing.Messager

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

    companion object Constants{
        const val HEARTBEAT_PORT = 1800
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

                            println("Se incepe un nou thread pentru ${message.split(":").last()}")
                            addToLog("Se incepe un nou thread pentru ${message.split(":").last()}")
                            socketSubscribe(socketObservable(socket))
                        }

//                    else if (message == "Start") {
//                        if (auctioneerSocket != null && biddingProcessorSocket != null && messageProcessorSocket != null && bidderSockets.isNotEmpty()) {
//
//                        }
//                    }

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

    private fun socketObservable(socket: Socket): Observable<String> {
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

    private fun socketSubscribe(socketObs: Observable<String>) {
        val sub = socketObs
            .subscribeOn(Schedulers.io())
            .subscribeBy (
                onNext = { message ->
                    println(message)
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
    }

    private fun processInitMessage(socket: Socket, message: String) {
        var whichMicroservice = message.split(":").last()
        if (whichMicroservice.contains("-"))
            whichMicroservice = whichMicroservice.split("-").first()

        when (whichMicroservice) {
            "auctioneerMicroservice" -> {
                if (auctioneerSocket == null) {
                    auctioneerSocket = socket
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
                    println("S-a conectat biddingProcessor")
                    addToLog("A fost actualizat biddingProcessorSocket")
                } else {
                    println("BiddingProcessorSocket deja exista")
                    addToLog("BiddingProcessorSocket deja exista")
                }
            }

            "bidderMicroservice" -> {
                val localPort = message.split(":").last().split("-").last().toInt()

                bidderSockets.add(Pair(socket, localPort))
                println("S-a conectat un bidder:$localPort")
                addToLog("A fost adaugat un bidder")
            }
        }
    }

    fun run() {
        receiveConnections()
    }
}

fun main() {
    val heartbeatMicroservice = HeartbeatMicroservice()
    heartbeatMicroservice.run()
}

