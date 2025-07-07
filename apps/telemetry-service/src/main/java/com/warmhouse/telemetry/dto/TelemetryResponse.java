package com.warmhouse.telemetry.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for telemetry data response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelemetryResponse {
    
    private UUID measurementId;
    private UUID deviceId;
    private UUID houseId;
    private UUID locationId;
    
    private String measurementType;
    private Double value;
    private String unit;
    private String quality;
    
    private Instant timestamp;
    
    private Map<String, String> tags;
    private Map<String, Object> metadata;
    
    // Factory method from model
    public static TelemetryResponse fromModel(com.warmhouse.telemetry.model.TelemetryData data) {
        return TelemetryResponse.builder()
                .measurementId(data.getMeasurementId())
                .deviceId(data.getDeviceId())
                .houseId(data.getHouseId())
                .locationId(data.getLocationId())
                .measurementType(data.getMeasurementType())
                .value(data.getValue())
                .unit(data.getUnit())
                .quality(data.getQuality())
                .timestamp(data.getTimestamp())
                .tags(data.getTags())
                .metadata(data.getMetadata())
                .build();
    }
} 