package beerApp.presentation.controllers

import beerApp.business.interfaces.IBeerService
import beerApp.models.Beer
import beerApp.presentation.config.RabbitMqComponent
import org.springframework.amqp.core.AmqpTemplate
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

@Component
class BeerDAOController(
    private val beerService: IBeerService,
    private val rabbitMqComponent: RabbitMqComponent,
    private val amqpTemplate: AmqpTemplate
) {

    @RabbitListener(queues = ["\${beerapp.rabbitmq.request.queue}"])
    fun receiveMessage(msg: String) {
        println("Received request: $msg")

        val parts = msg.split('~', limit = 2)
        if (parts.isEmpty()) {
            sendResponse("Invalid message format")
            return
        }

        val operation = parts[0]
        val parameters = if (parts.size > 1) parts[1] else ""

        try {
            val result = when (operation) {
                "createBeerTable" -> {
                    beerService.createBeerTable()
                    "Beer table created successfully."
                }

                "addBeer" -> {
                    val beer = parseBeer(parameters)
                    beerService.addBeer(beer)
                    "Beer added successfully."
                }

                "getBeers" -> {
                    val beers = beerService.getBeers()
                    beers.toString()
                }

                "getBeerByName" -> {
                    val name = parseName(parameters)
                    val beer = beerService.getBeerByName(name)
                    beer?.toString() ?: "Beer not found."
                }

                "getBeerByPrice" -> {
                    val price = parsePrice(parameters)
                    val beer = beerService.getBeerByPrice(price)
                    beer?.toString() ?: "Beer not found."
                }

                "updateBeer" -> {
                    val beer = parseBeer(parameters)
                    beerService.updateBeer(beer)
                    "Beer updated successfully."
                }

                "deleteBeer" -> {
                    val name = parseName(parameters)
                    beerService.deleteBeer(name)
                    "Beer deleted successfully."
                }

                else -> "Unknown operation: $operation"
            }

            sendResponse(result)
        } catch (e: Exception) {
            println("Error while processing message: ${e.message}")
            sendResponse("Error: ${e.message}")
        }
    }

    private fun sendResponse(message: String) {
        amqpTemplate.convertAndSend(
            rabbitMqComponent.getExchange(),
            rabbitMqComponent.getResponseRoutingKey(),
            message
        )
    }

    private fun parseBeer(parameters: String): Beer {
        val values = parameters
            .split(';')
            .associate {
                val pair = it.split('=', limit = 2)
                pair[0] to pair[1]
            }

        return Beer(
            values["id"]!!.toInt(),
            values["name"]!!,
            values["price"]!!.toFloat()
        )
    }

    private fun parseName(parameters: String): String {
        val pair = parameters.split('=', limit = 2)
        if (pair.size != 2 || pair[0] != "name") {
            throw IllegalArgumentException("Invalid name parameter: $parameters")
        }
        return pair[1]
    }

    private fun parsePrice(parameters: String): Float {
        val pair = parameters.split('=', limit = 2)
        if (pair.size != 2 || pair[0] != "price") {
            throw IllegalArgumentException("Invalid price parameter: $parameters")
        }
        return pair[1].toFloat()
    }
}