package libraryapp.libraryappcachespring.presentation.controllers

import libraryapp.libraryappcachespring.business.services.CacheQueryService
import libraryapp.libraryappcachespring.presentation.config.RabbitMQComponent
import org.springframework.amqp.core.AmqpTemplate
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Controller

@Controller
class RabbitMQController (
    private val rabbitMQComponent: RabbitMQComponent,
    private val amqpTemplate: AmqpTemplate,
    private val cacheQueryService: CacheQueryService
) {

    @RabbitListener(queues = ["\${rabbitmq.commands.queue}"])
    fun receiveCommands(command: String) {
        println("Command received: $command")

        val serializedCommand = command.split("~")
        if (serializedCommand.count() != 2) {
            println("Eroare de formatare comanda")
            return
        }

        val actualCommand = serializedCommand.first()

        val result = if (actualCommand == "getAllBooks") {
            cacheQueryService.getAllBooks(serializedCommand.last())
        }

            else if (actualCommand == "findBook") {
                val filters = serializedCommand.last().split("=")
                if (filters.count() != 2) {
                    println("Eroare de formatare la argumentele findBook")
                    return
                }

                else  {
                    when(filters.first()) {
                        "author" -> cacheQueryService.findBook(author = filters.last())
                        "title" -> cacheQueryService.findBook(title = filters.last())
                        "publisher" -> cacheQueryService.findBook(publisher = filters.last())
                        else -> null
                    }
                }
            }

        else {
            "Not implemented"
        }

        try {
            println("result: $result")
            if (result != null) {
                sendFile(result)
                println("file sent")
            }
        } catch (e: Exception) {
            println("Error while processing command: $e")
        }
    }

    private fun sendFile(file: String) {
        amqpTemplate.convertAndSend(
            rabbitMQComponent.getExchange(),
            rabbitMQComponent.getFileRoutingKey(),
            file
        )
    }
}