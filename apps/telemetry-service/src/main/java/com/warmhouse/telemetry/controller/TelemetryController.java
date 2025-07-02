package com.warmhouse.telemetry.controller;

import com.warmhouse.telemetry.dto.TelemetryRequest;
import com.warmhouse.telemetry.dto.TelemetryResponse;
import com.warmhouse.telemetry.dto.TelemetryStatistics;
import com.warmhouse.telemetry.service.TelemetryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for telemetry endpoints
 */
@RestController
@RequestMapping("/api/v1/telemetry")
@RequiredArgsConstructor
public class TelemetryController {
    
    private static final Logger log = LoggerFactory.getLogger(TelemetryController.class);
    
    private final TelemetryService telemetryService;
    
    /**
     * Store telemetry data
     * POST /api/v1/telemetry
     */
    @PostMapping
    public ResponseEntity<TelemetryResponse> storeTelemetryData(@Valid @RequestBody TelemetryRequest request) {
        log.info("Received telemetry data for device: {}", request.getDeviceId());
        
        try {
            TelemetryResponse response = telemetryService.storeTelemetryData(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error storing telemetry data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Store batch of telemetry data
     * POST /api/v1/telemetry/batch
     */
    @PostMapping("/batch")
    public ResponseEntity<Void> storeTelemetryDataBatch(@Valid @RequestBody List<TelemetryRequest> requests) {
        log.info("Received batch of {} telemetry data points", requests.size());
        
        try {
            telemetryService.storeTelemetryDataBatch(requests);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception e) {
            log.error("Error storing telemetry data batch: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get telemetry data for a specific device
     * GET /api/v1/telemetry/devices/{deviceId}
     */
    @GetMapping("/devices/{deviceId}")
    public ResponseEntity<List<TelemetryResponse>> getTelemetryByDevice(
            @PathVariable UUID deviceId,
            @RequestParam(defaultValue = "24h") String period) {
        
        log.info("Fetching telemetry data for device: {} with period: {}", deviceId, period);
        
        try {
            List<TelemetryResponse> responses = telemetryService.getTelemetryByDevice(deviceId, period);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error fetching telemetry data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get telemetry statistics for a device
     * GET /api/v1/telemetry/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<TelemetryStatistics> getTelemetryStatistics(
            @RequestParam UUID deviceId,
            @RequestParam String measurementType,
            @RequestParam(defaultValue = "24h") String period) {
        
        log.info("Calculating statistics for device: {}, type: {}, period: {}", 
                deviceId, measurementType, period);
        
        try {
            TelemetryStatistics stats = telemetryService.getTelemetryStatistics(
                    deviceId, measurementType, period);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error calculating telemetry statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
} 