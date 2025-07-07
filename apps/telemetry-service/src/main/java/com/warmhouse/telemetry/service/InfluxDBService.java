package com.warmhouse.telemetry.service;

import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.warmhouse.telemetry.dto.TelemetryStatistics;
import com.warmhouse.telemetry.model.TelemetryData;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for InfluxDB operations
 */
@Service
@RequiredArgsConstructor
public class InfluxDBService {
    
    private static final Logger log = LoggerFactory.getLogger(InfluxDBService.class);
    
    private final WriteApiBlocking writeApi;
    private final QueryApi queryApi;
    
    @Value("${influxdb.bucket}")
    private String bucket;
    
    @Value("${influxdb.org}")
    private String org;
    
    private static final String MEASUREMENT_NAME = "telemetry";
    
    /**
     * Write single telemetry data point to InfluxDB
     */
    public void writeTelemetryData(TelemetryData data) {
        try {
            Point point = createPointFromTelemetryData(data);
            writeApi.writePoint(point);
            log.info("Successfully wrote telemetry data for device: {}, type: {}, value: {}, timestamp: {}", 
                data.getDeviceId(), data.getMeasurementType(), data.getValue(), data.getTimestamp());
        } catch (Exception e) {
            log.error("Failed to write telemetry data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to write telemetry data", e);
        }
    }
    
    /**
     * Write batch of telemetry data points to InfluxDB
     */
    public void writeTelemetryDataBatch(List<TelemetryData> dataList) {
        try {
            List<Point> points = dataList.stream()
                    .map(this::createPointFromTelemetryData)
                    .collect(Collectors.toList());
            
            writeApi.writePoints(points);
            log.info("Successfully wrote {} telemetry data points", dataList.size());
        } catch (Exception e) {
            log.error("Failed to write telemetry data batch: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to write telemetry data batch", e);
        }
    }
    
    /**
     * Query telemetry data by device ID
     */
    public List<TelemetryData> queryByDeviceId(UUID deviceId, Instant start, Instant end) {
        String flux = String.format(
            "from(bucket: \"%s\")" +
            " |> range(start: %s, stop: %s)" +
            " |> filter(fn: (r) => r._measurement == \"%s\")" +
            " |> filter(fn: (r) => r.device_id == \"%s\")" +
            " |> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")",
            bucket, start.toString(), end.toString(), MEASUREMENT_NAME, deviceId.toString()
        );
        
