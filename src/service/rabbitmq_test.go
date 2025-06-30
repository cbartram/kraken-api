package service

import (
	"encoding/json"
	"errors"
	"os"
	"testing"
	"time"

	amqp "github.com/rabbitmq/amqp091-go"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"go.uber.org/zap"
	"go.uber.org/zap/zaptest"
	"gorm.io/gorm"
)

// Mock interfaces for AMQP components
type MockConnection struct {
	mock.Mock
}

func (m *MockConnection) Channel() (*amqp.Channel, error) {
	args := m.Called()
	return args.Get(0).(*amqp.Channel), args.Error(1)
}

func (m *MockConnection) Close() error {
	args := m.Called()
	return args.Error(0)
}

func (m *MockConnection) IsClosed() bool {
	args := m.Called()
	return args.Get(0).(bool)
}

func (m *MockConnection) NotifyClose(receiver chan *amqp.Error) chan *amqp.Error {
	args := m.Called(receiver)
	return args.Get(0).(chan *amqp.Error)
}

type MockChannel struct {
	mock.Mock
}

func (m *MockChannel) ExchangeDeclare(name, kind string, durable, autoDelete, internal, noWait bool, args amqp.Table) error {
	mockArgs := m.Called(name, kind, durable, autoDelete, internal, noWait, args)
	return mockArgs.Error(0)
}

func (m *MockChannel) QueueDeclare(name string, durable, autoDelete, exclusive, noWait bool, args amqp.Table) (amqp.Queue, error) {
	mockArgs := m.Called(name, durable, autoDelete, exclusive, noWait, args)
	return mockArgs.Get(0).(amqp.Queue), mockArgs.Error(1)
}

func (m *MockChannel) QueueBind(name, key, exchange string, noWait bool, args amqp.Table) error {
	mockArgs := m.Called(name, key, exchange, noWait, args)
	return mockArgs.Error(0)
}

func (m *MockChannel) Publish(exchange, key string, mandatory, immediate bool, msg amqp.Publishing) error {
	mockArgs := m.Called(exchange, key, mandatory, immediate, msg)
	return mockArgs.Error(0)
}

func (m *MockChannel) IsClosed() bool {
	mockArgs := m.Called()
	return mockArgs.Get(0).(bool)
}

func (m *MockChannel) NotifyClose(receiver chan *amqp.Error) chan *amqp.Error {
	mockArgs := m.Called(receiver)
	return mockArgs.Get(0).(chan *amqp.Error)
}

func (m *MockChannel) Qos(prefetchCount, prefetchSize int, global bool) error {
	mockArgs := m.Called(prefetchCount, prefetchSize, global)
	return mockArgs.Error(0)
}

func (m *MockChannel) Close() error {
	mockArgs := m.Called()
	return mockArgs.Error(0)
}

func (m *MockChannel) Consume(queue, consumer string, autoAck, exclusive, noLocal, noWait bool, args amqp.Table) (<-chan amqp.Delivery, error) {
	mockArgs := m.Called(queue, consumer, autoAck, exclusive, noLocal, noWait, args)
	return mockArgs.Get(0).(<-chan amqp.Delivery), mockArgs.Error(1)
}

// Test helper functions
func setupTestEnv() {
	os.Setenv("RABBITMQ_DEFAULT_USER", "testuser")
	os.Setenv("RABBITMQ_DEFAULT_PASS", "testpass")
	os.Setenv("RABBITMQ_BASE_URL", "localhost:5672")
}

func cleanupTestEnv() {
	os.Unsetenv("RABBITMQ_DEFAULT_USER")
	os.Unsetenv("RABBITMQ_DEFAULT_PASS")
	os.Unsetenv("RABBITMQ_BASE_URL")
}

func TestMessage_JSONMarshaling(t *testing.T) {
	tests := []struct {
		name     string
		message  Message
		expected string
	}{
		{
			name: "basic message",
			message: Message{
				Type: "test",
				Body: []byte("hello world"),
			},
			expected: `{"type":"test","content":"aGVsbG8gd29ybGQ="}`,
		},
		{
			name: "empty message",
			message: Message{
				Type: "",
				Body: []byte{},
			},
			expected: `{"type":"","content":""}`,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			data, err := json.Marshal(tt.message)
			assert.NoError(t, err)
			assert.JSONEq(t, tt.expected, string(data))

			// Test unmarshaling
			var unmarshaled Message
			err = json.Unmarshal(data, &unmarshaled)
			assert.NoError(t, err)
			assert.Equal(t, tt.message.Type, unmarshaled.Type)
			assert.Equal(t, tt.message.Body, unmarshaled.Body)
		})
	}
}

