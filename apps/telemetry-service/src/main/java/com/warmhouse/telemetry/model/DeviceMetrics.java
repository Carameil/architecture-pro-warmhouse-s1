package com.warmhouse.telemetry.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Model for aggregated device metrics
 * Represents calculated statistics over a time period
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceMetrics {
    
    private UUID metricId;
    
    // Device identification
    private UUID deviceId;
    private UUID houseId;
    
    // Metric details
    private String metricName; // e.g., "temperature_avg", "humidity_max"
    private Double metricValue;
    
    // Aggregation period
    private String aggregationPeriod; // e.g., "1h", "1d", "1w"
    private Instant calculatedAt;
    private Instant periodStart;
    private Instant periodEnd;
    
    // Generate ID if missing
    public void generateIdIfMissing() {
        if (metricId == null) {
            metricId = UUID.randomUUID();
        }
    }
} 