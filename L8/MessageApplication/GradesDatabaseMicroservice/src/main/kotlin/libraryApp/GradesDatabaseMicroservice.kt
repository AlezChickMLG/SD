package libraryApp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import kotlin.system.exitProcess

class GradesDatabaseMicroservice (
    private val repository: Repository
) {
    private val gradesDatabaseServer: ServerSocket = ServerSocket(DATABASE_PORT)
    private lateinit var teacherSocket: Socket
    private lateinit var heartbeatSocket: Socket

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object Constants {
        const val DATABASE_PORT = 2100
        const val HEARTBEAT_PORT = 1900
        const val HEARTBEAT_HOST = "localhost"
    }

    private fun connectToHeartbeat() {
        heartbeatSocket = Socket(HEARTBEAT_HOST, HEARTBEAT_PORT)
    }

    private fun sendInitMessageToHeartbeat() {
        heartbeatSocket.getOutputStream().write("Init:gradesDatabase\n".toByteArray())
    }

    private fun listenToHeartbeat() {
        val reader = BufferedReader(InputStreamReader(heartbeatSocket.inputStream))

        coroutineScope.launch {
            while (true) {
                try {
                    val ping = reader.readLine() ?: withContext(Dispatchers.IO) {
                        heartbeatSocket.close()
                        reader.close()
                        exitProcess(1)
                    }

                    val (messageType, microservice) = ping.split(":")
                    if (messageType == "Ping") {
                        heartbeatSocket.getOutputStream().write("Pong:gradesDatabase\n".toByteArray())
                        //println("Am raspuns heartbeatului")
                    }

                } catch (e: java.lang.Exception) {
                    println("Eroare la procesarea pingului")
                    withContext(Dispatchers.IO) {
                        heartbeatSocket.close()
                        reader.close()
                        exitProcess(1)
                    }
                }
            }
        }
    }

    private fun listenToRequests() {
        val reader = BufferedReader(InputStreamReader(teacherSocket.inputStream))

        while (true) {
            try {
                val request = reader.readLine()

                if (request == null) {
                    println("Teacher a fost inchis")
                    teacherSocket.close()
                    reader.close()
                    break
                }

                println("Request: $request")

                coroutineScope.launch {
                    processRequest(request)
                }
            } catch (e: Exception) {
                println("Eroare la citirea request-ului")
                break
            }
        }
    }

    private fun processRequest(request: String) {
        try {
            val (requestType, stringModel) = request.split(":")

            when {
                requestType == "add" -> {
                    if (stringModel != "None") {
                        val (studentName, question, grade) = stringModel.split(" | ")
                        val model = Model(studentName, question, grade.toInt())

                        repository.add(model)
                    }
                }

                requestType == "listAll" -> {
                    repository.listAll()
                }

                requestType == "getAll" -> {
                    val studentName = stringModel

                    //obtin notele
                    val gradesMap = repository.getAllGrades(studentName)

                    if (gradesMap == null) {
                        println("Studentul $studentName nu are note inregistrate")
                        sendAnswer("getAll:$studentName:Nu are note")
                        return
                    }

                    //obtin media
                    val mean = calculateMean(gradesMap)

                    //trimit rezultatul catre teacher
                    sendAnswer("getAll:$studentName:$mean")
                }
            }
        } catch (e: Exception) {
            println("Eroare la procesarea request-ului: $request")
        }
    }

    private fun calculateMean(gradesMap: HashMap<String, Int>): Float = (gradesMap.values.sum() / gradesMap.size.toFloat())

    private fun sendAnswer(answer: String) {
        try {
            println("Trimit rezultatul: $answer")
            teacherSocket.getOutputStream().write("$answer\n".toByteArray())
        } catch (e: Exception) {
            println("Eroare la trimiterea rezultatului")
        }
    }

    suspend fun run() {
        try {
            connectToHeartbeat()
            sendInitMessageToHeartbeat()
            listenToHeartbeat()
        } catch (e: Exception) {
            println("Nu pot folosi heartbeat")
        }

        //punem in loop, daca teacher se deconecteaza, apoi sa poata sa se conecteze inapoi fara restructurari agresive
        val runCoroutine = coroutineScope.launch {
            while (true) {
                println("Astept sa se conecteze teacher...")
                teacherSocket = gradesDatabaseServer.accept()

                println("Teacher s-a conectat")
                listenToRequests()
            }
        }

        runCoroutine.join()
    }
}

suspend fun main(args: Array<String>) {
    val repo = Repository()
    val gradesDatabaseMicroservice = GradesDatabaseMicroservice(repo)
    gradesDatabaseMicroservice.run()
}

