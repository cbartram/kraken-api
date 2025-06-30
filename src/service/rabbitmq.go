package service

import (
	"encoding/json"
	"fmt"
	amqp "github.com/rabbitmq/amqp091-go"
	"go.uber.org/zap"
	"gorm.io/gorm"
	"sync"
	"time"
)

type Message struct {
	Type string `json:"type"`
	Body []byte `json:"content"`
}

type Connectable interface {
	Channel() (*amqp.Channel, error)
	Close() error
	IsClosed() bool
	NotifyClose(receiver chan *amqp.Error) chan *amqp.Error
}

type Channelable interface {
	ExchangeDeclare(name, kind string, durable, autoDelete, internal, noWait bool, args amqp.Table) error
	QueueDeclare(name string, durable, autoDelete, exclusive, noWait bool, args amqp.Table) (amqp.Queue, error)
	QueueBind(name, key, exchange string, noWait bool, args amqp.Table) error
	Publish(exchange, key string, mandatory, immediate bool, msg amqp.Publishing) error
	Consume(queue, consumer string, autoAck, exclusive, noLocal, noWait bool, args amqp.Table) (<-chan amqp.Delivery, error)
	Close() error
	IsClosed() bool
	NotifyClose(receiver chan *amqp.Error) chan *amqp.Error
	Qos(prefetchCount, prefetchSize int, global bool) error
}

type RabbitMqService struct {
	log            *zap.SugaredLogger
	connectionURL  string
	Connection     Connectable
	ConsumeChannel Channelable
	PublishChannel Channelable
	Queue          *amqp.Queue
	exchangeName   string
	routingName    string
	mutex          sync.RWMutex
	reconnecting   bool
}

func MakeRabbitMQService(connectionURL, exchangeName, routingKey string, log *zap.SugaredLogger) (*RabbitMqService, error) {
	service := &RabbitMqService{
		log:           log,
		connectionURL: connectionURL,
		exchangeName:  exchangeName,
		routingName:   routingKey,
	}

	err := service.connect()
	if err != nil {
		return nil, err
	}

	go service.monitorConnection()

	return service, nil
}

func (r *RabbitMqService) connect() error {
	r.mutex.Lock()
	defer r.mutex.Unlock()

	// Configure connection with heartbeat
	config := amqp.Config{
		Heartbeat: 10 * time.Second,
		Locale:    "en_US",
	}

	conn, err := amqp.DialConfig(r.connectionURL, config)
	if err != nil {
		r.log.Errorf("failed to connect to RabbitMQ: %v", err)
		return err
	}

	r.Connection = conn

	// Create channels
	consumeCh, err := conn.Channel()
	if err != nil {
		r.log.Errorf("failed to open consume channel: %v", err)
		conn.Close()
		return err
	}

	publishCh, err := conn.Channel()
	if err != nil {
		r.log.Errorf("failed to open publish channel: %v", err)
		consumeCh.Close()
		conn.Close()
		return err
	}

	r.ConsumeChannel = consumeCh
	r.PublishChannel = publishCh

	err = r.setupTopology()
	if err != nil {
		r.closeChannels()
		conn.Close()
		return err
	}

	r.log.Info("RabbitMQ connection established successfully")
	return nil
}

func (r *RabbitMqService) setupTopology() error {
	err := r.ConsumeChannel.ExchangeDeclare(
		r.exchangeName, // exchange name
		"direct",       // exchange type
		true,           // durable
		false,          // auto-deleted
		false,          // internal
		false,          // no-wait
		nil,            // arguments
	)
	if err != nil {
		r.log.Errorf("failed to declare exchange: %v", err)
		return err
	}

	q, err := r.ConsumeChannel.QueueDeclare(
		"",    // name (empty for auto-generated)
		false, // durable
		true,  // delete when unused
		true,  // exclusive
		false, // no-wait
		nil,   // arguments
	)
	if err != nil {
		r.log.Errorf("error declaring queue: %v", err)
		return err
	}

	// Bind queue
	err = r.ConsumeChannel.QueueBind(
		q.Name,
		r.routingName,
		r.exchangeName,
		false,
		nil,
	)
	if err != nil {
		r.log.Errorf("error binding queue: %v", err)
		return err
	}

	r.Queue = &q
	return nil
}

func (r *RabbitMqService) monitorConnection() {
	for {
		r.mutex.RLock()
		conn := r.Connection
		r.mutex.RUnlock()

		if conn == nil {
			time.Sleep(5 * time.Second)
			continue
		}

		// Listen for connection close events
		closeNotify := make(chan *amqp.Error, 1)
		conn.NotifyClose(closeNotify)

		// Wait for connection to close
		err := <-closeNotify
		if err != nil {
			r.log.Errorf("RabbitMQ connection closed unexpectedly, attempting to re-connect: %v", err)
			r.handleReconnection()
		}
	}
}

