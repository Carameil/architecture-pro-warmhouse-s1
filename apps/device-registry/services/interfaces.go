package services

// EventPublisher interface for publishing device events
type EventPublisher interface {
	PublishDeviceCreated(deviceID, houseID, locationID, deviceName, deviceType string) error
	PublishDeviceUpdated(deviceID, houseID, locationID, deviceName, deviceType string) error
	PublishDeviceDeleted(deviceID, houseID, locationID, deviceName, deviceType string) error
}
