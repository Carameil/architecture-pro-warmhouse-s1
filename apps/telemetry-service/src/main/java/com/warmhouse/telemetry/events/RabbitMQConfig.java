package com.warmhouse.telemetry.events;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    // Exchange names
    public static final String TELEMETRY_EXCHANGE = "events.telemetry";
    public static final String SENSOR_EXCHANGE = "events.sensor";
    public static final String DEVICE_EXCHANGE = "events.device";

    // Queue names
    public static final String TELEMETRY_DEVICE_EVENTS_QUEUE = "telemetry-service.device-events";
    
    // Routing keys
    public static final String MEASUREMENT_RECEIVED_KEY = "telemetry.measurement.received";
    public static final String MEASUREMENT_AGGREGATED_KEY = "telemetry.measurement.aggregated";

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    // Telemetry Events Exchange
    @Bean
    public TopicExchange telemetryExchange() {
        return ExchangeBuilder.topicExchange(TELEMETRY_EXCHANGE)
                .durable(true)
                .build();
    }

    // Queue for listening to device/location change events
    @Bean
    public Queue telemetryDeviceEventsQueue() {
        return QueueBuilder.durable(TELEMETRY_DEVICE_EVENTS_QUEUE)
                .build();
    }

    // Bind telemetry service to device events for cache invalidation
    @Bean
    public Binding telemetryDeviceEventsBinding() {
        TopicExchange deviceExchange = new TopicExchange(DEVICE_EXCHANGE, true, false);
        return BindingBuilder
                .bind(telemetryDeviceEventsQueue())
                .to(deviceExchange)
                .with("device.*");
    }

    // Bind telemetry service to sensor events for correlation
    @Bean
    public Binding telemetrySensorEventsBinding() {
        TopicExchange sensorExchange = new TopicExchange(SENSOR_EXCHANGE, true, false);
        return BindingBuilder
                .bind(telemetryDeviceEventsQueue())
                .to(sensorExchange)
                .with("sensor.*");
    }
} 