func (r *RabbitMqService) handleReconnection() {
	r.mutex.Lock()
	if r.reconnecting {
		r.mutex.Unlock()
		return
	}
	r.reconnecting = true
	r.mutex.Unlock()

	defer func() {
		r.mutex.Lock()
		r.reconnecting = false
		r.mutex.Unlock()
	}()

	// Clean up old connections
	r.closeChannels()
	if r.Connection != nil {
		r.Connection.Close()
	}

	// Attempt to reconnect with exponential backoff
	backoff := time.Second
	maxBackoff := 30 * time.Second

	for {
		r.log.Info("Attempting to reconnect to RabbitMQ...")

		err := r.connect()
		if err == nil {
			r.log.Info("Successfully reconnected to RabbitMQ")
			return
		}

		r.log.Errorf("Failed to reconnect: %v. Retrying in %v", err, backoff)
		time.Sleep(backoff)

		// Exponential backoff with jitter
		backoff = backoff * 2
		if backoff > maxBackoff {
			backoff = maxBackoff
		}
	}
}

func (r *RabbitMqService) closeChannels() {
	if r.PublishChannel != nil {
		r.PublishChannel.Close()
	}
	if r.ConsumeChannel != nil {
		r.ConsumeChannel.Close()
	}
}

func (r *RabbitMqService) PublishMessage(message *Message) error {
	r.mutex.RLock()
	defer r.mutex.RUnlock()

	if r.PublishChannel == nil || r.PublishChannel.IsClosed() {
		return fmt.Errorf("publish channel is not available")
	}

	messageBytes, err := json.Marshal(message)
	if err != nil {
		r.log.Errorf("failed to marshal message: %v", err)
		return err
	}

	// Retry logic for publishing
	maxRetries := 3
	for attempt := 1; attempt <= maxRetries; attempt++ {
		err = r.PublishChannel.Publish(
			r.exchangeName,
			r.routingName,
			false,
			false,
			amqp.Publishing{
				ContentType:  "application/json",
				Body:         messageBytes,
				DeliveryMode: amqp.Persistent,
				Timestamp:    time.Now(),
			},
		)

		if err == nil {
			return nil
		}

		r.log.Errorf("Failed to publish message (attempt %d/%d): %v", attempt, maxRetries, err)

		if attempt < maxRetries {
			time.Sleep(time.Duration(attempt) * time.Second)
		}
	}

	return fmt.Errorf("failed to publish message after %d attempts: %v", maxRetries, err)
}

func (r *RabbitMqService) RegisterConsumer(consumer func(message Message, db *gorm.DB, log *zap.SugaredLogger), delay time.Duration, db *gorm.DB) error {
	r.mutex.RLock()
	defer r.mutex.RUnlock()

	if r.ConsumeChannel == nil || r.ConsumeChannel.IsClosed() {
		return fmt.Errorf("consume channel is not available")
	}

	err := r.ConsumeChannel.Qos(
		10,
		0,
		false,
	)
	if err != nil {
		r.log.Errorf("failed to set QoS: %v", err)
		return err
	}

	msgs, err := r.ConsumeChannel.Consume(
		r.Queue.Name,
		"",    // consumer name
		false, // auto-ack (changed to false for manual ack)
		true,  // exclusive
		false, // no-local
		false, // no-wait
		nil,   // args
	)
	if err != nil {
		r.log.Errorf("error starting consumer: %v", err)
		return err
	}

	go func() {
		for msg := range msgs {
			if delay > 0 {
				time.Sleep(delay)
			}

			var message Message
			if err := json.Unmarshal(msg.Body, &message); err != nil {
				r.log.Errorf("error unmarshaling message: %v", err)
				msg.Nack(false, false) // Reject message
				continue
			}

			// Process message
			func() {
				defer func() {
					if rec := recover(); rec != nil {
						r.log.Errorf("panic while processing message: %v", rec)
						msg.Nack(false, false) // Reject on panic
					}
				}()

				consumer(message, db, r.log)
				msg.Ack(false)
			}()
		}
	}()

	return nil
}

func (r *RabbitMqService) IsHealthy() bool {
	r.mutex.RLock()
	defer r.mutex.RUnlock()

	return r.Connection != nil && !r.Connection.IsClosed() &&
		r.PublishChannel != nil && !r.PublishChannel.IsClosed() &&
		r.ConsumeChannel != nil && !r.ConsumeChannel.IsClosed()
}

func (r *RabbitMqService) Close() {
	r.mutex.Lock()
	defer r.mutex.Unlock()

	r.closeChannels()
	if r.Connection != nil {
		r.Connection.Close()
	}
}