func TestRabbitMqService_PublishMessage(t *testing.T) {
	logger := zaptest.NewLogger(t).Sugar()

	tests := []struct {
		name          string
		message       *Message
		publishError  error
		expectedError bool
	}{
		{
			name: "successful publish",
			message: &Message{
				Type: "test",
				Body: []byte("test message"),
			},
			publishError:  nil,
			expectedError: false,
		},
		{
			name: "publish error",
			message: &Message{
				Type: "test",
				Body: []byte("test message"),
			},
			publishError:  errors.New("publish failed"),
			expectedError: true,
		},
		{
			name: "empty message",
			message: &Message{
				Type: "",
				Body: []byte{},
			},
			publishError:  nil,
			expectedError: false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			mockChannel := &MockChannel{}

			service := &RabbitMqService{
				log:            logger,
				PublishChannel: (Channelable)(mockChannel),
				exchangeName:   "test-exchange",
				routingName:    "test-routing",
			}

			expectedBytes, _ := json.Marshal(tt.message)
			expectedPublishing := amqp.Publishing{
				ContentType: "application/json",
				Body:        expectedBytes,
			}

			mockChannel.On("Publish", "test-exchange", "test-routing", false, false, expectedPublishing).Return(tt.publishError)

			err := service.PublishMessage(tt.message)

			if tt.expectedError {
				assert.Error(t, err)
			} else {
				assert.NoError(t, err)
			}

			mockChannel.AssertExpectations(t)
		})
	}
}

func TestRabbitMqService_RegisterConsumer(t *testing.T) {
	logger := zaptest.NewLogger(t).Sugar()
	mockDB := &gorm.DB{}

	tests := []struct {
		name          string
		consumeError  error
		expectedError bool
		setupMock     func(*MockChannel, *MockConnection)
	}{
		{
			name:          "successful consumer registration",
			consumeError:  nil,
			expectedError: false,
			setupMock: func(mockChannel *MockChannel, mockConn *MockConnection) {
				deliveryChan := make(chan amqp.Delivery, 1)

				// Send a test message and close the channel
				go func() {
					testMessage := Message{Type: "test", Body: []byte("test")}
					messageBytes, _ := json.Marshal(testMessage)

					deliveryChan <- amqp.Delivery{
						Body: messageBytes,
					}
					close(deliveryChan)
				}()

				mockChannel.On("Consume", "test-queue", "", true, true, false, false, mock.Anything).Return((<-chan amqp.Delivery)(deliveryChan), nil)
			},
		},
		{
			name:          "consume error",
			consumeError:  errors.New("consume failed"),
			expectedError: true,
			setupMock: func(mockChannel *MockChannel, mockConn *MockConnection) {
				mockChannel.On("Consume", "test-queue", "", true, true, false, false, mock.Anything).Return((<-chan amqp.Delivery)(nil), errors.New("consume failed"))
				mockConn.On("Close").Return(nil)
			},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			mockChannel := &MockChannel{}
			mockConn := &MockConnection{}

			tt.setupMock(mockChannel, mockConn)

			queue := &amqp.Queue{Name: "test-queue"}
			service := &RabbitMqService{
				log:            logger,
				Connection:     (Connectable)(mockConn),
				ConsumeChannel: (Channelable)(mockChannel),
				Queue:          queue,
			}

			consumerCalled := false
			consumer := func(message Message, db *gorm.DB, log *zap.SugaredLogger) {
				consumerCalled = true
				assert.Equal(t, "test", message.Type)
				assert.Equal(t, []byte("test"), message.Body)
				assert.Equal(t, mockDB, db)
				assert.Equal(t, logger, log)
			}

			err := service.RegisterConsumer(consumer, 0, mockDB)

			if tt.expectedError {
				assert.Error(t, err)
			} else {
				assert.NoError(t, err)
				// Give the goroutine time to process the message
				time.Sleep(10 * time.Millisecond)
				assert.True(t, consumerCalled)
			}

			mockChannel.AssertExpectations(t)
			mockConn.AssertExpectations(t)
		})
	}
}

func TestRabbitMqService_RegisterConsumer_InvalidJSON(t *testing.T) {
	logger := zaptest.NewLogger(t).Sugar()
	mockDB := &gorm.DB{}
	mockChannel := &MockChannel{}

	deliveryChan := make(chan amqp.Delivery, 1)

	// Send invalid JSON and close the channel
	go func() {
		deliveryChan <- amqp.Delivery{
			Body: []byte("invalid json"),
		}
		close(deliveryChan)
	}()

	mockChannel.On("Consume", "test-queue", "", true, true, false, false, mock.Anything).Return((<-chan amqp.Delivery)(deliveryChan), nil)

	queue := &amqp.Queue{Name: "test-queue"}
	service := &RabbitMqService{
		log:            logger,
		ConsumeChannel: (Channelable)(mockChannel),
		Queue:          queue,
	}

	consumerCalled := false
	consumer := func(message Message, db *gorm.DB, log *zap.SugaredLogger) {
		consumerCalled = true
	}

	err := service.RegisterConsumer(consumer, 0, mockDB)
	assert.NoError(t, err)

	// Give the goroutine time to process the invalid message
	time.Sleep(10 * time.Millisecond)

	// Consumer should not be called due to JSON unmarshal error
	assert.False(t, consumerCalled)
	mockChannel.AssertExpectations(t)
}

