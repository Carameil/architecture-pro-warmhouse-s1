package com.warmhouse.telemetry.events;

import com.warmhouse.telemetry.model.TelemetryData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TelemetryEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public TelemetryEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publishes measurement received event when telemetry data is stored
     */
    public void publishMeasurementReceived(TelemetryData telemetryData) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("event_id", UUID.randomUUID().toString());
            eventData.put("event_type", "telemetry.measurement.received");
            eventData.put("measurement_id", telemetryData.getMeasurementId());
            eventData.put("device_id", telemetryData.getDeviceId());
            eventData.put("house_id", telemetryData.getHouseId());
            eventData.put("location_id", telemetryData.getLocationId());
            eventData.put("measurement_type", telemetryData.getMeasurementType());
            eventData.put("value", telemetryData.getValue());
            eventData.put("unit", telemetryData.getUnit());
            eventData.put("quality", telemetryData.getQuality());
            eventData.put("timestamp", telemetryData.getTimestamp());

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.TELEMETRY_EXCHANGE,
                RabbitMQConfig.MEASUREMENT_RECEIVED_KEY,
                eventData
            );
        } catch (Exception e) {
            // Log error but don't fail the main operation
            System.err.println("Failed to publish measurement received event: " + e.getMessage());
        }
    }

    /**
     * Publishes batch measurement events
     */
    public void publishBatchMeasurementsReceived(List<TelemetryData> telemetryDataList) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("event_id", UUID.randomUUID().toString());
            eventData.put("event_type", "telemetry.batch.received");
            eventData.put("batch_size", telemetryDataList.size());
            eventData.put("timestamp", LocalDateTime.now());
            
            // Add summary statistics
            long uniqueDevices = telemetryDataList.stream()
                .map(TelemetryData::getDeviceId)
                .distinct()
                .count();
            
            eventData.put("unique_devices", uniqueDevices);
            eventData.put("device_ids", telemetryDataList.stream()
                .map(TelemetryData::getDeviceId)
                .distinct()
                .toList());

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.TELEMETRY_EXCHANGE,
                "telemetry.batch.received",
                eventData
            );
        } catch (Exception e) {
            System.err.println("Failed to publish batch measurements event: " + e.getMessage());
        }
    }

    /**
     * Publishes aggregated statistics events
     */
    public void publishMeasurementAggregated(String deviceId, String measurementType, 
                                           String period, Map<String, Object> statistics) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("event_id", UUID.randomUUID().toString());
            eventData.put("event_type", "telemetry.measurement.aggregated");
            eventData.put("device_id", deviceId);
            eventData.put("measurement_type", measurementType);
            eventData.put("aggregation_period", period);
            eventData.put("statistics", statistics);
            eventData.put("timestamp", LocalDateTime.now());

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.TELEMETRY_EXCHANGE,
                RabbitMQConfig.MEASUREMENT_AGGREGATED_KEY,
                eventData
            );
        } catch (Exception e) {
            System.err.println("Failed to publish aggregated measurement event: " + e.getMessage());
        }
    }
} 