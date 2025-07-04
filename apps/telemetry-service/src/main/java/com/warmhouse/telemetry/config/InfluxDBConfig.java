package com.warmhouse.telemetry.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.QueryApi;
import com.influxdb.client.DeleteApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for InfluxDB client
 */
@Configuration
public class InfluxDBConfig {
    
    private static final Logger log = LoggerFactory.getLogger(InfluxDBConfig.class);
    
    @Value("${influxdb.url}")
    private String url;
    
    @Value("${influxdb.token}")
    private String token;
    
    @Value("${influxdb.org}")
    private String org;
    
    @Value("${influxdb.bucket}")
    private String bucket;
    
    @Bean
    public InfluxDBClient influxDBClient() {
        log.info("Connecting to InfluxDB at: {}", url);
        InfluxDBClient client = InfluxDBClientFactory.create(url, token.toCharArray(), org, bucket);
        
        // Test connection
        try {
            if (client.ping()) {
                log.info("Successfully connected to InfluxDB");
            }
        } catch (Exception e) {
            log.error("Failed to connect to InfluxDB: {}", e.getMessage());
        }
        
        return client;
    }
    
    @Bean
    public WriteApiBlocking writeApi(InfluxDBClient client) {
        return client.getWriteApiBlocking();
    }
    
    @Bean
    public QueryApi queryApi(InfluxDBClient client) {
        return client.getQueryApi();
    }
    
    @Bean
    public DeleteApi deleteApi(InfluxDBClient client) {
        return client.getDeleteApi();
    }
    
    @Bean
    public String influxBucket() {
        return bucket;
    }
    
    @Bean 
    public String influxOrg() {
        return org;
    }
} 