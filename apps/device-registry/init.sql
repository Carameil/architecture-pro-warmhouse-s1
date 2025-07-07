-- Device Registry Database Schema
-- Based on ER Diagram for Device Context

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create device_types table
CREATE TABLE IF NOT EXISTS device_types (
    type_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    type_name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    manufacturer VARCHAR(100),
    model VARCHAR(100),
    protocol VARCHAR(30),
    capabilities JSONB DEFAULT '{}',
    default_config JSONB DEFAULT '{}',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_device_type_name_model UNIQUE (type_name, model)
);

-- Create devices table
CREATE TABLE IF NOT EXISTS devices (
    device_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    type_id UUID NOT NULL,
    house_id UUID NOT NULL,
    location_id UUID NOT NULL,
    registered_by UUID NOT NULL,
    device_name VARCHAR(100) NOT NULL,
    serial_number VARCHAR(100) NOT NULL UNIQUE,
    mac_address VARCHAR(17),
    ip_address INET,
    firmware_version VARCHAR(50),
    configuration JSONB DEFAULT '{}',
    installation_date DATE,
    warranty_expires DATE,
    is_online BOOLEAN DEFAULT false,
    last_seen TIMESTAMP,
    legacy_sensor_id INTEGER, -- For mapping from existing sensors
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_device_type FOREIGN KEY (type_id) REFERENCES device_types(type_id)
);

-- Create indexes for performance
CREATE INDEX idx_devices_house_id ON devices(house_id);
CREATE INDEX idx_devices_location_id ON devices(location_id);
CREATE INDEX idx_devices_registered_by ON devices(registered_by);
CREATE INDEX idx_devices_type_id ON devices(type_id);
CREATE INDEX idx_devices_is_online ON devices(is_online);
CREATE INDEX idx_devices_legacy_sensor_id ON devices(legacy_sensor_id);

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_device_types_updated_at BEFORE UPDATE
    ON device_types FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_devices_updated_at BEFORE UPDATE
    ON devices FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert default device types
INSERT INTO device_types (type_name, category, manufacturer, model, protocol, capabilities, default_config) VALUES
    ('Temperature Sensor', 'sensor', 'SmartHome Inc', 'TH-100', 'MQTT', 
     '{"measurement_types": ["temperature", "humidity"], "polling_interval": 60}', 
     '{"unit": "celsius", "precision": 0.1}'),
    
    ('Smart Light', 'actuator', 'SmartHome Inc', 'SL-200', 'MQTT', 
     '{"control_types": ["on_off", "brightness", "color"], "max_brightness": 100}', 
     '{"default_brightness": 50, "default_color": "white"}'),
    
    ('Smart Lock', 'actuator', 'SecureHome', 'SL-300', 'MQTT', 
     '{"control_types": ["lock", "unlock"], "has_keypad": true}', 
     '{"auto_lock_timeout": 300}'),
    
    ('Motion Sensor', 'sensor', 'SmartHome Inc', 'MS-100', 'MQTT', 
     '{"measurement_types": ["motion", "presence"], "detection_range": 5}', 
     '{"sensitivity": "medium"}'),
    
    ('Smart Thermostat', 'actuator', 'WarmHouse', 'WT-500', 'MQTT', 
     '{"control_types": ["temperature_set", "mode"], "modes": ["heat", "cool", "auto"]}', 
     '{"default_mode": "auto", "default_temp": 22}'),
    
    ('Security Camera', 'sensor', 'SecureView', 'SC-400', 'HTTP', 
     '{"features": ["video", "motion_detection", "night_vision"], "resolution": "1080p"}', 
     '{"recording_enabled": false, "motion_alerts": true}')
ON CONFLICT (type_name, model) DO NOTHING;

-- Grant permissions to device_registry user
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO device_registry;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO device_registry; 