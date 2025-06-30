package service

import (
	"encoding/json"
	amqp "github.com/rabbitmq/amqp091-go"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"go.uber.org/zap/zaptest"
	"os"
	"testing"
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
