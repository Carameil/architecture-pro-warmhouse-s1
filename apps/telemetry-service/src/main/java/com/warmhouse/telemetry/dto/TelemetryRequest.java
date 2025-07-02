package com.warmhouse.telemetry.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for telemetry data submission request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelemetryRequest {
    
    @NotNull(message = "Device ID is required")
    private UUID deviceId;
    
    @NotNull(message = "House ID is required")
    private UUID houseId;
    
    @NotNull(message = "Location ID is required")
    private UUID locationId;
    
    @NotNull(message = "Measurement type is required")
    private String measurementType;
    
    @NotNull(message = "Value is required")
    @Positive(message = "Value must be positive")
    private Double value;
    
    private String unit;
    private String quality;
    
    // Optional timestamp, will use current time if not provided
    private Instant timestamp;
    
    // Optional tags and metadata
    private Map<String, String> tags;
    private Map<String, Object> metadata;
} 