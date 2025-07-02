package com.warmhouse.telemetry.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for telemetry statistics response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelemetryStatistics {
    
    private UUID deviceId;
    private String measurementType;
    private String period; // e.g., "1h", "24h", "7d"
    
    // Statistics
    private Double min;
    private Double max;
    private Double avg;
    private Double sum;
    private Long count;
    
    // Time range
    private Instant periodStart;
    private Instant periodEnd;
    
    // Optional device details
    private String deviceName;
    private String locationName;
} 