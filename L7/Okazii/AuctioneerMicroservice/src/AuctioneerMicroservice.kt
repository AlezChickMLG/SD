import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.*
import kotlin.system.exitProcess

class AuctioneerMicroservice {
    private var auctioneerSocket: ServerSocket
    private lateinit var messageProcessorSocket: Socket
    private lateinit var heartbeatSocket: Socket
    private var receiveBidsObservable: Observable<String>
    private val subscriptions = CompositeDisposable()
    private val bidQueue: Queue<Message> = LinkedList<Message>()
    private val bidderConnections: MutableList<Socket> = mutableListOf()
    private val exceptionLog: MutableList<String> = mutableListOf()

    companion object Constants {
        const val MESSAGE_PROCESSOR_HOST = "localhost"
        const val HEARTBEAT_PORT = 1800
        const val MESSAGE_PROCESSOR_PORT = 1600
        const val AUCTIONEER_PORT = 1500
        const val AUCTION_DURATION: Long = 15_000 // licitatia dureaza 15 secunde
    }

    private fun addToLog(message: String) = exceptionLog.add(message)

    private fun log() {
        try {
            val file = File("AuctioneerMicroserviceLog")
            if (!file.exists())
                throw FileNotFoundException("Nu a fost gasit log-ul pentru Auctioneer")
            for (string in exceptionLog)
                file.appendText(string + "\n")
            exceptionLog.clear()
        } catch (e: FileNotFoundException) {
            println("Eroare: $e")
        }
    }

    init {
        connectToHeartbeat()
        listenToHeartbeat()

        auctioneerSocket = ServerSocket(AUCTIONEER_PORT)
        auctioneerSocket.setSoTimeout(AUCTION_DURATION.toInt())
        println("AuctioneerMicroservice2 se executa pe portul: ${auctioneerSocket.localPort}")
        println("Se asteapta oferte de la bidderi...")

        // se creeaza obiectul Observable cu care se genereaza evenimente cand se primesc oferte de la bidderi
        receiveBidsObservable = Observable.create<String> { emitter ->
            // se asteapta conexiuni din partea bidderilor
            while (true) {
                try {
                    val bidderConnection = auctioneerSocket.accept()
                    bidderConnections.add(bidderConnection)

                    // se citeste mesajul de la bidder de pe socketul TCP
                    val bufferReader = BufferedReader(InputStreamReader(bidderConnection.inputStream))
                    val receivedInitMessage = bufferReader.readLine()

                    //mesaj de init
                    // daca se primeste un mesaj gol (NULL), atunci inseamna ca cealalta parte a socket-ului a fost inchisa
                    if (receivedInitMessage == null) {
                        // deci subscriber-ul respectiv a fost deconectat
                        bufferReader.close()
                        bidderConnection.close()

                        emitter.onError(Exception("Eroare: Bidder-ul ${bidderConnection.port} a fost deconectat."))
                    }

                    // se emite ce s-a citit ca si element in fluxul de mesaje
                    emitter.onNext(receivedInitMessage)

                    //licitatia
                    val receivedMessage = bufferReader.readLine()

                    // daca se primeste un mesaj gol (NULL), atunci inseamna ca cealalta parte a socket-ului a fost inchisa
                    if (receivedMessage == null) {
                        // deci subscriber-ul respectiv a fost deconectat
                        bufferReader.close()
                        bidderConnection.close()

                        emitter.onError(Exception("Eroare: Bidder-ul ${bidderConnection.port} a fost deconectat."))
                        log()
                    }
                    // se emite ce s-a citit ca si element in fluxul de mesaje
                    emitter.onNext(receivedMessage)

                } catch (e: SocketTimeoutException) {
                    // daca au trecut cele 15 secunde de la pornirea licitatiei, inseamna ca licitatia s-a incheiat
                    // se emite semnalul Complete pentru a incheia fluxul de oferte
                    startAuctionHeartbeat()
                    emitter.onComplete()
                    break
                }
            }
        }
    }

    private fun listenToHeartbeat() {
        val listenObservable = Observable.create<String> { emitter ->
            val reader = BufferedReader(InputStreamReader(heartbeatSocket.inputStream))

            while (!emitter.isDisposed) {
                try {
                    val message = reader.readLine()
                    emitter.onNext(message)
                } catch (e: Exception) {
                    println("Eroare la citirea mesajelor de la heartbeat")
                    addToLog("Eroare la citirea mesajelor de la heartbeat")
                    log()
                }
            }
            emitter.onComplete()
        }

        val listenSubscribe = listenObservable
            .subscribeOn(Schedulers.io())
            .subscribeBy (
                onNext = {
                    val messageType = it.split(":").first()
                    if (messageType == "Ping") {
                        respondToPing()
                        println("Am trimis raspuns la pingul heartbeatului")
                    }
                },
                onComplete = {
                    println("Heartbeat-ul a fost inchis")
                    addToLog("Heartbeat-ul a fost inchis")
                }
            )
        subscriptions.add(listenSubscribe)
    }

