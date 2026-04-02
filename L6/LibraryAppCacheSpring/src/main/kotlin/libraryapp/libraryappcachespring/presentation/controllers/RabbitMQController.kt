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
        val format = serializedCommand.last()

        try {
            val result = when (actualCommand) {
                "getAllBooks" -> cacheQueryService.getAllBooks(format)
                else -> null
            }

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