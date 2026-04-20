import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.TimeoutException
import javax.lang.model.element.UnknownElementException

class UnknownMessageException(message: String) : Exception(message)
class SocketAlreadyInitializedException(message: String) : Exception(message)

class HeartbeatMicroservice {
    private var heartbeatSocket: ServerSocket = ServerSocket(HEARTBEAT_PORT)
    private var auctioneerSocket: Socket? = null
    private var biddingProcessorSocket: Socket? = null
    private var messageProcessorSocket: Socket? = null
    private var bidderSockets = mutableListOf<Socket>()
    private var exceptionLog = mutableListOf<String>()

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
        while (true) {
            try {
                println("Astept sa se conecteze microservicii...")
                val socket = heartbeatSocket.accept()
                println("S-a conectat un microserviciu")
                addToLog("S-a conectat un microserviciu")
                val bufferReader = BufferedReader(InputStreamReader(socket.inputStream))

                val initMessage = bufferReader.readLine()
                println("Message received: $initMessage")

                addToLog("Init message: $initMessage")
                if (initMessage.split(":")[0] != "Init")
                    throw UnknownMessageException("Mesaj")

                val whichMicroservice = initMessage.split(":").last()

                when(whichMicroservice) {
                    "auctioneerMicroservice" -> {
                        if (auctioneerSocket == null) {
                            auctioneerSocket = socket
                            println("S-a conectat auctioneer")
                            addToLog("A fost actualizat auctioneerSocket")
                        }
                        else {
                            println("AuctioneerSocket deja exista")
                            addToLog("AuctioneerSocket deja exista")
                        }
                    }

                    "messageProcessorMicroservice" -> {
                        if (messageProcessorSocket == null) {
                            messageProcessorSocket = socket
                            println("S-a conectat messageProcessor")
                            addToLog("A fost actualizat messageProcessorSocket")
                        }
                        else {
                            println("MessageProcessorSocket deja exista")
                            addToLog("AuctioneerSocket deja exista")
                        }
                    }

                    "biddingProcessorMicroservice" -> {
                        if (biddingProcessorSocket == null) {
                            biddingProcessorSocket = socket
                            println("S-a conectat biddingProcessor")
                            addToLog("A fost actualizat biddingProcessorSocket")
                        }
                        else {
                            println("BiddingProcessorSocket deja exista")
                            addToLog("BiddingProcessorSocket deja exista")
                        }
                    }

                    "bidderMicroservice" -> {
                        bidderSockets.add(socket)
                        println("S-a conectat un bidder")
                        addToLog("A fost adaugat un bidder")
                    }
                }
            } catch (e: Exception) {
                println("Eroare: $e")
                break
                log()
            }
        }
        log()
    }
}

fun main() {
    val heartbeatMicroservice = HeartbeatMicroservice()
}

