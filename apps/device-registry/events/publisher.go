package events

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"time"

	"github.com/rabbitmq/amqp091-go"
)

// DeviceEvent represents a device-related event
type DeviceEvent struct {
	EventID    string    `json:"event_id"`
	EventType  string    `json:"event_type"`
	DeviceID   string    `json:"device_id"`
	HouseID    string    `json:"house_id,omitempty"`
	LocationID string    `json:"location_id,omitempty"`
	DeviceName string    `json:"device_name,omitempty"`
	DeviceType string    `json:"device_type,omitempty"`
	Timestamp  time.Time `json:"timestamp"`
}

// Publisher handles publishing device events to RabbitMQ
type Publisher struct {
	conn    *amqp091.Connection
	channel *amqp091.Channel
}

// NewPublisher creates a new RabbitMQ publisher for device events
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

	// Declare device events exchange
	if err := publisher.declareExchange(); err != nil {
		publisher.Close()
		return nil, fmt.Errorf("failed to declare exchange: %w", err)
	}

	log.Println("Device event publisher initialized successfully")
	return publisher, nil
}

// declareExchange declares the device events exchange
func (p *Publisher) declareExchange() error {
	err := p.channel.ExchangeDeclare(
		"events.device", // name
		"topic",         // type
		true,            // durable
		false,           // auto-deleted
		false,           // internal
		false,           // no-wait
		nil,             // arguments
	)
	if err != nil {
		return fmt.Errorf("failed to declare device exchange: %w", err)
	}

	log.Println("Declared exchange: events.device")
	return nil
}

// PublishDeviceCreated publishes a device created event
func (p *Publisher) PublishDeviceCreated(deviceID, houseID, locationID, deviceName, deviceType string) error {
	event := DeviceEvent{
		EventID:    fmt.Sprintf("device-created-%s-%d", deviceID, time.Now().Unix()),
		EventType:  "device.created",
		DeviceID:   deviceID,
		HouseID:    houseID,
		LocationID: locationID,
		DeviceName: deviceName,
		DeviceType: deviceType,
		Timestamp:  time.Now(),
	}

	return p.publishEvent("device.created", event)
}

// PublishDeviceUpdated publishes a device updated event
func (p *Publisher) PublishDeviceUpdated(deviceID, houseID, locationID, deviceName, deviceType string) error {
	event := DeviceEvent{
		EventID:    fmt.Sprintf("device-updated-%s-%d", deviceID, time.Now().Unix()),
		EventType:  "device.updated",
		DeviceID:   deviceID,
		HouseID:    houseID,
		LocationID: locationID,
		DeviceName: deviceName,
		DeviceType: deviceType,
		Timestamp:  time.Now(),
	}

	return p.publishEvent("device.updated", event)
}

// PublishDeviceDeleted publishes a device deleted event for cascading deletion
func (p *Publisher) PublishDeviceDeleted(deviceID, houseID, locationID, deviceName, deviceType string) error {
	event := DeviceEvent{
		EventID:    fmt.Sprintf("device-deleted-%s-%d", deviceID, time.Now().Unix()),
		EventType:  "device.deleted",
		DeviceID:   deviceID,
		HouseID:    houseID,
		LocationID: locationID,
		DeviceName: deviceName,
		DeviceType: deviceType,
		Timestamp:  time.Now(),
	}

	return p.publishEvent("device.deleted", event)
}

// publishEvent publishes an event to the device exchange
func (p *Publisher) publishEvent(routingKey string, event DeviceEvent) error {
	body, err := json.Marshal(event)
	if err != nil {
		return fmt.Errorf("failed to marshal event: %w", err)
	}

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	err = p.channel.PublishWithContext(
		ctx,
		"events.device", // exchange
		routingKey,      // routing key
		false,           // mandatory
		false,           // immediate
		amqp091.Publishing{
			ContentType: "application/json",
			Body:        body,
			Timestamp:   time.Now(),
		},
	)

	if err != nil {
		return fmt.Errorf("failed to publish event: %w", err)
	}

	log.Printf("Published device event %s for device %s", event.EventType, event.DeviceID)
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
	log.Println("Device event publisher closed")
	return nil
}

// IsConnected checks if the RabbitMQ connection is still active
func (p *Publisher) IsConnected() bool {
	return p.conn != nil && !p.conn.IsClosed()
}
