import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import kotlin.system.exitProcess

class MessageProcessorMicroservice {
    private var messageProcessorSocket: ServerSocket
    private lateinit var biddingProcessorSocket: Socket
    private lateinit var heartbeatSocket: Socket
    private var auctioneerConnection:Socket
    private var receiveInQueueObservable: Observable<String>
    private val subscriptions = CompositeDisposable()
    private val messageQueue: Queue<Message> = LinkedList<Message>()
    private val initQueue: Queue<Message> = LinkedList()
    private val exceptionLog = mutableListOf<String>()

    companion object Constants {
        const val HEARTBEAT_PORT = 1800
        const val MESSAGE_PROCESSOR_PORT = 1600
        const val BIDDING_PROCESSOR_HOST = "localhost"
        const val BIDDING_PROCESSOR_PORT = 1700
    }

    private fun addToLog(message: String) = exceptionLog.add(message)

    private fun log() {
        try {
            val file = File("MessageProcessorMicroservice")
            if (!file.exists())
                throw FileNotFoundException("Nu a fost gasit log-ul pentru MessageProcessor")
            for (string in exceptionLog)
                file.appendText(string + "\n")
            exceptionLog.clear()
        } catch (e: FileNotFoundException) {
            println("Eroare: $e")
        }
    }

    init {
        connectToHeartbeat()

        messageProcessorSocket = ServerSocket(MESSAGE_PROCESSOR_PORT)
        println("MessageProcessorMicroservice se executa pe portul: ${messageProcessorSocket.localPort}")
        println("Se asteapta mesaje pentru procesare...")
        addToLog("Se asteapta mesaje pentru procesare...")

        // se asteapta mesaje primite de la AuctioneerMicroservice
        auctioneerConnection = messageProcessorSocket.accept()
        addToLog("S-a conectat auctioneer")
        val bufferReader = BufferedReader(InputStreamReader(auctioneerConnection.inputStream))

        // se creeaza obiectul Observable cu care se captureaza mesajele de la AuctioneerMicroservice
        receiveInQueueObservable = Observable.create<String> { emitter ->
            while (true) {
                // se citeste mesajul de la AuctioneerMicroservice de pe socketul TCP
                val receivedMessage = bufferReader.readLine()

                // daca se primeste un mesaj gol (NULL), atunci inseamna ca cealalta parte a socket-ului a fost inchisa
                if (receivedMessage == null) {
                    // deci subscriber-ul respectiv a fost deconectat
                    bufferReader.close()
                    auctioneerConnection.close()

                    emitter.onError(Exception("Eroare: AuctioneerMicroservice ${auctioneerConnection.port} a fost deconectat."))

                    addToLog("Eroare: AuctioneerMicroservice ${auctioneerConnection.port} a fost deconectat.")
                    log()

                    break
                }

                // daca mesajul este cel de incheiere a licitatiei (avand corpul "final"), atunci se emite semnalul Complete
                if (Message.deserialize(receivedMessage.toByteArray()).body == "final") {
                    println("Am primit mesajul final")
                    addToLog("Am primit mesajul final")
                    log()
                    emitter.onComplete()

                    break
                } else {
                    // se emite ce s-a citit ca si element in fluxul de mesaje
                    emitter.onNext(receivedMessage)
                }
            }
        }
    }

    private fun connectToHeartbeat() {
        heartbeatSocket = Socket("localhost", HEARTBEAT_PORT)
        heartbeatSocket.getOutputStream().write("Init:messageProcessorMicroservice\n".toByteArray())
    }

    private fun receiveAndProcessMessages() {
        // se primesc si se adauga in coada mesajele de la AuctioneerMicroservice
        println("Incepe procesarea: ")
        val receiveInQueueSubscription = receiveInQueueObservable
            .subscribeBy(
                onNext = {
                    val message = Message.deserialize(it.toByteArray())
                    println(message)
                    addToLog(message.toString())

                    if (message.body.startsWith("Init:"))
                        initQueue.add(message)

                    else {
                        if (!messageQueue.contains(message)) {
                            messageQueue.add(message)
                            addToLog("Am eliminat um mesaj duplicat: $message")
                        }
                    }

                },
                onComplete = {
                    // s-a incheiat primirea tuturor mesajelor

                    val sortedMessageQueue = messageQueue.sortedBy {
                        it.body.split(" ")[1].toInt()
                    }

                    addToLog("Am sortat mesajele")

                    messageQueue.clear()
                    sortedMessageQueue.forEach{
                        val initMessage = initQueue.find { initMessage->
                            initMessage.sender.endsWith(it.sender.split(":").last())
                        }
                        val parts = initMessage?.body?.split(":")?.last()?.split("&")

                        val name = parts?.get(0)?.split("=")?.last()
                        val phoneNumber = parts?.get(1)?.split("=")?.last()
                        val email = parts?.get(2)?.split("=")?.last()

                        val newMessage = Message.create("$name|$phoneNumber|$email", it.body)
                        messageQueue.add(newMessage)
                    }

                    addToLog("Am procesat datele din mesajul de init")

                    // s-au primit toate mesajele de la AuctioneerMicroservice, i se trimite un mesaj pentru a semnala
                    // acest lucru
                    val finishedMessagesMessage = Message.create(
                        "${auctioneerConnection.localAddress}:${auctioneerConnection.localPort}",
                        "am primit tot"
                    )

                    addToLog("Am primit toate mesajele")

                    auctioneerConnection.getOutputStream().write(finishedMessagesMessage.serialize())
                    auctioneerConnection.close()

                    // se trimit mai departe mesajele procesate catre BiddingProcessor
                    sendProcessedMessages()
                },
                onError = { println("Eroare: $it") }
            )
        subscriptions.add(receiveInQueueSubscription)
    }

    private fun sendProcessedMessages() {
        try {
            biddingProcessorSocket = Socket(BIDDING_PROCESSOR_HOST, BIDDING_PROCESSOR_PORT)

            println("Trimit urmatoarele mesaje:")
            addToLog("Trimit urmatoarele mesaje:")
            Observable.fromIterable(messageQueue).subscribeBy(
                onNext = {
                    println(it.toString())
                    addToLog(it.toString())

                    // trimitere mesaje catre procesorul licitatiei, care decide rezultatul final
                    biddingProcessorSocket.getOutputStream().write(it.serialize())
                },
                onComplete = {
                    val noMoreMessages = Message.create(
                        "${biddingProcessorSocket.localAddress}:${biddingProcessorSocket.localPort}",
                        "final"
                    )
                    addToLog("Am trimis toate mesajele")
                    biddingProcessorSocket.getOutputStream().write(noMoreMessages.serialize())
                    biddingProcessorSocket.close()

                    // se elibereaza memoria din multimea de Subscriptions
                    subscriptions.dispose()
                }
            )
        } catch (e: Exception) {
            println("Nu ma pot conecta la BiddingProcessor!")
            addToLog("Nu ma pot conecta la BiddingProcessor!")
            log()
            messageProcessorSocket.close()
            exitProcess(1)
        }
    }

    fun run() {
        receiveAndProcessMessages()
    }
}

fun main(args: Array<String>) {
    val messageProcessorMicroservice = MessageProcessorMicroservice()
    messageProcessorMicroservice.run()
}