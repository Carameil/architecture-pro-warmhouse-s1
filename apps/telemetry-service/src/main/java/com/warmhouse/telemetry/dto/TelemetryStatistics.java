package com.warmhouse.telemetry.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
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
    
    /**
     * Convert statistics to event data for RabbitMQ publishing
     */
    public Map<String, Object> toEventData() {
        Map<String, Object> eventData = new HashMap<>();
        
        // Basic statistics
        eventData.put("min", min);
        eventData.put("max", max);
        eventData.put("avg", avg);
        eventData.put("sum", sum);
        eventData.put("count", count);
        
        // Time information
        eventData.put("period_start", periodStart);
        eventData.put("period_end", periodEnd);
        
        // Optional metadata
        if (deviceName != null) {
            eventData.put("device_name", deviceName);
        }
        if (locationName != null) {
            eventData.put("location_name", locationName);
        }
        
        return eventData;
    }
} 