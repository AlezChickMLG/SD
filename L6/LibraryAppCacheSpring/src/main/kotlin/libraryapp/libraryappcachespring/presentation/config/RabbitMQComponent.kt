package libraryapp.libraryappcachespring.presentation.config

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQComponent {

    @Value("\${rabbitmq.exchange}")
    private lateinit var exchangeName: String

    @Value("\${rabbitmq.file.queue}")
    private lateinit var fileQueueName: String

    @Value("\${rabbitmq.commands.queue}")
    private lateinit var commandsQueueName: String

    @Value("\${rabbitmq.state.queue}")
    private lateinit var stateQueueName: String

    @Value("\${rabbitmq.file.routing-key}")
    private lateinit var fileRoutingKey: String

    @Value("\${rabbitmq.commands.routing-key}")
    private lateinit var commandsRoutingKey: String

    @Value("\${rabbitmq.state.routing-key}")
    private lateinit var stateRoutingKey: String

    @Bean
    fun exchange(): DirectExchange {
        return DirectExchange(exchangeName)
    }

    fun getExchange(): String = exchangeName
    fun getFileQueue(): String = fileQueueName
    fun getCommandsQueue(): String = commandsQueueName
    fun getStateQueue(): String = stateQueueName
    fun getFileRoutingKey(): String = fileRoutingKey
    fun getCommandsRoutingKey(): String = commandsRoutingKey
    fun getStateRoutingKey(): String = stateRoutingKey

    @Bean
    fun fileQueue(): Queue {
        return Queue(fileQueueName, true)
    }

    @Bean
    fun commandsQueue(): Queue {
        return Queue(commandsQueueName, true)
    }

    @Bean
    fun stateQueue(): Queue {
        return Queue(stateQueueName, true)
    }

    @Bean
    fun fileBinding(fileQueue: Queue, exchange: DirectExchange): Binding {
        return BindingBuilder
            .bind(fileQueue)
            .to(exchange)
            .with(fileRoutingKey)
    }

    @Bean
    fun commandsBinding(commandsQueue: Queue, exchange: DirectExchange): Binding {
        return BindingBuilder
            .bind(commandsQueue)
            .to(exchange)
            .with(commandsRoutingKey)
    }

    @Bean
    fun stateBinding(stateQueue: Queue, exchange: DirectExchange): Binding {
        return BindingBuilder
            .bind(stateQueue)
            .to(exchange)
            .with(stateRoutingKey)
    }
}