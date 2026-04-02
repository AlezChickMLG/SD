import pika
from retry import retry

class RabbitMQ:
    def __init__(self):
        self.config = {
            'host': 'localhost',
            'port': 5672,
            'username': 'student',
            'password': 'student',

            'exchange': 'direct',

            'file_queue': 'file.queue',
            'commands_queue': 'commands.queue',
            'state_queue': 'state.queue',

            'file_routing_key': 'file',
            'commands_routing_key': 'commands',
            'state_routing_key': 'state'
        }

        self.credentials = pika.PlainCredentials(
            self.config['username'],
            self.config['password']
        )

        self.parameters = pika.ConnectionParameters(
            host=self.config['host'],
            port=self.config['port'],
            credentials=self.credentials
        )

        self.result = None

    def on_received_message(self, ch, method, properties, body):
        try:
            result = body.decode('utf-8')
            print("\nResponse from server:")
            self.result = result
        except Exception:
            print("wrong data format")
        finally:
            ch.stop_consuming()

    @retry(pika.exceptions.AMQPConnectionError, delay=5, jitter=(1, 3))
    def receive_message(self):
        with pika.BlockingConnection(self.parameters) as connection:
            channel = connection.channel()

            channel.basic_consume(
                queue=self.config['file_queue'],
                on_message_callback=self.on_received_message,
                auto_ack=True
            )

            try:
                channel.start_consuming()
            except pika.exceptions.ConnectionClosedByBroker:
                print("Connection closed by broker.")
            except pika.exceptions.AMQPChannelError:
                print("AMQP Channel Error")
            except KeyboardInterrupt:
                print("Application closed.")

    def send_message(self, message):
        with pika.BlockingConnection(self.parameters) as connection:
            channel = connection.channel()

            channel.basic_publish(
                exchange=self.config['exchange'],
                routing_key=self.config['commands_routing_key'],
                body=message
            )