import pika
from retry import retry


class RabbitMq:
    def __init__(self):
        self.config = {
            'host': 'localhost',
            'port': 5672,
            'username': 'student',
            'password': 'student',

            'exchange': 'beerapp.direct',

            'request_routing_key': 'beerapp.request',
            'response_routing_key': 'beerapp.response',

            'request_queue': 'beerapp.request.queue',
            'response_queue': 'beerapp.response.queue'
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

    def clear_response_queue(self, channel):
        channel.queue_purge(queue=self.config['response_queue'])

    def on_received_message(self, ch, method, properties, body):
        try:
            result = body.decode('utf-8')
            print("\nResponse from server:")
            print(result)
        except Exception:
            print("wrong data format")
        finally:
            ch.stop_consuming()

    @retry(pika.exceptions.AMQPConnectionError, delay=5, jitter=(1, 3))
    def receive_message(self):
        with pika.BlockingConnection(self.parameters) as connection:
            channel = connection.channel()

            channel.basic_consume(
                queue=self.config['response_queue'],
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
                routing_key=self.config['request_routing_key'],
                body=message
            )


def print_menu():
    print('\n0 --> Exit program')
    print('1 --> addBeer')
    print('2 --> getBeers')
    print('3 --> getBeerByName')
    print('4 --> getBeerByPrice')
    print('5 --> updateBeer')
    print('6 --> deleteBeer')
    return input("Option=")


if __name__ == '__main__':
    rabbit_mq = RabbitMq()

    rabbit_mq.send_message("createBeerTable~")
    rabbit_mq.receive_message()

    while True:
        option = print_menu()

        if option == '0':
            break

        elif option == '1':
            name = input("Beer name: ")
            price = float(input("Beer price: "))
            rabbit_mq.send_message(f"addBeer~id=-1;name={name};price={price}")
            rabbit_mq.receive_message()

        elif option == '2':
            rabbit_mq.send_message("getBeers~")
            rabbit_mq.receive_message()

        elif option == '3':
            name = input("Beer name: ")
            rabbit_mq.send_message(f"getBeerByName~name={name}")
            rabbit_mq.receive_message()

        elif option == '4':
            price = float(input("Beer price: "))
            rabbit_mq.send_message(f"getBeerByPrice~price={price}")
            rabbit_mq.receive_message()

        elif option == '5':
            beer_id = int(input("Beer ID: "))
            name = input("Beer name: ")
            price = float(input("Beer price: "))
            rabbit_mq.send_message(f"updateBeer~id={beer_id};name={name};price={price}")
            rabbit_mq.receive_message()

        elif option == '6':
            name = input("Beer name: ")
            rabbit_mq.send_message(f"deleteBeer~name={name}")
            rabbit_mq.receive_message()

        else:
            print("Invalid option")