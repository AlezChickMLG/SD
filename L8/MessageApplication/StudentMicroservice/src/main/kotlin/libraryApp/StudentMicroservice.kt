package libraryApp

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.Exception
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class StudentMicroservice {
    // intrebarile si raspunsurile sunt mentinute intr-o lista de perechi de forma:
    // [<INTREBARE 1, RASPUNS 1>, <INTREBARE 2, RASPUNS 2>, ... ]
    private lateinit var questionDatabase: MutableList<Pair<String, String>>
    private lateinit var messageManagerSocket: Socket

    //id-ul studentului
    private lateinit var studentID: Number

    //server socket pentru gui pe port: 2000 + ID-ul studentului
    private lateinit var guiSocket: ServerSocket
    private lateinit var guiClient: Socket

    init {
        val databaseLines: List<String> = File("questions_database.txt").readLines()
        questionDatabase = mutableListOf()

        /*
         "baza de date" cu intrebari si raspunsuri este de forma:

         <INTREBARE_1>\n
         <RASPUNS_INTREBARE_1>\n
         <INTREBARE_2>\n
         <RASPUNS_INTREBARE_2>\n
         ...
         */
        for (i in 0..(databaseLines.size - 1) step 2) {
            questionDatabase.add(Pair(databaseLines[i], databaseLines[i + 1]))
        }

        studentID = try {
            System.getenv("STUDENT_ID").toInt()
        } catch (e: kotlin.Exception) {
            println("Eroare la gasirea variabilei de mediu STUDENT_ID")
            0
        }
    }

    companion object Constants {
        // pentru testare, se foloseste localhost. pentru deploy, server-ul socket (microserviciul MessageManager) se identifica dupa un "hostname"
        // acest hostname poate fi trimis (optional) ca variabila de mediu
        val MESSAGE_MANAGER_HOST = System.getenv("MESSAGE_MANAGER_HOST") ?: "localhost"
        const val MESSAGE_MANAGER_PORT = 1500
    }

    private fun sendInitMessage() {
        messageManagerSocket.getOutputStream().write("Init:student$studentID\n".toByteArray())
    }

    private fun listenToGUI() {
        thread {
            val guiPort = 2000 + studentID.toInt()

            guiSocket = ServerSocket(guiPort)
            println("Am creat un server pe portul $guiPort")

            while (true) {
                guiClient = guiSocket.accept()
                //println("S-a conectat un client pe portul: ${guiClient.port}")

                thread {
                    val reader = BufferedReader(InputStreamReader(guiClient.inputStream))

                    while (true) {
                        val question = reader.readLine()

                        if (question == null) {
                            guiClient.close()
                            //println("S-a terminat conexiunea pe portul: ${guiClient.port}")
                            break
                        }

                        println("Intrebare de la GUI: $question")

                        //messageManagerSocket.getOutputStream.write()
                    }
                }
            }
        }
    }

    private fun subscribeToMessageManager() {
        try {
            messageManagerSocket = Socket(MESSAGE_MANAGER_HOST, MESSAGE_MANAGER_PORT)
            sendInitMessage()
            println("M-am conectat la MessageManager!")
        } catch (e: Exception) {
            println("Nu ma pot conecta la MessageManager!")
            exitProcess(1)
        }
    }

    private fun respondToQuestion(question: String): String? {
        questionDatabase.forEach {
            // daca se gaseste raspunsul la intrebare, acesta este returnat apelantului
            if (it.first == question) {
                return it.second
            }
        }
        return null
    }

    public fun run() {
        // microserviciul se inscrie in lista de "subscribers" de la MessageManager prin conectarea la acesta
        subscribeToMessageManager()
        listenToGUI()

        println("StudentMicroservice se executa pe portul: ${messageManagerSocket.localPort}")
        println("Se asteapta mesaje...")

        val bufferReader = BufferedReader(InputStreamReader(messageManagerSocket.inputStream))

        while (true) {
            // se asteapta intrebari trimise prin intermediarul "MessageManager"
            val response = bufferReader.readLine()

            if (response == null) {
                // daca se primeste un mesaj gol (NULL), atunci inseamna ca cealalta parte a socket-ului a fost inchisa
                println("Microserviciul MessageService (${messageManagerSocket.port}) a fost oprit.")
                bufferReader.close()
                messageManagerSocket.close()
                break
            }

            // se foloseste un thread separat pentru tratarea intrebarii primite
            thread {
                val (messageType, messageDestination, messageBody) = response.split(" ", limit = 3)

                when(messageType) {
                    // tipul mesajului cunoscut de acest microserviciu este de forma:
                    // intrebare <DESTINATIE_RASPUNS> <CONTINUT_INTREBARE>
                    "intrebare" -> {
                        println("Am primit o intrebare de la $messageDestination: \"${messageBody}\"")
                        var responseToQuestion = respondToQuestion(messageBody)
                        responseToQuestion?.let {
                            responseToQuestion = "raspuns $messageDestination $it"
                            println("Trimit raspunsul: \"${response}\"")
                            messageManagerSocket.getOutputStream().write((responseToQuestion + "\n").toByteArray())
                        }
                    }
                }
            }
        }
    }
}

fun main() {
    val studentMicroservice = StudentMicroservice()
    studentMicroservice.run()
}