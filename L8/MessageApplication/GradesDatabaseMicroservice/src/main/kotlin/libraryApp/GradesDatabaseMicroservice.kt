package libraryApp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket

class GradesDatabaseMicroservice (
    private val repository: Repository
) {
    private val gradesDatabaseServer: ServerSocket = ServerSocket(DATABASE_PORT)
    private lateinit var teacherSocket: Socket

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object Constants {
        const val DATABASE_PORT = 2100
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
                    println("Toate notele")
                    repository.listAll()
                }
            }
        } catch (e: Exception) {
            println("Eroare la procesarea request-ului: $request")
        }
    }

    fun run() {
        //punem in loop, daca teacher se deconecteaza, apoi sa poata sa se conecteze inapoi fara restructurari agresive
        while (true) {
            println("Astept sa se conecteze teacher...")
            teacherSocket = gradesDatabaseServer.accept()

            println("Teacher s-a conectat")
            listenToRequests()
        }
    }
}

fun main(args: Array<String>) {
    val repo = Repository()
    val gradesDatabaseMicroservice = GradesDatabaseMicroservice(repo)
    gradesDatabaseMicroservice.run()
}

