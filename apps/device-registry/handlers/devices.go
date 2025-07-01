package handlers

import (
	"log"
	"net/http"
	"strconv"

	"device-registry/models"
	"device-registry/services"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
)

// DeviceHandler handles device-related requests
type DeviceHandler struct {
	deviceService     *services.DeviceService
	deviceTypeService *services.DeviceTypeService
}

// NewDeviceHandler creates a new device handler
func NewDeviceHandler(deviceService *services.DeviceService, deviceTypeService *services.DeviceTypeService) *DeviceHandler {
	return &DeviceHandler{
		deviceService:     deviceService,
		deviceTypeService: deviceTypeService,
	}
}

// GetDevices handles GET /api/v1/devices
func (h *DeviceHandler) GetDevices(c *gin.Context) {
	// Parse query parameters
	var filter models.DeviceFilter

	// House ID filter
	if houseIDStr := c.Query("house_id"); houseIDStr != "" {
		houseID, err := uuid.Parse(houseIDStr)
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid house_id format"})
			return
		}
		filter.HouseID = &houseID
	}

	// Location ID filter
	if locationIDStr := c.Query("location_id"); locationIDStr != "" {
		locationID, err := uuid.Parse(locationIDStr)
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid location_id format"})
			return
		}
		filter.LocationID = &locationID
	}

	// Type ID filter
	if typeIDStr := c.Query("type_id"); typeIDStr != "" {
		typeID, err := uuid.Parse(typeIDStr)
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid type_id format"})
			return
		}
		filter.TypeID = &typeID
	}

	// Online status filter
	if isOnlineStr := c.Query("is_online"); isOnlineStr != "" {
		isOnline, err := strconv.ParseBool(isOnlineStr)
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid is_online format"})
			return
		}
		filter.IsOnline = &isOnline
	}

	// Category filter
	if category := c.Query("category"); category != "" {
		filter.Category = &category
	}

	// Pagination
	page := 1
	if pageStr := c.Query("page"); pageStr != "" {
		var err error
		page, err = strconv.Atoi(pageStr)
		if err != nil || page < 1 {
			page = 1
		}
	}
	filter.Page = page

	limit := 20
	if limitStr := c.Query("limit"); limitStr != "" {
		var err error
		limit, err = strconv.Atoi(limitStr)
		if err != nil || limit < 1 || limit > 100 {
			limit = 20
		}
	}
	filter.Limit = limit

	// Get devices
	devices, err := h.deviceService.GetDevices(filter)
	if err != nil {
		log.Printf("Error getting devices: %v", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to get devices"})
		return
	}

	c.JSON(http.StatusOK, gin.H{"devices": devices})
}

// GetDeviceByID handles GET /api/v1/devices/{deviceId}
func (h *DeviceHandler) GetDeviceByID(c *gin.Context) {
	deviceIDStr := c.Param("deviceId")
	deviceID, err := uuid.Parse(deviceIDStr)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid device ID format"})
		return
	}

	device, err := h.deviceService.GetDeviceByID(deviceID)
	if err != nil {
		log.Printf("Error getting device %s: %v", deviceID, err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to get device"})
		return
	}

	if device == nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "Device not found"})
		return
	}

	c.JSON(http.StatusOK, device)
}

// CreateDevice handles POST /api/v1/devices
func (h *DeviceHandler) CreateDevice(c *gin.Context) {
	var req models.DeviceRegistrationRequest

	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	// For simplicity, use a fixed user ID (in real implementation, get from JWT)
	registeredBy := uuid.New() // This should come from authentication

	device, err := h.deviceService.CreateDevice(req, registeredBy)
	if err != nil {
		log.Printf("Error creating device: %v", err)
		if err.Error() == "device with serial number "+req.SerialNumber+" already exists" {
			c.JSON(http.StatusConflict, gin.H{"error": "Device with this serial number already exists"})
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to create device"})
		return
	}

	c.JSON(http.StatusCreated, device)
}

// UpdateDevice handles PUT /api/v1/devices/{deviceId}
func (h *DeviceHandler) UpdateDevice(c *gin.Context) {
	deviceIDStr := c.Param("deviceId")
	deviceID, err := uuid.Parse(deviceIDStr)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid device ID format"})
		return
	}

	var req models.DeviceUpdateRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	// Check if device exists first
	existing, err := h.deviceService.GetDeviceByID(deviceID)
	if err != nil {
		log.Printf("Error checking device %s: %v", deviceID, err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to check device"})
		return
	}

	if existing == nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "Device not found"})
		return
	}

	device, err := h.deviceService.UpdateDevice(deviceID, req)
	if err != nil {
		log.Printf("Error updating device %s: %v", deviceID, err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to update device"})
		return
	}

	c.JSON(http.StatusOK, device)
}

// DeleteDevice handles DELETE /api/v1/devices/{deviceId}
func (h *DeviceHandler) DeleteDevice(c *gin.Context) {
	deviceIDStr := c.Param("deviceId")
	deviceID, err := uuid.Parse(deviceIDStr)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid device ID format"})
		return
	}

	// Check if device exists first
	existing, err := h.deviceService.GetDeviceByID(deviceID)
	if err != nil {
		log.Printf("Error checking device %s: %v", deviceID, err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to check device"})
		return
	}

	if existing == nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "Device not found"})
		return
	}

	err = h.deviceService.DeleteDevice(deviceID)
	if err != nil {
		log.Printf("Error deleting device %s: %v", deviceID, err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to delete device"})
		return
	}

	c.Status(http.StatusNoContent)
}

// GetDeviceTypes handles GET /api/v1/device-types
func (h *DeviceHandler) GetDeviceTypes(c *gin.Context) {
	// Parse query parameters
	var category *string
	if categoryStr := c.Query("category"); categoryStr != "" {
		category = &categoryStr
	}

	var isActive *bool
	if isActiveStr := c.Query("is_active"); isActiveStr != "" {
		activeVal, err := strconv.ParseBool(isActiveStr)
		if err == nil {
			isActive = &activeVal
		}
	} else {
		// Default to active only
		activeVal := true
		isActive = &activeVal
	}

	deviceTypes, err := h.deviceTypeService.GetDeviceTypes(category, isActive)
	if err != nil {
		log.Printf("Error getting device types: %v", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to get device types"})
		return
	}

	c.JSON(http.StatusOK, gin.H{"device_types": deviceTypes})
}
