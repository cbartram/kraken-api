package service

import (
	"encoding/json"
	amqp "github.com/rabbitmq/amqp091-go"
	"go.uber.org/zap"
	"gorm.io/gorm"
	"time"
)

type Message struct {
	Type string `json:"type"`
	Body []byte `json:"content"`
}

type Connectable interface {
	Channel() (*amqp.Channel, error)
	Close() error
}

type Channelable interface {
	ExchangeDeclare(name, kind string, durable, autoDelete, internal, noWait bool, args amqp.Table) error
	QueueDeclare(name string, durable, autoDelete, exclusive, noWait bool, args amqp.Table) (amqp.Queue, error)
	QueueBind(name, key, exchange string, noWait bool, args amqp.Table) error
	Publish(exchange, key string, mandatory, immediate bool, msg amqp.Publishing) error
	Consume(queue, consumer string, autoAck, exclusive, noLocal, noWait bool, args amqp.Table) (<-chan amqp.Delivery, error)
	Close() error
}

type RabbitMqService struct {
	log            *zap.SugaredLogger
	Connection     Connectable
	ConsumeChannel Channelable
	PublishChannel Channelable
	Queue          *amqp.Queue
	exchangeName   string
	routingName    string
}

func MakeRabbitMQService(exchangeName, routingKey string, conn Connectable, log *zap.SugaredLogger) (*RabbitMqService, error) {
	ch, err := conn.Channel()
	if err != nil {
		log.Errorf("failed to open consume channel: %v", err)
		return nil, err
	}

	pubChan, err := conn.Channel()
	if err != nil {
		log.Errorf("failed to open publish channel: %v", err)
		return nil, err
	}

	err = ch.ExchangeDeclare(
		exchangeName, // exchange name
		"direct",     // exchange type
		true,         // durable
		false,        // auto-deleted
		false,        // internal
		false,        // no-wait
		nil,          // arguments
	)
	if err != nil {
		log.Errorf("failed to declare exchange: %v", err)
		return nil, err
	}

	// Declare a unique queue for this object instance
	q, err := ch.QueueDeclare(
		"",
		false,
		true,
		true,
		false,
		nil,
	)

	if err != nil {
		log.Errorf("error declaring queue: %v", err)
		conn.Close()
		return nil, err
	}

	err = ch.QueueBind(
		q.Name,
		routingKey,
		exchangeName,
		false,
		nil,
	)

	if err != nil {
		log.Errorf("error binding queue: %v", err)
		conn.Close()
		return nil, err
	}

	return &RabbitMqService{
		log:            log,
		Connection:     conn,
		ConsumeChannel: ch,
		PublishChannel: pubChan,
		Queue:          &q,
		exchangeName:   exchangeName,
		routingName:    routingKey,
	}, nil
}

func (r *RabbitMqService) PublishMessage(message *Message) error {
	messageBytes, err := json.Marshal(message)
	if err != nil {
		r.log.Errorf("failed to publish message: %v", err)
		return err
	}

	return r.PublishChannel.Publish(
		r.exchangeName,
		r.routingName,
		false,
		false,
		amqp.Publishing{
			ContentType: "application/json",
			Body:        messageBytes,
		},
	)
}

func (r *RabbitMqService) RegisterConsumer(consumer func(message Message, db *gorm.DB, log *zap.SugaredLogger), delay time.Duration, db *gorm.DB) error {
	msgs, err := r.ConsumeChannel.Consume(
		r.Queue.Name,
		"",
		true,
		true,
		false,
		false,
		nil,
	)
	if err != nil {
		r.log.Errorf("error starting consumer: %v", err)
		r.Connection.Close()
		return err
	}

	go func() {
		for msg := range msgs {
			time.Sleep(delay)
			var message Message
			if err := json.Unmarshal(msg.Body, &message); err != nil {
				r.log.Errorf("error unmarshaling stripe websocket message: %v", err)
				continue
			}
			consumer(message, db, r.log)
		}
	}()

	return nil
}

func (r *RabbitMqService) Close() {
	r.PublishChannel.Close()
	r.ConsumeChannel.Close()
	r.Connection.Close()
}