        return executeQueryAndMapResults(flux);
    }
    
    /**
     * Calculate statistics for a device
     */
    public TelemetryStatistics calculateStatistics(UUID deviceId, String measurementType, String period) {
        Instant end = Instant.now();
        Instant start = calculateStartTime(end, period);
        
        String flux = String.format(
            "from(bucket: \"%s\")" +
            " |> range(start: %s, stop: %s)" +
            " |> filter(fn: (r) => r._measurement == \"%s\")" +
            " |> filter(fn: (r) => r.device_id == \"%s\")" +
            " |> filter(fn: (r) => r.measurement_type == \"%s\")" +
            " |> filter(fn: (r) => r._field == \"value\")",
            bucket, start.toString(), end.toString(), MEASUREMENT_NAME, 
            deviceId.toString(), measurementType
        );
        
        log.info("Calculating statistics for device: {}, type: {}, period: {} - from {} to {}", 
            deviceId, measurementType, period, start, end);
        log.debug("Flux query: {}", flux);
        
        // Calculate aggregations
        Map<String, Double> stats = new HashMap<>();
        stats.put("min", queryAggregation(flux + " |> min()", "min"));
        stats.put("max", queryAggregation(flux + " |> max()", "max"));
        stats.put("mean", queryAggregation(flux + " |> mean()", "mean"));
        stats.put("sum", queryAggregation(flux + " |> sum()", "sum"));
        stats.put("count", queryAggregation(flux + " |> count()", "count"));
        
        log.debug("Statistics calculated: min={}, max={}, mean={}, sum={}, count={}", 
            stats.get("min"), stats.get("max"), stats.get("mean"), stats.get("sum"), stats.get("count"));
        
        return TelemetryStatistics.builder()
                .deviceId(deviceId)
                .measurementType(measurementType)
                .period(period)
                .min(stats.get("min"))
                .max(stats.get("max"))
                .avg(stats.get("mean"))
                .sum(stats.get("sum"))
                .count(stats.get("count").longValue())
                .periodStart(start)
                .periodEnd(end)
                .build();
    }
    
    private Point createPointFromTelemetryData(TelemetryData data) {
        // Normalize quality to uppercase
        String normalizedQuality = data.getQuality() != null ? data.getQuality().toUpperCase() : "UNKNOWN";
        
        Point point = Point.measurement(MEASUREMENT_NAME)
                .time(data.getTimestamp(), WritePrecision.NS)
                .addTag("device_id", data.getDeviceId().toString())
                .addTag("house_id", data.getHouseId().toString())
                .addTag("location_id", data.getLocationId().toString())
                .addTag("measurement_type", data.getMeasurementType())
                .addTag("quality", normalizedQuality)
                .addField("value", data.getValue())
                .addField("unit", data.getUnit() != null ? data.getUnit() : "");
        
        // Add custom tags if present
        if (data.getTags() != null) {
            data.getTags().forEach(point::addTag);
        }
        
        // Add metadata fields if present
        if (data.getMetadata() != null) {
            data.getMetadata().forEach((key, value) -> {
                if (value instanceof Number) {
                    point.addField(key, ((Number) value).doubleValue());
                } else {
                    point.addField(key, value.toString());
                }
            });
        }
        
        log.debug("Created point for device: {}, measurement: {}, value: {}, quality: {}, timestamp: {}", 
            data.getDeviceId(), data.getMeasurementType(), data.getValue(), normalizedQuality, data.getTimestamp());
        
        return point;
    }
    
    private List<TelemetryData> executeQueryAndMapResults(String flux) {
        List<TelemetryData> results = new ArrayList<>();
        
        try {
            List<FluxTable> tables = queryApi.query(flux, org);
            
            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    TelemetryData data = mapRecordToTelemetryData(record);
                    if (data != null) {
                        results.add(data);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to query telemetry data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to query telemetry data", e);
        }
        
        return results;
    }
    
    private TelemetryData mapRecordToTelemetryData(FluxRecord record) {
        try {
            return TelemetryData.builder()
                    .deviceId(UUID.fromString((String) record.getValueByKey("device_id")))
                    .houseId(UUID.fromString((String) record.getValueByKey("house_id")))
                    .locationId(UUID.fromString((String) record.getValueByKey("location_id")))
                    .measurementType((String) record.getValueByKey("measurement_type"))
                    .value(((Number) record.getValueByKey("value")).doubleValue())
                    .unit((String) record.getValueByKey("unit"))
                    .quality((String) record.getValueByKey("quality"))
                    .timestamp(record.getTime())
                    .build();
        } catch (Exception e) {
            log.warn("Failed to map record to TelemetryData: {}", e.getMessage());
            return null;
        }
    }
    
    private Double queryAggregation(String flux, String aggregationType) {
        try {
            log.debug("Executing {} aggregation query: {}", aggregationType, flux);
            List<FluxTable> tables = queryApi.query(flux, org);
            log.debug("{} query returned {} tables", aggregationType, tables.size());
            
            // For aggregations like count, sum - we need to process all tables and combine results
            if ("count".equals(aggregationType) || "sum".equals(aggregationType)) {
                double total = 0.0;
                for (FluxTable table : tables) {
                    if (!table.getRecords().isEmpty()) {
                        Object value = table.getRecords().get(0).getValue();
                        if (value instanceof Number) {
                            total += ((Number) value).doubleValue();
                            log.debug("{} table result: {}", aggregationType, value);
                        }
                    }
                }
                log.debug("{} aggregation total result: {}", aggregationType, total);
                return total;
            }
            
            // For min, max, mean - find the appropriate value across all tables
            List<Double> values = new ArrayList<>();
            for (FluxTable table : tables) {
                if (!table.getRecords().isEmpty()) {
                    Object value = table.getRecords().get(0).getValue();
                    if (value instanceof Number) {
                        values.add(((Number) value).doubleValue());
                        log.debug("{} table result: {}", aggregationType, value);
                    }
                }
            }
            
            if (!values.isEmpty()) {
                double result;
                switch (aggregationType) {
                    case "min":
                        result = values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
                        break;
                    case "max":
                        result = values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
                        break;
                    case "mean":
                        result = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                        break;
                    default:
                        result = values.get(0);
                }
                log.debug("{} aggregation final result: {}", aggregationType, result);
                return result;
            } else {
                log.warn("No data found for {} aggregation query", aggregationType);
            }
        } catch (Exception e) {
            log.warn("Failed to query {} aggregation: {}", aggregationType, e.getMessage());
        }
        return 0.0;
    }
    
    private Instant calculateStartTime(Instant end, String period) {
        switch (period.toLowerCase()) {
            case "1h":
                return end.minus(1, ChronoUnit.HOURS);
            case "24h":
            case "1d":
                return end.minus(1, ChronoUnit.DAYS);
            case "7d":
            case "1w":
                return end.minus(7, ChronoUnit.DAYS);
            case "30d":
            case "1m":
                return end.minus(30, ChronoUnit.DAYS);
            default:
                return end.minus(1, ChronoUnit.HOURS);
        }
    }
} 