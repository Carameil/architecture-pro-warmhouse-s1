package events

import (
	"context"
	"encoding/json"
	"fmt"
	"log"

	"device-registry/models"
	"device-registry/services"

	"github.com/google/uuid"
	"github.com/rabbitmq/amqp091-go"
)

// SensorEvent represents a sensor event from the monolith
type SensorEvent struct {
	EventID   string  `json:"event_id"`
	EventType string  `json:"event_type"`
	SensorID  int     `json:"sensor_id"`
	Name      string  `json:"name"`
	Type      string  `json:"type"`
	Location  string  `json:"location"`
	Value     float64 `json:"value,omitempty"`
	Status    string  `json:"status,omitempty"`
	Timestamp string  `json:"timestamp"`
}

// Subscriber handles consuming events from RabbitMQ
type Subscriber struct {
	conn              *amqp091.Connection
	channel           *amqp091.Channel
	deviceService     *services.DeviceService
	deviceTypeService *services.DeviceTypeService
}

// NewSubscriber creates a new RabbitMQ subscriber
func NewSubscriber(rabbitMQURL string, deviceService *services.DeviceService, deviceTypeService *services.DeviceTypeService) (*Subscriber, error) {
	conn, err := amqp091.Dial(rabbitMQURL)
	if err != nil {
		return nil, fmt.Errorf("failed to connect to RabbitMQ: %w", err)
	}

	channel, err := conn.Channel()
	if err != nil {
		conn.Close()
		return nil, fmt.Errorf("failed to open channel: %w", err)
	}

	subscriber := &Subscriber{
		conn:              conn,
		channel:           channel,
		deviceService:     deviceService,
		deviceTypeService: deviceTypeService,
	}

	// Setup exchanges and queues
	if err := subscriber.setup(); err != nil {
		subscriber.Close()
		return nil, fmt.Errorf("failed to setup subscriber: %w", err)
	}

	log.Println("RabbitMQ subscriber initialized successfully")
	return subscriber, nil
}

// setup declares exchanges and queues
func (s *Subscriber) setup() error {
	// Declare sensor events exchange
	err := s.channel.ExchangeDeclare(
		"events.sensor", // name
		"topic",         // type
		true,            // durable
		false,           // auto-deleted
		false,           // internal
		false,           // no-wait
		nil,             // arguments
	)
	if err != nil {
		return fmt.Errorf("failed to declare exchange: %w", err)
	}

	// Declare queue for device registry
	queueName := "device-registry.sensor-events"
	queue, err := s.channel.QueueDeclare(
		queueName, // name
		true,      // durable
		false,     // delete when unused
		false,     // exclusive
		false,     // no-wait
		nil,       // arguments
	)
	if err != nil {
		return fmt.Errorf("failed to declare queue: %w", err)
	}

	// Bind queue to exchange for sensor events
	err = s.channel.QueueBind(
		queue.Name,      // queue name
		"sensor.*",      // routing key pattern
		"events.sensor", // exchange
		false,           // no-wait
		nil,             // arguments
	)
	if err != nil {
		return fmt.Errorf("failed to bind queue: %w", err)
	}

	log.Printf("Setup complete: exchange events.sensor, queue %s", queueName)
	return nil
}

// StartListening starts consuming messages
func (s *Subscriber) StartListening(ctx context.Context) error {
	queueName := "device-registry.sensor-events"

	msgs, err := s.channel.Consume(
		queueName, // queue
		"",        // consumer
		false,     // auto-ack
		false,     // exclusive
		false,     // no-local
		false,     // no-wait
		nil,       // args
	)
	if err != nil {
		return fmt.Errorf("failed to register consumer: %w", err)
	}

	log.Printf("Started listening for sensor events on queue %s", queueName)

	for {
		select {
		case <-ctx.Done():
			log.Println("Stopping event subscriber")
			return nil
		case msg, ok := <-msgs:
			if !ok {
				log.Println("Message channel closed")
				return nil
			}

			if err := s.handleMessage(msg); err != nil {
				log.Printf("Error handling message: %v", err)
				msg.Nack(false, true) // Reject and requeue
			} else {
				msg.Ack(false) // Acknowledge
			}
		}
	}
}

// handleMessage processes incoming sensor events
func (s *Subscriber) handleMessage(msg amqp091.Delivery) error {
	var event SensorEvent
	if err := json.Unmarshal(msg.Body, &event); err != nil {
		return fmt.Errorf("failed to unmarshal event: %w", err)
	}

	log.Printf("Received sensor event: %s for sensor %d", event.EventType, event.SensorID)

	switch event.EventType {
	case "sensor.created":
		return s.handleSensorCreated(event)
	case "sensor.updated":
		return s.handleSensorUpdated(event)
	case "sensor.deleted":
		return s.handleSensorDeleted(event)
	default:
		log.Printf("Unknown event type: %s", event.EventType)
		return nil // Don't requeue unknown events
	}
}

