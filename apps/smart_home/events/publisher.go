package events

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"time"

	"github.com/rabbitmq/amqp091-go"
)

// Publisher handles publishing events to RabbitMQ
type Publisher struct {
	conn    *amqp091.Connection
	channel *amqp091.Channel
}

// SensorEvent represents a sensor-related event
type SensorEvent struct {
	EventID   string    `json:"event_id"`
	EventType string    `json:"event_type"`
	SensorID  int       `json:"sensor_id"`
	Name      string    `json:"name"`
	Type      string    `json:"type"`
	Location  string    `json:"location"`
	Value     float64   `json:"value,omitempty"`
	Status    string    `json:"status,omitempty"`
	Timestamp time.Time `json:"timestamp"`
}

// NewPublisher creates a new RabbitMQ publisher
func NewPublisher(rabbitMQURL string) (*Publisher, error) {
	conn, err := amqp091.Dial(rabbitMQURL)
	if err != nil {
		return nil, fmt.Errorf("failed to connect to RabbitMQ: %w", err)
	}

	channel, err := conn.Channel()
	if err != nil {
		conn.Close()
		return nil, fmt.Errorf("failed to open channel: %w", err)
	}

	publisher := &Publisher{
		conn:    conn,
		channel: channel,
	}

	// Declare exchanges
	if err := publisher.declareExchanges(); err != nil {
		publisher.Close()
		return nil, fmt.Errorf("failed to declare exchanges: %w", err)
	}

	log.Println("RabbitMQ publisher initialized successfully")
	return publisher, nil
}

// declareExchanges declares all necessary exchanges
func (p *Publisher) declareExchanges() error {
	exchanges := []string{
		"events.sensor",
		"events.device",
		"events.telemetry",
	}

	for _, exchange := range exchanges {
		err := p.channel.ExchangeDeclare(
			exchange, // name
			"topic",  // type
			true,     // durable
			false,    // auto-deleted
			false,    // internal
			false,    // no-wait
			nil,      // arguments
		)
		if err != nil {
			return fmt.Errorf("failed to declare exchange %s: %w", exchange, err)
		}
		log.Printf("Declared exchange: %s", exchange)
	}

	return nil
}

// PublishSensorCreated publishes a sensor created event
func (p *Publisher) PublishSensorCreated(sensorID int, name, sensorType, location string) error {
	event := SensorEvent{
		EventID:   fmt.Sprintf("sensor-created-%d-%d", sensorID, time.Now().Unix()),
		EventType: "sensor.created",
		SensorID:  sensorID,
		Name:      name,
		Type:      sensorType,
		Location:  location,
		Timestamp: time.Now(),
	}

	return p.publishEvent("events.sensor", "sensor.created", event)
}

// PublishSensorUpdated publishes a sensor updated event
func (p *Publisher) PublishSensorUpdated(sensorID int, name, sensorType, location string) error {
	event := SensorEvent{
		EventID:   fmt.Sprintf("sensor-updated-%d-%d", sensorID, time.Now().Unix()),
		EventType: "sensor.updated",
		SensorID:  sensorID,
		Name:      name,
		Type:      sensorType,
		Location:  location,
		Timestamp: time.Now(),
	}

	return p.publishEvent("events.sensor", "sensor.updated", event)
}

// PublishSensorValueChanged publishes a sensor value changed event
func (p *Publisher) PublishSensorValueChanged(sensorID int, name, sensorType, location string, value float64, status string) error {
	event := SensorEvent{
		EventID:   fmt.Sprintf("sensor-value-changed-%d-%d", sensorID, time.Now().Unix()),
		EventType: "sensor.value.changed",
		SensorID:  sensorID,
		Name:      name,
		Type:      sensorType,
		Location:  location,
		Value:     value,
		Status:    status,
		Timestamp: time.Now(),
	}

	return p.publishEvent("events.sensor", "sensor.value.changed", event)
}

// PublishSensorDeleted publishes a sensor deleted event
func (p *Publisher) PublishSensorDeleted(sensorID int, name, sensorType, location string) error {
	event := SensorEvent{
		EventID:   fmt.Sprintf("sensor-deleted-%d-%d", sensorID, time.Now().Unix()),
		EventType: "sensor.deleted",
		SensorID:  sensorID,
		Name:      name,
		Type:      sensorType,
		Location:  location,
		Timestamp: time.Now(),
	}

	return p.publishEvent("events.sensor", "sensor.deleted", event)
}

// publishEvent publishes an event to the specified exchange and routing key
func (p *Publisher) publishEvent(exchange, routingKey string, event interface{}) error {
	body, err := json.Marshal(event)
	if err != nil {
		return fmt.Errorf("failed to marshal event: %w", err)
	}

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	err = p.channel.PublishWithContext(
		ctx,
		exchange,   // exchange
		routingKey, // routing key
		false,      // mandatory
		false,      // immediate
		amqp091.Publishing{
			ContentType: "application/json",
			Body:        body,
			Timestamp:   time.Now(),
		},
	)

	if err != nil {
		return fmt.Errorf("failed to publish event: %w", err)
	}

	log.Printf("Published event %s to exchange %s with routing key %s",
		event.(SensorEvent).EventType, exchange, routingKey)
	return nil
}

// Close closes the RabbitMQ connection
func (p *Publisher) Close() error {
	if p.channel != nil {
		if err := p.channel.Close(); err != nil {
			log.Printf("Error closing RabbitMQ channel: %v", err)
		}
	}
	if p.conn != nil {
		if err := p.conn.Close(); err != nil {
			log.Printf("Error closing RabbitMQ connection: %v", err)
		}
	}
	log.Println("RabbitMQ publisher closed")
	return nil
}

// IsConnected checks if the RabbitMQ connection is still active
func (p *Publisher) IsConnected() bool {
	return p.conn != nil && !p.conn.IsClosed()
}
