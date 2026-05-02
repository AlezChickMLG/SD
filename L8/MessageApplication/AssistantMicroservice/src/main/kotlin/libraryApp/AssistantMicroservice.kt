package libraryApp

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import kotlin.system.exitProcess

class AssistantMicroservice {
    private lateinit var messageManagerSocket: Socket
    private lateinit var assistantMicroserviceServerSocket: ServerSocket
    private lateinit var heartbeatSocket: Socket
    private lateinit var idDatabaseSocket: Socket

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val studentIDMap = ConcurrentHashMap<String, Int>()
    private val studentIDMapMutex = Mutex()

    private val idRequests = ConcurrentHashMap<String, CompletableDeferred<Int>>()
    private val idRequestMutex = Mutex()

    companion object Constants {
        const val MESSAGE_MANAGER_HOST = "localhost"
        const val MESSAGE_MANAGER_PORT = 1500
        const val ID_DATABASE_PORT = 2200
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

    private fun connectToIdDatabase() {
        try {
            idDatabaseSocket = Socket("localhost", ID_DATABASE_PORT)
            println("M-am conectat la id database")
        } catch (e: Exception) {
            println("Nu ma pot conecta la id database")
            exitProcess(1)
        }
    }

    private fun sendInitMessage() {
        messageManagerSocket.getOutputStream().write("Init:asistent\n".toByteArray())
    }

    private fun listenToDatabase() {
        val reader = BufferedReader(InputStreamReader(idDatabaseSocket.inputStream))

        while (true) {
            val result = reader.readLine()

            println("Mesaj primit de la supervisor: $result")

            if (result == null) {
                println("ID Database a fost inchis")
                idDatabaseSocket.close()
                break
            }

            coroutineScope.launch {
                processDatabaseResult(result)
            }
        }
    }

    private fun listenToMessageManager() {
        val reader = BufferedReader(InputStreamReader(messageManagerSocket.inputStream))

        while (true) {
            val request = reader.readLine()

            println("Mesaj primit de la message manager: $request")

            if (request == null) {
                println("Message manager a fost inchis")
                messageManagerSocket.close()
                break
            }

            coroutineScope.launch {
                processRequest(request)
            }
        }
    }

    private fun checkStudentID(studentName: String, givenStudentID: Int, realStudentID: Int): Boolean {
        if (givenStudentID == realStudentID) {
            println("$studentName a fost verificat")
            return true
        }

        else {
            println("$studentName nu a fost verificat cu succes")
            return false
        }
    }

    private suspend fun generateRandomID(): Int {
        while (true) {
            val random = Random.nextInt(0, 10000)
            studentIDMapMutex.withLock {
                if (!studentIDMap.values.contains(random)) {
                    return random
                }
            }
        }
    }

    private suspend fun processRequest(request: String) {
        try {
            //requestul e de forma Verificare:studentName:studentID
            val (messageType, studentName, studentID) = request.split(":")

            when {
                messageType.startsWith("Verificare") -> {
                    val existsCheck = studentIDMapMutex.withLock {
                        studentIDMap.containsKey(studentName)
                    }

                    if (existsCheck)
                    {
                        val realStudentID = studentIDMapMutex.withLock {
                            studentIDMap[studentName]
                        }
                        val check = checkStudentID(studentName, studentID.toInt(), realStudentID!!)

                        sendMessageToMessageManager("Verificare:$studentName:${if (check) "corect" else "gresit"}")
                    }

                    else {
                        val deferred = CompletableDeferred<Int>()
                        idRequests[studentName] = deferred

                        sendRequestToIdDatabase("get:$studentName")
                        coroutineScope.launch {
                            val realStudentID = withTimeoutOrNull(3000) {
                                deferred.await()
                            }

                            //println("A fost completat deferrul cu id: $realStudentID")

                            if (realStudentID == null) {
                                println("Nu am primit un rezultat de la baza de date in timp")
                            } else {
                                if (realStudentID == -1) {
                                    println("$studentName apare pentru prima oara")

                                    val id = generateRandomID()
                                    println("Id-ul generat pentru $studentName: $id")

                                    studentIDMapMutex.withLock {
                                        studentIDMap[studentName] = id
                                        //println("ID-ul studentului $studentName este $id")
                                    }

                                    sendRequestToIdDatabase("add:$studentName | $id")
                                    sendMessageToMessageManager("Inregistrare:$studentName:$id")
                                } else {
                                    val check = checkStudentID(studentName, studentID.toInt(), realStudentID)
                                    sendMessageToMessageManager("Verificare:$studentName:${if (check) "corect" else "gresit"}")

                                    studentIDMapMutex.withLock {
                                        studentIDMap[studentName] = realStudentID
                                    }

                                    //println("ID-ul corect al studentului $studentName este $realStudentID")
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Eroare la procesarea request-ului: $request\n$e")
        }
    }

    private suspend fun processDatabaseResult(result: String) {
        try {
            val (messageType, student, result) = result.split(":")

            when {
                messageType.startsWith("get") -> {
                    val id = result.toInt()

                    println("ID-ul procesat pentru $student: $id")

                    idRequestMutex.withLock {
                        idRequests[student]?.complete(id)
                        println("Am completat deferrul pentru $student cu id: $id")
                    }
                }
            }
        } catch (e: Exception) {
            println("Eroare la procesarea rezultatului: $result")
        }
    }

    private fun sendMessageToMessageManager(message: String) {
        try {
            //raspunsul e de forma Verificare:corect/gresit
            messageManagerSocket.getOutputStream().write("$message\n".toByteArray())
            println("Am trimis mesajul $message catre message manager")
        } catch (e: Exception) {
            println("Eroare la trimiterea mesajului catre message manager: $message")
        }
    }

    private fun sendRequestToIdDatabase(request: String) {
        try {
            idDatabaseSocket.getOutputStream().write("$request\n".toByteArray())
            println("Am trimis mesajul $request catre id database")
        } catch (e: Exception) {
            println("Eroare la trimiterea mesajului catre id database: $request")
        }
    }

    suspend fun run() {
        connectToIdDatabase()
        subscribeToMessageManager()

        val listenDatabaseJob = coroutineScope.launch {
            listenToDatabase()
        }

        val listenMessageManagerJob = coroutineScope.launch {
            listenToMessageManager()
        }

        listenDatabaseJob.join()
        listenMessageManagerJob.join()
    }
}

suspend fun main(args: Array<String>) {
    val assistant = AssistantMicroservice()
    assistant.run()
}

