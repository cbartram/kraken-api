package service

import (
	"encoding/json"
	"fmt"
	amqp "github.com/rabbitmq/amqp091-go"
	log "github.com/sirupsen/logrus"
	"gorm.io/gorm"
	"os"
	"time"
)

type Message struct {
	Type string `json:"type"`
	Body []byte `json:"content"`
}

type RabbitMqService struct {
	Connection     *amqp.Connection
	ConsumeChannel *amqp.Channel
	PublishChannel *amqp.Channel
	Queue          *amqp.Queue
	exchangeName   string
	routingName    string
}

func MakeRabbitMQService(exchangeName, routingKey string) (*RabbitMqService, error) {
	credentials := fmt.Sprintf("%s:%s", os.Getenv("RABBITMQ_DEFAULT_USER"), os.Getenv("RABBITMQ_DEFAULT_PASS"))
	conn, err := amqp.Dial(fmt.Sprintf("amqp://%s@%s/", credentials, os.Getenv("RABBITMQ_BASE_URL")))
	if err != nil {
		log.Errorf("failed to connect to RabbitMQ: %v", err)
		return nil, err
	}

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
		log.Errorf("failed to publish message: %v", err)
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

func (r *RabbitMqService) RegisterConsumer(consumer func(message Message, db *gorm.DB), delay time.Duration, db *gorm.DB) error {
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
		log.Errorf("error starting consumer: %v", err)
		r.Connection.Close()
		return err
	}

	go func() {
		for msg := range msgs {
			time.Sleep(delay)
			var message Message
			if err := json.Unmarshal(msg.Body, &message); err != nil {
				log.Errorf("error unmarshaling stripe websocket message: %v", err)
				continue
			}
			consumer(message, db)
		}
	}()

	return nil
}

func (r *RabbitMqService) Close() {
	r.PublishChannel.Close()
	r.ConsumeChannel.Close()
	r.Connection.Close()
}
