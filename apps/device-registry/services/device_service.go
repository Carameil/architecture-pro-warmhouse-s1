package services

import (
	"database/sql"
	"fmt"
	"log"
	"time"

	"device-registry/db"
	"device-registry/models"

	"github.com/google/uuid"
)

// DeviceService handles device operations
type DeviceService struct {
	db             *db.DB
	eventPublisher EventPublisher
}

// NewDeviceService creates a new device service
func NewDeviceService(database *db.DB) *DeviceService {
	return &DeviceService{
		db: database,
	}
}

// SetEventPublisher sets the event publisher for the service
func (s *DeviceService) SetEventPublisher(publisher EventPublisher) {
	s.eventPublisher = publisher
}

// GetDevices returns a list of devices with filtering
func (s *DeviceService) GetDevices(filter models.DeviceFilter) ([]models.Device, error) {
	query := `
		SELECT d.device_id, d.type_id, d.house_id, d.location_id, d.registered_by,
		       d.device_name, d.serial_number, d.mac_address, d.ip_address,
		       d.firmware_version, d.configuration, d.installation_date,
		       d.warranty_expires, d.is_online, d.last_seen, d.legacy_sensor_id,
		       d.created_at, d.updated_at,
		       dt.type_name, dt.category, dt.manufacturer, dt.model, dt.protocol
		FROM devices d
		JOIN device_types dt ON d.type_id = dt.type_id
		WHERE 1=1`

	args := []interface{}{}
	argCount := 0

	// Add filters
	if filter.HouseID != nil {
		argCount++
		query += fmt.Sprintf(" AND d.house_id = $%d", argCount)
		args = append(args, *filter.HouseID)
	}

	if filter.LocationID != nil {
		argCount++
		query += fmt.Sprintf(" AND d.location_id = $%d", argCount)
		args = append(args, *filter.LocationID)
	}

	if filter.TypeID != nil {
		argCount++
		query += fmt.Sprintf(" AND d.type_id = $%d", argCount)
		args = append(args, *filter.TypeID)
	}

	if filter.IsOnline != nil {
		argCount++
		query += fmt.Sprintf(" AND d.is_online = $%d", argCount)
		args = append(args, *filter.IsOnline)
	}

	if filter.Category != nil {
		argCount++
		query += fmt.Sprintf(" AND dt.category = $%d", argCount)
		args = append(args, *filter.Category)
	}

	// Add pagination
	offset := (filter.Page - 1) * filter.Limit
	argCount++
	query += fmt.Sprintf(" ORDER BY d.created_at DESC LIMIT $%d", argCount)
	args = append(args, filter.Limit)

	argCount++
	query += fmt.Sprintf(" OFFSET $%d", argCount)
	args = append(args, offset)

	rows, err := s.db.Query(query, args...)
	if err != nil {
		return nil, fmt.Errorf("failed to query devices: %w", err)
	}
	defer rows.Close()

	devices := []models.Device{}
	for rows.Next() {
		var device models.Device
		var deviceType models.DeviceType

		err := rows.Scan(
			&device.DeviceID, &device.TypeID, &device.HouseID, &device.LocationID,
			&device.RegisteredBy, &device.DeviceName, &device.SerialNumber,
			&device.MacAddress, &device.IPAddress, &device.FirmwareVersion,
			&device.Configuration, &device.InstallationDate, &device.WarrantyExpires,
			&device.IsOnline, &device.LastSeen, &device.LegacySensorID,
			&device.CreatedAt, &device.UpdatedAt,
			&deviceType.TypeName, &deviceType.Category, &deviceType.Manufacturer,
			&deviceType.Model, &deviceType.Protocol,
		)
		if err != nil {
			return nil, fmt.Errorf("failed to scan device: %w", err)
		}

		deviceType.TypeID = device.TypeID
		device.Type = &deviceType
		devices = append(devices, device)
	}

	return devices, nil
}

// GetDeviceByID returns a device by its ID
func (s *DeviceService) GetDeviceByID(deviceID uuid.UUID) (*models.Device, error) {
	query := `
		SELECT d.device_id, d.type_id, d.house_id, d.location_id, d.registered_by,
		       d.device_name, d.serial_number, d.mac_address, d.ip_address,
		       d.firmware_version, d.configuration, d.installation_date,
		       d.warranty_expires, d.is_online, d.last_seen, d.legacy_sensor_id,
		       d.created_at, d.updated_at,
		       dt.type_name, dt.category, dt.manufacturer, dt.model, dt.protocol
		FROM devices d
		JOIN device_types dt ON d.type_id = dt.type_id
		WHERE d.device_id = $1`

	var device models.Device
	var deviceType models.DeviceType

	err := s.db.QueryRow(query, deviceID).Scan(
		&device.DeviceID, &device.TypeID, &device.HouseID, &device.LocationID,
		&device.RegisteredBy, &device.DeviceName, &device.SerialNumber,
		&device.MacAddress, &device.IPAddress, &device.FirmwareVersion,
		&device.Configuration, &device.InstallationDate, &device.WarrantyExpires,
		&device.IsOnline, &device.LastSeen, &device.LegacySensorID,
		&device.CreatedAt, &device.UpdatedAt,
		&deviceType.TypeName, &deviceType.Category, &deviceType.Manufacturer,
		&deviceType.Model, &deviceType.Protocol,
	)

	if err != nil {
		if err == sql.ErrNoRows {
			return nil, nil // Device not found
		}
		return nil, fmt.Errorf("failed to query device: %w", err)
	}

	deviceType.TypeID = device.TypeID
	device.Type = &deviceType

	return &device, nil
}

