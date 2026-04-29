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
import java.util.*
import kotlin.system.exitProcess

class BiddingProcessorMicroservice {
    private var biddingProcessorSocket: ServerSocket
    private lateinit var auctioneerSocket: Socket
    private val heartbeatSocket: Socket = Socket("localhost", HEARTBEAT_PORT)
    private var receiveProcessedBidsObservable: Observable<String>
    private val subscriptions = CompositeDisposable()
    private val processedBidsQueue: Queue<Message> = LinkedList<Message>()
    private val exceptionLog = mutableListOf<String>()

    companion object Constants {
        const val HEARTBEAT_PORT = 1800
        const val BIDDING_PROCESSOR_PORT = 1700
        const val AUCTIONEER_PORT = 1500
        const val AUCTIONEER_HOST = "localhost"
    }

    private fun addToLog(message: String) = exceptionLog.add(message)

    private fun log() {
        try {
            val file = File("BiddingProcessorMicroserviceLog")
            if (!file.exists())
                throw FileNotFoundException("Nu a fost gasit log-ul pentru BiddingProcessor")
            for (string in exceptionLog)
                file.appendText(string + "\n")
            exceptionLog.clear()
        } catch (e: FileNotFoundException) {
            println("Eroare: $e")
        }
    }

    init {
        connectToHeartbeat(heartbeatSocket, "biddingProcessorMicroservice")
        listenToHeartbeat()

        biddingProcessorSocket = ServerSocket(BIDDING_PROCESSOR_PORT)
        println("BiddingProcessorMicroservice se executa pe portul: ${biddingProcessorSocket.localPort}")
        println("Se asteapta ofertele pentru finalizarea licitatiei...")
        addToLog("Se asteapta ofertele pentru finalizarea licitatiei...")

        // se asteapta mesaje primite de la MessageProcessorMicroservice
        val messageProcessorConnection = biddingProcessorSocket.accept()
        addToLog("S-a conectat biddingProcessor")
        val bufferReader = BufferedReader(InputStreamReader(messageProcessorConnection.inputStream))

        // se creeaza obiectul Observable cu care se captureaza mesajele de la MessageProcessorMicroservice
        receiveProcessedBidsObservable = Observable.create<String> { emitter ->
            while (true) {
                // se citeste mesajul de la MessageProcessorMicroservice de pe socketul TCP
                val receivedMessage = bufferReader.readLine()

                // daca se primeste un mesaj gol (NULL), atunci inseamna ca cealalta parte a socket-ului a fost inchisa
                if (receivedMessage == null) {
                    // deci MessageProcessorMicroservice a fost deconectat
                    bufferReader.close()
                    messageProcessorConnection.close()

                    addToLog("MessageProcessor ${messageProcessorConnection.port} a fost deconectat")

                    emitter.onError(Exception("Eroare: MessageProcessorMicroservice ${messageProcessorConnection.port} a fost deconectat."))
                    break
                }

                // daca mesajul este cel de tip „FINAL DE LISTA DE MESAJE” (avand corpul "final"), atunci se emite semnalul Complete
                if (Message.deserialize(receivedMessage.toByteArray()).body == "final") {
                    emitter.onComplete()

                    // s-au primit toate mesajele de la MessageProcessorMicroservice, i se trimite un mesaj pentru a semnala
                    // acest lucru
                    val finishedBidsMessage = Message.create(
                        "${messageProcessorConnection.localAddress}:${messageProcessorConnection.localPort}",
                        "am primit tot"
                    )
                    addToLog("S-au primit toate mesajele de la messageProcessor")

                    messageProcessorConnection.getOutputStream().write(finishedBidsMessage.serialize())
                    messageProcessorConnection.close()

                    break
                } else {
                    // se emite ce s-a citit ca si element in fluxul de mesaje
                    emitter.onNext(receivedMessage)
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
                        respondToPing(heartbeatSocket, "biddingProcessorMicroservice")
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

    private fun receiveProcessedBids() {
        // se primesc si se adauga in coada ofertele procesate de la MessageProcessorMicroservice
        val receiveProcessedBidsSubscription = receiveProcessedBidsObservable
            .subscribeBy(
                onNext = {
                    val message = Message.deserialize(it.toByteArray())
                    println(message)
                    addToLog(message.toString())
                    processedBidsQueue.add(message)
                },
                onComplete = {
                    // s-a incheiat primirea tuturor mesajelor
                    // se decide castigatorul licitatiei
                    decideAuctionWinner()
                    addToLog("se decide castigatorul licitatiei")
                },
                onError = { println("Eroare: $it") }
            )
        subscriptions.add(receiveProcessedBidsSubscription)
    }

    private fun decideAuctionWinner() {
        // se calculeaza castigatorul ca fiind cel care a ofertat cel mai mult
        val winner: Message? = processedBidsQueue.toList().maxByOrNull {
            // corpul mesajului e de forma "licitez <SUMA_LICITATA>"
            // se preia a doua parte, separata de spatiu
            it.body.split(" ")[1].toInt()
        }

        println("Castigatorul este: ${winner?.sender}")
        addToLog("Castigatorul este: ${winner?.sender}")

        try {
            auctioneerSocket = Socket(AUCTIONEER_HOST, AUCTIONEER_PORT)
            addToLog("S-a conectat la auctioneer")

            // se trimite castigatorul catre AuctioneerMicroservice
            auctioneerSocket.getOutputStream().write(winner!!.serialize())
            auctioneerSocket.close()

            addToLog("A fost anuntat castigatorul")

            println("Am anuntat castigatorul catre AuctioneerMicroservice.")
        } catch (e: Exception) {
            println("Nu ma pot conecta la Auctioneer!")
            addToLog("Nu ma pot conecta la Auctioneer!")
            log()
            biddingProcessorSocket.close()
            exitProcess(1)
        }
    }

    fun run() {
        receiveProcessedBids()

        // se elibereaza memoria din multimea de Subscriptions
        subscriptions.dispose()
        endHeartbeatConnection(heartbeatSocket, "biddingProcessorMicroservice")
    }
}

fun main(args: Array<String>) {
    val biddingProcessorMicroservice = BiddingProcessorMicroservice()
    biddingProcessorMicroservice.run()
}