func TestRabbitMqService_RegisterConsumer_WithDelay(t *testing.T) {
	logger := zaptest.NewLogger(t).Sugar()
	mockDB := &gorm.DB{}
	mockChannel := &MockChannel{}

	deliveryChan := make(chan amqp.Delivery, 1)

	go func() {
		testMessage := Message{Type: "test", Body: []byte("test")}
		messageBytes, _ := json.Marshal(testMessage)

		deliveryChan <- amqp.Delivery{
			Body: messageBytes,
		}
		close(deliveryChan)
	}()

	mockChannel.On("Consume", "test-queue", "", true, true, false, false, mock.Anything).Return((<-chan amqp.Delivery)(deliveryChan), nil)

	queue := &amqp.Queue{Name: "test-queue"}
	service := &RabbitMqService{
		log:            logger,
		ConsumeChannel: (Channelable)(mockChannel),
		Queue:          queue,
	}

	consumerCalled := false
	startTime := time.Now()
	consumer := func(message Message, db *gorm.DB, log *zap.SugaredLogger) {
		consumerCalled = true
		elapsed := time.Since(startTime)
		// Should have waited at least the delay time
		assert.True(t, elapsed >= 50*time.Millisecond)
	}

	err := service.RegisterConsumer(consumer, 50*time.Millisecond, mockDB)
	assert.NoError(t, err)

	// Give enough time for delay + processing
	time.Sleep(100 * time.Millisecond)
	assert.True(t, consumerCalled)
	mockChannel.AssertExpectations(t)
}

func TestRabbitMqService_Close(t *testing.T) {
	logger := zaptest.NewLogger(t).Sugar()
	mockPublishChannel := &MockChannel{}
	mockConsumeChannel := &MockChannel{}
	mockConn := &MockConnection{}

	mockPublishChannel.On("Close").Return(nil)
	mockConsumeChannel.On("Close").Return(nil)
	mockConn.On("Close").Return(nil)

	service := &RabbitMqService{
		log:            logger,
		Connection:     (Connectable)(mockConn),
		ConsumeChannel: (Channelable)(mockConsumeChannel),
		PublishChannel: (Channelable)(mockPublishChannel),
	}

	// Should not panic and should call all Close methods
	service.Close()

	mockPublishChannel.AssertExpectations(t)
	mockConsumeChannel.AssertExpectations(t)
	mockConn.AssertExpectations(t)
}

// Benchmarks
func BenchmarkMessage_Marshal(b *testing.B) {
	message := Message{
		Type: "benchmark",
		Body: []byte("this is a benchmark test message with some content"),
	}

	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		_, err := json.Marshal(message)
		if err != nil {
			b.Fatal(err)
		}
	}
}

func BenchmarkMessage_Unmarshal(b *testing.B) {
	message := Message{
		Type: "benchmark",
		Body: []byte("this is a benchmark test message with some content"),
	}
	data, _ := json.Marshal(message)

	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		var unmarshaled Message
		err := json.Unmarshal(data, &unmarshaled)
		if err != nil {
			b.Fatal(err)
		}
	}
}

// Example usage tests
func ExampleRabbitMqService_PublishMessage() {
	// This would be a real integration test in practice
	logger := zap.NewNop().Sugar()

	// Mock service for example
	service := &RabbitMqService{
		log: logger,
	}

	message := &Message{
		Type: "user.created",
		Body: []byte(`{"user_id": 123, "email": "user@example.com"}`),
	}

	// In real usage:
	// err := service.PublishMessage(message)
	// if err != nil {
	//     log.Fatal(err)
	// }

	_ = service
	_ = message
	// Output:
}

func ExampleRabbitMqService_RegisterConsumer() {
	logger := zap.NewNop().Sugar()

	// Mock service for example
	service := &RabbitMqService{
		log: logger,
	}

	consumer := func(message Message, db *gorm.DB, log *zap.SugaredLogger) {
		log.Infof("Processing message of type: %s", message.Type)
		// Process the message here
	}

	// In real usage:
	// err := service.RegisterConsumer(consumer, 100*time.Millisecond, db)
	// if err != nil {
	//     log.Fatal(err)
	// }

	_ = service
	_ = consumer
	// Output:
}