// CreateDevice creates a new device
func (s *DeviceService) CreateDevice(req models.DeviceRegistrationRequest, registeredBy uuid.UUID) (*models.Device, error) {
	// Check if serial number already exists
	var exists bool
	err := s.db.QueryRow("SELECT EXISTS(SELECT 1 FROM devices WHERE serial_number = $1)", req.SerialNumber).Scan(&exists)
	if err != nil {
		return nil, fmt.Errorf("failed to check serial number: %w", err)
	}
	if exists {
		return nil, fmt.Errorf("device with serial number %s already exists", req.SerialNumber)
	}

	deviceID := uuid.New()
	now := time.Now()

	query := `
		INSERT INTO devices (device_id, type_id, house_id, location_id, registered_by,
		                    device_name, serial_number, mac_address, firmware_version,
		                    configuration, installation_date, warranty_expires, legacy_sensor_id, created_at, updated_at)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15)
		RETURNING device_id`

	err = s.db.QueryRow(query,
		deviceID, req.TypeID, req.HouseID, req.LocationID, registeredBy,
		req.DeviceName, req.SerialNumber, req.MacAddress, req.FirmwareVersion,
		req.Configuration, req.InstallationDate, req.WarrantyExpires, req.LegacySensorID, now, now,
	).Scan(&deviceID)

	if err != nil {
		return nil, fmt.Errorf("failed to create device: %w", err)
	}

	// Return the created device
	return s.GetDeviceByID(deviceID)
}

// UpdateDevice updates device metadata
func (s *DeviceService) UpdateDevice(deviceID uuid.UUID, req models.DeviceUpdateRequest) (*models.Device, error) {
	// Build dynamic update query
	setParts := []string{}
	args := []interface{}{}
	argCount := 0

	if req.DeviceName != nil {
		argCount++
		setParts = append(setParts, fmt.Sprintf("device_name = $%d", argCount))
		args = append(args, *req.DeviceName)
	}

	if req.Configuration != nil {
		argCount++
		setParts = append(setParts, fmt.Sprintf("configuration = $%d", argCount))
		args = append(args, req.Configuration)
	}

	if req.FirmwareVersion != nil {
		argCount++
		setParts = append(setParts, fmt.Sprintf("firmware_version = $%d", argCount))
		args = append(args, *req.FirmwareVersion)
	}

	if req.InstallationDate != nil {
		argCount++
		setParts = append(setParts, fmt.Sprintf("installation_date = $%d", argCount))
		args = append(args, *req.InstallationDate)
	}

	if req.WarrantyExpires != nil {
		argCount++
		setParts = append(setParts, fmt.Sprintf("warranty_expires = $%d", argCount))
		args = append(args, *req.WarrantyExpires)
	}

	if len(setParts) == 0 {
		return s.GetDeviceByID(deviceID) // No updates, return existing
	}

	// Add updated_at
	argCount++
	setParts = append(setParts, fmt.Sprintf("updated_at = $%d", argCount))
	args = append(args, time.Now())

	// Add WHERE clause
	argCount++
	args = append(args, deviceID)

	query := fmt.Sprintf("UPDATE devices SET %s WHERE device_id = $%d",
		fmt.Sprintf("%s", setParts[0]+", "+setParts[1:][0]), argCount)

	// Fix query building
	if len(setParts) > 1 {
		query = "UPDATE devices SET "
		for i, part := range setParts {
			if i > 0 {
				query += ", "
			}
			query += part
		}
		query += fmt.Sprintf(" WHERE device_id = $%d", argCount)
	} else {
		query = fmt.Sprintf("UPDATE devices SET %s WHERE device_id = $%d", setParts[0], argCount)
	}

	_, err := s.db.Exec(query, args...)
	if err != nil {
		return nil, fmt.Errorf("failed to update device: %w", err)
	}

	return s.GetDeviceByID(deviceID)
}

// DeleteDevice deletes a device and publishes cascading deletion event
func (s *DeviceService) DeleteDevice(deviceID uuid.UUID) error {
	// Get device info before deletion for event publishing
	device, err := s.GetDeviceByID(deviceID)
	if err != nil {
		return fmt.Errorf("failed to get device for deletion: %w", err)
	}
	if device == nil {
		return fmt.Errorf("device not found")
	}

	// Delete the device
	result, err := s.db.Exec("DELETE FROM devices WHERE device_id = $1", deviceID)
	if err != nil {
		return fmt.Errorf("failed to delete device: %w", err)
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		return fmt.Errorf("failed to get rows affected: %w", err)
	}

	if rowsAffected == 0 {
		return fmt.Errorf("device not found")
	}

	// Publish device deleted event for cascading deletion
	if s.eventPublisher != nil {
		err = s.eventPublisher.PublishDeviceDeleted(
			device.DeviceID.String(),
			device.HouseID.String(),
			device.LocationID.String(),
			device.DeviceName,
			device.Type.TypeName,
		)
		if err != nil {
			log.Printf("Warning: Failed to publish device deleted event for %s: %v", deviceID, err)
			// Don't fail the deletion if event publishing fails
		} else {
			log.Printf("Published device.deleted event for device %s", deviceID)
		}
	}

	return nil
}
