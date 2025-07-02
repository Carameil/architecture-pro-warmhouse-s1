package com.warmhouse.telemetry.events;

import com.warmhouse.telemetry.service.DeviceValidationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class TelemetryEventListener {

    private final DeviceValidationService deviceValidationService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public TelemetryEventListener(DeviceValidationService deviceValidationService,
                                RedisTemplate<String, Object> redisTemplate) {
        this.deviceValidationService = deviceValidationService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Listens to device and sensor events for cache invalidation
     */
    @RabbitListener(queues = RabbitMQConfig.TELEMETRY_DEVICE_EVENTS_QUEUE)
    public void handleDeviceEvents(Map<String, Object> eventData) {
        try {
            String eventType = (String) eventData.get("event_type");
            System.out.println("Telemetry Service received event: " + eventType);

            switch (eventType) {
                case "device.created":
                case "device.updated":
                    handleDeviceChange(eventData);
                    break;
                
                case "device.deleted":
                    handleDeviceDeleted(eventData);
                    break;

                case "sensor.created":
                    handleSensorCreated(eventData);
                    break;

                case "sensor.updated":
                case "sensor.deleted":
                    handleSensorChange(eventData);
                    break;

                default:
                    System.out.println("Unknown event type received: " + eventType);
            }
        } catch (Exception e) {
            System.err.println("Error processing device event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle device created/updated - refresh cache
     */
    private void handleDeviceChange(Map<String, Object> eventData) {
        Object deviceIdObj = eventData.get("device_id");
        if (deviceIdObj != null) {
            String deviceId = deviceIdObj.toString();
            
            // Invalidate device cache
            String cacheKey = "device_validation:" + deviceId;
            redisTemplate.delete(cacheKey);
            
            System.out.println("Invalidated device cache for device: " + deviceId);
            
            // Pre-warm cache for frequently accessed device
            try {
                deviceValidationService.validateDevice(java.util.UUID.fromString(deviceId));
                System.out.println("Pre-warmed cache for device: " + deviceId);
            } catch (Exception e) {
                System.err.println("Failed to pre-warm cache for device " + deviceId + ": " + e.getMessage());
            }
        }
    }

    /**
     * Handle device deleted - clear cache completely
     */
    private void handleDeviceDeleted(Map<String, Object> eventData) {
        Object deviceIdObj = eventData.get("device_id");
        if (deviceIdObj != null) {
            String deviceId = deviceIdObj.toString();
            
            // Clear all cache entries for this device
            String cachePattern = "*" + deviceId + "*";
            try {
                redisTemplate.delete(redisTemplate.keys(cachePattern));
                System.out.println("Cleared all cache for deleted device: " + deviceId);
            } catch (Exception e) {
                System.err.println("Failed to clear cache for deleted device " + deviceId + ": " + e.getMessage());
            }
        }
    }

    /**
     * Handle sensor created - correlation with future telemetry
     */
    private void handleSensorCreated(Map<String, Object> eventData) {
        Object sensorIdObj = eventData.get("sensor_id");
        Object nameObj = eventData.get("name");
        Object typeObj = eventData.get("type");
        Object locationObj = eventData.get("location");

        if (sensorIdObj != null) {
            // Store sensor metadata for potential correlation
            String correlationKey = "sensor_correlation:" + sensorIdObj.toString();
            
            Map<String, Object> sensorMetadata = Map.of(
                "sensor_id", sensorIdObj,
                "name", nameObj != null ? nameObj : "Unknown",
                "type", typeObj != null ? typeObj : "Unknown", 
                "location", locationObj != null ? locationObj : "Unknown",
                "created_at", System.currentTimeMillis()
            );
            
            // Store for 24 hours for correlation
            redisTemplate.opsForValue().set(correlationKey, sensorMetadata, 24, TimeUnit.HOURS);
            
            System.out.println("Stored sensor correlation data for sensor: " + sensorIdObj);
        }
    }

    /**
     * Handle sensor updated/deleted - update correlation data
     */
    private void handleSensorChange(Map<String, Object> eventData) {
        Object sensorIdObj = eventData.get("sensor_id");
        String eventType = (String) eventData.get("event_type");
        
        if (sensorIdObj != null) {
            String correlationKey = "sensor_correlation:" + sensorIdObj.toString();
            
            if ("sensor.deleted".equals(eventType)) {
                redisTemplate.delete(correlationKey);
                System.out.println("Removed sensor correlation for deleted sensor: " + sensorIdObj);
            } else {
                // Update correlation data
                Map<String, Object> existingData = (Map<String, Object>) redisTemplate.opsForValue().get(correlationKey);
                if (existingData != null) {
                    // Update with new data
                    Map<String, Object> updatedData = Map.of(
                        "sensor_id", sensorIdObj,
                        "name", eventData.getOrDefault("name", existingData.get("name")),
                        "type", eventData.getOrDefault("type", existingData.get("type")),
                        "location", eventData.getOrDefault("location", existingData.get("location")),
                        "updated_at", System.currentTimeMillis()
                    );
                    
                    redisTemplate.opsForValue().set(correlationKey, updatedData, 24, TimeUnit.HOURS);
                    System.out.println("Updated sensor correlation for sensor: " + sensorIdObj);
                }
            }
        }
    }
} 