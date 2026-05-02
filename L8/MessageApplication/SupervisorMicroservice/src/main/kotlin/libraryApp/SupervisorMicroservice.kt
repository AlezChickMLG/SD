package libraryApp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket

class SupervisorMicroservice (
    private val repository: StudentIDRepository
) {
    private val idDatabaseServer: ServerSocket = ServerSocket(DATABASE_PORT)
    private lateinit var assistantSocket: Socket

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object Constants {
        const val DATABASE_PORT = 2200
    }

    private fun listenToRequests() {
        val reader = BufferedReader(InputStreamReader(assistantSocket.inputStream))

        while (true) {
            try {
                val request = reader.readLine()

                if (request == null) {
                    println("Assistant a fost inchis")
                    assistantSocket.close()
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
                        val (studentName, studentID) = stringModel.split(" | ")
                        val iDModel = IDModel(studentName, studentID.toInt())

                        repository.add(iDModel)
                    }

                    else {
                        throw Exception()
                    }
                }

                requestType == "listAll" -> {
                    repository.listAll()
                }

                requestType == "get" -> {
                    if (stringModel != "None") {
                        val studentID = repository.getID(stringModel)
                        sendAnswer("get:$stringModel:$studentID")
                    }

                    else {
                        throw Exception()
                    }
                }

                requestType == "getAll" -> {}
            }
        } catch (e: Exception) {
            println("Eroare la procesarea request-ului: $request :$e")
        }
    }

    private fun sendAnswer(answer: String) {
        try {
            println("Trimit rezultatul: $answer")
            assistantSocket.getOutputStream().write("$answer\n".toByteArray())
        } catch (e: Exception) {
            println("Eroare la trimiterea rezultatului")
        }
    }

    fun run() {
        //punem in loop, daca teacher se deconecteaza, apoi sa poata sa se conecteze inapoi fara restructurari agresive
        while (true) {
            println("Astept sa se conecteze assistant...")
            assistantSocket = idDatabaseServer.accept()

            println("Assistant s-a conectat")
            listenToRequests()
        }
    }
}

fun main(args: Array<String>) {
    val repo = StudentIDRepository()
    val idDatabaseMicroservice = SupervisorMicroservice(repo)
    idDatabaseMicroservice.run()
}