// handleSensorCreated creates a corresponding device
func (s *Subscriber) handleSensorCreated(event SensorEvent) error {
	log.Printf("Creating device for sensor %d: %s", event.SensorID, event.Name)

	// Map sensor type to device type
	deviceTypeName := mapSensorTypeToDeviceType(event.Type)

	// Find device type ID
	deviceTypes, err := s.deviceTypeService.GetDeviceTypes(nil, nil)
	if err != nil {
		return fmt.Errorf("failed to get device types: %w", err)
	}

	var typeID uuid.UUID
	for _, dt := range deviceTypes {
		if dt.TypeName == deviceTypeName {
			typeID = dt.TypeID
			break
		}
	}

	if typeID == uuid.Nil {
		return fmt.Errorf("device type not found: %s", deviceTypeName)
	}

	// Generate serial number from sensor ID
	serialNumber := fmt.Sprintf("SENSOR_%d", event.SensorID)

	// Create device from sensor event
	device := models.DeviceRegistrationRequest{
		TypeID:       typeID,
		HouseID:      uuid.MustParse("550e8400-e29b-41d4-a716-446655440001"), // Default house for demo
		LocationID:   uuid.MustParse("550e8400-e29b-41d4-a716-446655440002"), // Default location for demo
		DeviceName:   event.Name,
		SerialNumber: serialNumber,
		Configuration: models.JSONB{
			"source":     "sensor_migration",
			"sensor_id":  event.SensorID,
			"created_by": "event_subscriber",
			"location":   event.Location,
		},
	}

	// Use system UUID for registeredBy
	systemUserID := uuid.MustParse("00000000-0000-0000-0000-000000000001")

	createdDevice, err := s.deviceService.CreateDevice(device, systemUserID)
	if err != nil {
		return fmt.Errorf("failed to create device: %w", err)
	}

	log.Printf("Successfully created device %s for sensor %d", createdDevice.DeviceID, event.SensorID)
	return nil
}

// handleSensorUpdated updates the corresponding device
func (s *Subscriber) handleSensorUpdated(event SensorEvent) error {
	log.Printf("Updating device for sensor %d: %s", event.SensorID, event.Name)

	// Find device by legacy sensor ID
	filter := models.DeviceFilter{
		Page:  1,
		Limit: 100,
	}
	devices, err := s.deviceService.GetDevices(filter)
	if err != nil {
		return fmt.Errorf("failed to get devices: %w", err)
	}

	for _, device := range devices {
		if device.LegacySensorID != nil && *device.LegacySensorID == event.SensorID {
			// Update device
			deviceUpdate := models.DeviceUpdateRequest{
				DeviceName: &event.Name,
			}

			_, err := s.deviceService.UpdateDevice(device.DeviceID, deviceUpdate)
			if err != nil {
				return fmt.Errorf("failed to update device %s: %w", device.DeviceID, err)
			}

			log.Printf("Successfully updated device %s for sensor %d", device.DeviceID, event.SensorID)
			return nil
		}
	}

	log.Printf("No device found for sensor %d", event.SensorID)
	return nil
}

// handleSensorDeleted deletes the corresponding device
func (s *Subscriber) handleSensorDeleted(event SensorEvent) error {
	log.Printf("Deleting device for sensor %d: %s", event.SensorID, event.Name)

	// Find device by legacy sensor ID
	filter := models.DeviceFilter{
		Page:  1,
		Limit: 100,
	}
	devices, err := s.deviceService.GetDevices(filter)
	if err != nil {
		return fmt.Errorf("failed to get devices: %w", err)
	}

	for _, device := range devices {
		if device.LegacySensorID != nil && *device.LegacySensorID == event.SensorID {
			err := s.deviceService.DeleteDevice(device.DeviceID)
			if err != nil {
				return fmt.Errorf("failed to delete device %s: %w", device.DeviceID, err)
			}

			log.Printf("Successfully deleted device %s for sensor %d", device.DeviceID, event.SensorID)
			return nil
		}
	}

	log.Printf("No device found for sensor %d", event.SensorID)
	return nil
}

// mapSensorTypeToDeviceType maps sensor types to device types
func mapSensorTypeToDeviceType(sensorType string) string {
	switch sensorType {
	case "temperature":
		return "Temperature Sensor"
	case "humidity":
		return "Temperature Sensor" // Temperature Sensor supports both temp and humidity
	case "motion":
		return "Motion Sensor"
	case "door":
		return "Smart Lock" // Door sensors can use smart lock type
	case "window":
		return "Smart Lock" // Window sensors can use smart lock type
	case "light":
		return "Smart Light"
	default:
		return "Temperature Sensor" // Default fallback
	}
}

// Close closes the RabbitMQ connection
func (s *Subscriber) Close() error {
	if s.channel != nil {
		if err := s.channel.Close(); err != nil {
			log.Printf("Error closing RabbitMQ channel: %v", err)
		}
	}
	if s.conn != nil {
		if err := s.conn.Close(); err != nil {
			log.Printf("Error closing RabbitMQ connection: %v", err)
		}
	}
	log.Println("RabbitMQ subscriber closed")
	return nil
}

// IsConnected checks if the RabbitMQ connection is still active
func (s *Subscriber) IsConnected() bool {
	return s.conn != nil && !s.conn.IsClosed()
}
