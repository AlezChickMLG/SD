import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import kotlin.Exception
import kotlin.random.Random
import kotlin.system.exitProcess
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.io.File
import java.io.FileNotFoundException
import kotlin.random.nextInt

//import Message

class BidderMicroservice {
    private var auctioneerSocket: Socket
    private var auctionResultObservable: Observable<String>
    private lateinit var heartbeatSocket: Socket
    private var myIdentity: String = "[BIDDER_NECONECTAT]"
    private var name: String = ""
    private var phone_number: String = ""
    private var email: String = ""
    private val exceptionLog: MutableList<String> = mutableListOf()

    companion object Constants {
        const val HEARTBEAT_PORT = 1800
        const val AUCTIONEER_HOST = "localhost"
        const val AUCTIONEER_PORT = 1500
        const val MAX_BID = 10_000
        const val MIN_BID = 1_000
    }

    private fun addToLog(message: String) = exceptionLog.add(message)

    private fun log() {
        try {
            val file = File("BidderMicroserviceLog")
            if (!file.exists())
                throw FileNotFoundException("Nu a fost gasit log-ul pentru Bidder")
            for (string in exceptionLog)
                file.appendText(string + "\n")
            exceptionLog.clear()
        } catch (e: FileNotFoundException) {
            println("Eroare: $e")
        }
    }

    init {
        try {
            auctioneerSocket = Socket(AUCTIONEER_HOST, AUCTIONEER_PORT)
            println("M-am conectat la Auctioneer!")
            addToLog("M-am conectat la Auctioneer!")

            myIdentity = "[${auctioneerSocket.localPort}]"
            addToLog("Identitatea mea: [${myIdentity}]")

            connectToHeartbeat()

//            name = readLine() ?: ""
//            phone_number = readLine() ?: ""
//            email = readLine() ?: ""

            name = listOf("Alex", "Cosmin", "Matei", "Stefan", "Sebi", "Marcel", "Luca", "Corcodus", "Bogdan", "Mircea")[Random.nextInt(0, 10)] + Random.nextInt(1, 101).toString()
            addToLog("[${myIdentity}] name: $name")

            phone_number = "07" + (1..8)
                    .map { Random.nextInt(0, 10) }
                    .joinToString("")
            addToLog("[${myIdentity}] phone_number: $phone_number")

            email = "$name@gmail.com"
            addToLog("[${myIdentity}] email: $email")

            val initMessage = Message.create("${auctioneerSocket.localAddress}:${auctioneerSocket.localPort}", "Init:name=$name&phone_number=$phone_number&email=$email").serialize()
            addToLog("[${myIdentity}] initMessage: $initMessage")

            auctioneerSocket.getOutputStream().write(initMessage)

            // se creeaza un obiect Observable ce va emite mesaje primite printr-un TCP

            // fiecare mesaj primit reprezinta un element al fluxului de date reactiv
            auctionResultObservable = Observable.create<String>
            { emitter ->
                // se citeste raspunsul de pe socketul TCP
                val bufferReader = BufferedReader(InputStreamReader(auctioneerSocket.inputStream))
                val receivedMessage = bufferReader.readLine()

                // daca se primeste un mesaj gol (NULL), atunci inseamna ca cealalta parte a socket-ului a fost inchisa
                if (receivedMessage == null) {
                    bufferReader.close()
                    auctioneerSocket.close()

                    emitter.onError(Exception("AuctioneerMicroservice s-a deconectat."))
                    addToLog("[${auctioneerSocket.localPort}] AuctioneerMicroservice s-a deconectat")
                    log()

                    return@create
                }
                // mesajul primit este emis in flux
                emitter.onNext(receivedMessage)

                // deoarece se asteapta un singur mesaj, in continuare se emite semnalul de incheiere al fluxului
                emitter.onComplete()
                bufferReader.close()
                auctioneerSocket.close()
            }

        } catch (e: Exception) {
            println("$myIdentity Nu ma pot conecta la Auctioneer!: $e")

            addToLog("[${myIdentity}] Nu ma pot conecta la Auctioneer")
            log()

            exitProcess(1)
        }
    }

    private fun connectToHeartbeat() {
        heartbeatSocket = Socket("localhost", HEARTBEAT_PORT)
        heartbeatSocket.getOutputStream().write("Init:bidderMicroservice-${auctioneerSocket.localPort}\n".toByteArray())
    }

    private fun endHeartbeatConnection() {
        heartbeatSocket.getOutputStream().write("End:bidderMicroservice-${auctioneerSocket.localPort}\n".toByteArray())
        heartbeatSocket.close()
    }

    private fun bid() {
        // se genereaza o oferta aleatorie din partea bidderului curent
        val pret = Random.nextInt(MIN_BID, MAX_BID)
        addToLog("[${myIdentity}] pret: $pret")

        // se creeaza mesajul care incapsuleaza oferta
        val biddingMessage = Message.create("${auctioneerSocket.localAddress}:${auctioneerSocket.localPort}", "licitez $pret")
        addToLog("[${myIdentity}] biddingMessage: $biddingMessage")

        // bidder-ul trimite pretul pentru care doreste sa liciteze
        val serializedMessage = biddingMessage.serialize()
        auctioneerSocket.getOutputStream().write(serializedMessage)
        addToLog("[${myIdentity}] biddingMessage a fost trimis")

        // exista o sansa din 2 ca bidder-ul sa-si trimita oferta de 2 ori, eronat
        if (Random.nextBoolean()) {
            auctioneerSocket.getOutputStream().write(serializedMessage)
            addToLog("[${myIdentity}] a fost generat al doilea mesaj")
        }
    }

    private fun waitForResult() {
        println("$myIdentity Astept rezultatul licitatiei...")
        addToLog("[${myIdentity}] Astept rezultatul licitatiei...")
        // bidder-ul se inscrie pentru primirea unui raspuns la oferta trimisa de acesta
        val auctionResultSubscription = auctionResultObservable.subscribeBy(
        // cand se primeste un mesaj in flux, inseamna ca a sosit rezultatul licitatiei
                        onNext = {
                    val resultMessage: Message = Message.deserialize(it.toByteArray())
                    println("$myIdentity Rezultat licitatie:${resultMessage.body}")
                            addToLog("[${myIdentity}] Rezultat licitatie:${resultMessage.body}")
                },
                onError = {
                    println("$myIdentity Eroare: $it")
                    addToLog("[${myIdentity}] Eroare: $it")
                }
            )
        // se elibereaza memoria obiectului Subscription
        auctionResultSubscription.dispose()
    }

    fun run() {
        bid()
        waitForResult()
        endHeartbeatConnection()
    }
}

fun main(args: Array<String>) {
    val bidderMicroservice = BidderMicroservice()
    bidderMicroservice.run()
}