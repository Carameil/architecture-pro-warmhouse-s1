package models

import (
	"database/sql/driver"
	"encoding/json"
	"time"

	"github.com/google/uuid"
)

// DeviceType represents a type of device in the system
type DeviceType struct {
	TypeID        uuid.UUID `json:"type_id" db:"type_id"`
	TypeName      string    `json:"type_name" db:"type_name"`
	Category      string    `json:"category" db:"category"`
	Manufacturer  string    `json:"manufacturer" db:"manufacturer"`
	Model         string    `json:"model" db:"model"`
	Protocol      string    `json:"protocol" db:"protocol"`
	Capabilities  JSONB     `json:"capabilities" db:"capabilities"`
	DefaultConfig JSONB     `json:"default_config" db:"default_config"`
	IsActive      bool      `json:"is_active" db:"is_active"`
	CreatedAt     time.Time `json:"created_at" db:"created_at"`
	UpdatedAt     time.Time `json:"updated_at" db:"updated_at"`
}

// JSONB is a custom type for PostgreSQL JSONB columns
type JSONB map[string]interface{}

// Value implements driver.Valuer interface
func (j JSONB) Value() (driver.Value, error) {
	if j == nil {
		return nil, nil
	}
	return json.Marshal(j)
}

// Scan implements sql.Scanner interface
func (j *JSONB) Scan(value interface{}) error {
	if value == nil {
		*j = nil
		return nil
	}

	bytes, ok := value.([]byte)
	if !ok {
		return json.Unmarshal([]byte(value.(string)), j)
	}

	return json.Unmarshal(bytes, j)
}

// DeviceCategory constants
const (
	CategorySensor   = "sensor"
	CategoryActuator = "actuator"
)

// Protocol constants
const (
	ProtocolMQTT = "MQTT"
	ProtocolHTTP = "HTTP"
	ProtocolCoAP = "CoAP"
)
