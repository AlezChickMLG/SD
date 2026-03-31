package beerApp.presentation.config

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMqComponent {

    @Value("\${beerapp.rabbitmq.exchange}")
    private lateinit var exchangeName: String

    @Value("\${beerapp.rabbitmq.request.queue}")
    private lateinit var requestQueueName: String

    @Value("\${beerapp.rabbitmq.request.routing-key}")
    private lateinit var requestRoutingKey: String

    @Value("\${beerapp.rabbitmq.response.queue}")
    private lateinit var responseQueueName: String

    @Value("\${beerapp.rabbitmq.response.routing-key}")
    private lateinit var responseRoutingKey: String

    fun getExchange(): String = exchangeName
    fun getRequestQueue(): String = requestQueueName
    fun getRequestRoutingKey(): String = requestRoutingKey
    fun getResponseQueue(): String = responseQueueName
    fun getResponseRoutingKey(): String = responseRoutingKey

    @Bean
    fun exchange(): DirectExchange {
        return DirectExchange(exchangeName)
    }

    @Bean
    fun requestQueue(): Queue {
        return Queue(requestQueueName, true)
    }

    @Bean
    fun responseQueue(): Queue {
        return Queue(responseQueueName, true)
    }

    @Bean
    fun requestBinding(
        requestQueue: Queue,
        exchange: DirectExchange
    ): Binding {
        return BindingBuilder
            .bind(requestQueue)
            .to(exchange)
            .with(requestRoutingKey)
    }

    @Bean
    fun responseBinding(
        responseQueue: Queue,
        exchange: DirectExchange
    ): Binding {
        return BindingBuilder
            .bind(responseQueue)
            .to(exchange)
            .with(responseRoutingKey)
    }
}