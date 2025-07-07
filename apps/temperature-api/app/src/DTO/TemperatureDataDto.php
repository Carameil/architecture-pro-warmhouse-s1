<?php

declare(strict_types=1);

namespace App\DTO;

readonly class TemperatureDataDto
{
    public function __construct(
        public float $value,
        public string $unit,
        public string $timestamp,
        public string $location,
        public string $status,
        public string $sensorId,
        public string $sensorType,
        public string $description
    ) {
    }

    public function toArray(): array
    {
        return [
            'value' => $this->value,
            'unit' => $this->unit,
            'timestamp' => $this->timestamp,
            'location' => $this->location,
            'status' => $this->status,
            'sensor_id' => $this->sensorId,
            'sensor_type' => $this->sensorType,
            'description' => $this->description,
        ];
    }
}