    private fun connectToHeartbeat() {
        heartbeatSocket = Socket("localhost", HEARTBEAT_PORT)
        heartbeatSocket.getOutputStream().write("Init:auctioneerMicroservice\n".toByteArray())
    }

    private fun respondToPing() {
        heartbeatSocket.getOutputStream().write("Response:auctioneerMicroservice\n".toByteArray())
    }

    private fun startAuctionHeartbeat() {
        heartbeatSocket.getOutputStream().write("Start:\n".toByteArray())
    }

    private fun endHeartbeatConnection() {
        heartbeatSocket.getOutputStream().write("End:auctioneerMicroservice\n".toByteArray())
        heartbeatSocket.close()
    }

    private fun receiveBids() {
        // se incepe prin a primi ofertele de la bidderi
        val receiveBidsSubscription = receiveBidsObservable.subscribeBy(
            onNext = {
                val message = Message.deserialize(it.toByteArray())
                println(message)
                bidQueue.add(message)
                addToLog("Am adaugat mesajul $message")
            },
            onComplete = {
                // licitatia s-a incheiat
                // se trimit raspunsurile mai departe catre procesorul de mesaje
                println("Licitatia s-a incheiat! Se trimit ofertele spre procesare...")
                addToLog("Licitatia s-a incheiat! Se trimit ofertele spre procesare...")
                forwardBids()
            },
            onError = {
                println("Eroare: $it")
                addToLog(it.toString())
            }
        )
        subscriptions.add(receiveBidsSubscription)
    }

    private fun forwardBids() {
        try {
            messageProcessorSocket = Socket(MESSAGE_PROCESSOR_HOST, MESSAGE_PROCESSOR_PORT)
            subscriptions.add(Observable.fromIterable(bidQueue).subscribeBy(
                onNext = {
                    // trimitere mesaje catre procesorul de mesaje
                    messageProcessorSocket.getOutputStream().write(it.serialize())
                    println("Am trimis mesajul: $it")
                    addToLog("Am trimis catre MessageProcessor mesajul: $it")
                },
                onComplete = {
                    println("Am trimis toate ofertele catre MessageProcessor.")
                    addToLog("Am trimis toate ofertele catre MessageProcessor.")
                    val bidEndMessage = Message.create(
                        "${messageProcessorSocket.localAddress}:${messageProcessorSocket.localPort}",
                        "final"
                    )
                    messageProcessorSocket.getOutputStream().write(bidEndMessage.serialize())

                    // dupa ce s-a terminat licitatia, se asteapta raspuns de la MessageProcessorMicroservice
                    // cum ca a primit toate mesajele
                    val bufferReader = BufferedReader(InputStreamReader(messageProcessorSocket.inputStream))
                    bufferReader.readLine()

                    messageProcessorSocket.close()

                    finishAuction()
                }
            ))
        } catch (e: Exception) {
            println("Nu ma pot conecta la MessageProcessor!")
            addToLog("Nu ma pot conecta la MessageProcessor!")
            auctioneerSocket.close()
            log()
            exitProcess(1)
        }
    }

    private fun finishAuction() {
        // se asteapta rezultatul licitatiei
        try {
            val biddingProcessorConnection = auctioneerSocket.accept()
            val bufferReader = BufferedReader(InputStreamReader(biddingProcessorConnection.inputStream))

            // se citeste rezultatul licitatiei de la AuctioneerMicroservice de pe socketul TCP
            val receivedMessage = bufferReader.readLine()

            val result: Message = Message.deserialize(receivedMessage.toByteArray())
            val winningPrice = result.body.split(" ")[1].toInt()
            println("Am primit rezultatul licitatiei de la BiddingProcessor: ${result.sender} a castigat cu pretul: $winningPrice")
            addToLog("Am primit rezultatul licitatiei de la BiddingProcessor: ${result.sender} a castigat cu pretul: $winningPrice")

            // se creeaza mesajele pentru rezultatele licitatiei
            val winningMessage = Message.create(auctioneerSocket.localSocketAddress.toString(),
                "Licitatie castigata! Pret castigator: $winningPrice")
            val losingMessage = Message.create(auctioneerSocket.localSocketAddress.toString(),
                "Licitatie pierduta...")

            // se anunta castigatorul
            bidderConnections.forEach {
                if (it.remoteSocketAddress.toString() == result.sender) {
                    it.getOutputStream().write(winningMessage.serialize())
                } else {
                    it.getOutputStream().write(losingMessage.serialize())
                }
                it.close()
            }
        } catch (e: Exception) {
            println("Nu ma pot conecta la BiddingProcessor!")
            addToLog("Nu ma pot conecta la BiddingProcessor!")
            auctioneerSocket.close()
            log()
            exitProcess(1)
        }

        // se elibereaza memoria din multimea de Subscriptions
        subscriptions.dispose()
    }

    fun run() {
        receiveBids()
        endHeartbeatConnection()
    }
}

fun main(args: Array<String>) {
    val bidderMicroservice = AuctioneerMicroservice()
    bidderMicroservice.run()
}