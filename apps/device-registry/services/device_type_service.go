package services

import (
	"fmt"

	"device-registry/db"
	"device-registry/models"
)

// DeviceTypeService handles device type operations
type DeviceTypeService struct {
	db *db.DB
}

// NewDeviceTypeService creates a new device type service
func NewDeviceTypeService(database *db.DB) *DeviceTypeService {
	return &DeviceTypeService{
		db: database,
	}
}

// GetDeviceTypes returns a list of device types with filtering
func (s *DeviceTypeService) GetDeviceTypes(category *string, isActive *bool) ([]models.DeviceType, error) {
	query := `
		SELECT type_id, type_name, category, manufacturer, model, protocol,
		       capabilities, default_config, is_active, created_at, updated_at
		FROM device_types
		WHERE 1=1`

	args := []interface{}{}
	argCount := 0

	// Add filters
	if category != nil {
		argCount++
		query += fmt.Sprintf(" AND category = $%d", argCount)
		args = append(args, *category)
	}

	if isActive != nil {
		argCount++
		query += fmt.Sprintf(" AND is_active = $%d", argCount)
		args = append(args, *isActive)
	}

	query += " ORDER BY type_name ASC"

	rows, err := s.db.Query(query, args...)
	if err != nil {
		return nil, fmt.Errorf("failed to query device types: %w", err)
	}
	defer rows.Close()

	deviceTypes := []models.DeviceType{}
	for rows.Next() {
		var deviceType models.DeviceType

		err := rows.Scan(
			&deviceType.TypeID, &deviceType.TypeName, &deviceType.Category,
			&deviceType.Manufacturer, &deviceType.Model, &deviceType.Protocol,
			&deviceType.Capabilities, &deviceType.DefaultConfig,
			&deviceType.IsActive, &deviceType.CreatedAt, &deviceType.UpdatedAt,
		)
		if err != nil {
			return nil, fmt.Errorf("failed to scan device type: %w", err)
		}

		deviceTypes = append(deviceTypes, deviceType)
	}

	return deviceTypes, nil
}
