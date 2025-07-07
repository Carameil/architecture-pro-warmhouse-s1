package models

import (
	"net"
	"time"

	"github.com/google/uuid"
)

// Device represents a smart home device
type Device struct {
	DeviceID         uuid.UUID  `json:"device_id" db:"device_id"`
	TypeID           uuid.UUID  `json:"type_id" db:"type_id"`
	HouseID          uuid.UUID  `json:"house_id" db:"house_id"`
	LocationID       uuid.UUID  `json:"location_id" db:"location_id"`
	RegisteredBy     uuid.UUID  `json:"registered_by" db:"registered_by"`
	DeviceName       string     `json:"device_name" db:"device_name"`
	SerialNumber     string     `json:"serial_number" db:"serial_number"`
	MacAddress       *string    `json:"mac_address,omitempty" db:"mac_address"`
	IPAddress        *net.IP    `json:"ip_address,omitempty" db:"ip_address"`
	FirmwareVersion  *string    `json:"firmware_version,omitempty" db:"firmware_version"`
	Configuration    JSONB      `json:"configuration" db:"configuration"`
	InstallationDate *time.Time `json:"installation_date,omitempty" db:"installation_date"`
	WarrantyExpires  *time.Time `json:"warranty_expires,omitempty" db:"warranty_expires"`
	IsOnline         bool       `json:"is_online" db:"is_online"`
	LastSeen         *time.Time `json:"last_seen,omitempty" db:"last_seen"`
	LegacySensorID   *int       `json:"legacy_sensor_id,omitempty" db:"legacy_sensor_id"`
	CreatedAt        time.Time  `json:"created_at" db:"created_at"`
	UpdatedAt        time.Time  `json:"updated_at" db:"updated_at"`

	// Joined fields from related tables (for API responses)
	Type *DeviceType `json:"type,omitempty" db:"-"`
}

// DeviceRegistrationRequest represents the request to register a new device
type DeviceRegistrationRequest struct {
	TypeID           uuid.UUID  `json:"type_id" binding:"required"`
	HouseID          uuid.UUID  `json:"house_id" binding:"required"`
	LocationID       uuid.UUID  `json:"location_id" binding:"required"`
	DeviceName       string     `json:"device_name" binding:"required,min=1,max=100"`
	SerialNumber     string     `json:"serial_number" binding:"required,min=1,max=100"`
	MacAddress       *string    `json:"mac_address,omitempty"`
	FirmwareVersion  *string    `json:"firmware_version,omitempty"`
	Configuration    JSONB      `json:"configuration,omitempty"`
	InstallationDate *time.Time `json:"installation_date,omitempty"`
	WarrantyExpires  *time.Time `json:"warranty_expires,omitempty"`
	LegacySensorID   *int       `json:"legacy_sensor_id,omitempty"`
}

// DeviceUpdateRequest represents the request to update device metadata
type DeviceUpdateRequest struct {
	DeviceName       *string    `json:"device_name,omitempty" binding:"omitempty,min=1,max=100"`
	Configuration    JSONB      `json:"configuration,omitempty"`
	FirmwareVersion  *string    `json:"firmware_version,omitempty"`
	InstallationDate *time.Time `json:"installation_date,omitempty"`
	WarrantyExpires  *time.Time `json:"warranty_expires,omitempty"`
}

// DeviceFilter represents query parameters for filtering devices
type DeviceFilter struct {
	HouseID    *uuid.UUID `form:"house_id"`
	LocationID *uuid.UUID `form:"location_id"`
	TypeID     *uuid.UUID `form:"type_id"`
	IsOnline   *bool      `form:"is_online"`
	Category   *string    `form:"category"`
	Page       int        `form:"page,default=1" binding:"min=1"`
	Limit      int        `form:"limit,default=20" binding:"min=1,max=100"`
}

// DeviceState represents the current state of a device (cached in Redis)
type DeviceState struct {
	DeviceID   uuid.UUID              `json:"device_id"`
	State      map[string]interface{} `json:"state"`
	LastUpdate time.Time              `json:"last_update"`
}
