package com.warmhouse.telemetry.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Model for telemetry data representing sensor measurements
 * Stored in InfluxDB as time-series data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelemetryData {
    
    private UUID measurementId;
    
    // Required fields for validation
    private UUID deviceId;
    private UUID houseId;
    private UUID locationId;
    
    // Measurement details
    private String measurementType;
    private Double value;
    private String unit;
    private String quality; // e.g., "GOOD", "BAD", "UNKNOWN"
    
    // Timestamp for time-series
    private Instant timestamp;
    
    // Additional metadata
    private Map<String, String> tags; // For InfluxDB tags
    private Map<String, Object> metadata; // Additional metadata
    
    // Pre-validation convenience method
    public void generateIdIfMissing() {
        if (measurementId == null) {
            measurementId = UUID.randomUUID();
        }
    }
    
    // Set current timestamp if not provided
    public void setTimestampIfMissing() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
} 