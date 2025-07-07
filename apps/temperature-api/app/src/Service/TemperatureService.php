<?php

declare(strict_types=1);

namespace App\Service;

use App\DTO\TemperatureDataDto;

class TemperatureService implements TemperatureServiceInterface
{
    public function __construct(
        private readonly LocationMapperInterface $locationMapper,
        private readonly TemperatureGeneratorInterface $temperatureGenerator
    ) {
    }

    public function getTemperatureByLocation(string $location): TemperatureDataDto
    {
        $sensorId = $this->locationMapper->getSensorIdByLocation($location);
        $actualLocation = $location ?: 'Unknown';

        return $this->createTemperatureData($sensorId, $actualLocation);
    }

    public function getTemperatureBySensorId(string $sensorId): TemperatureDataDto
    {
        $location = $this->locationMapper->getLocationBySensorId($sensorId);

        return $this->createTemperatureData($sensorId, $location);
    }

    private function createTemperatureData(string $sensorId, string $location): TemperatureDataDto
    {
        $temperature = $this->temperatureGenerator->generateTemperature();

        return new TemperatureDataDto(
            value: $temperature,
            unit: 'celsius',
            timestamp: date('c'),
            location: $location,
            status: 'active',
            sensorId: $sensorId,
            sensorType: 'temperature',
            description: "Temperature reading from {$location}"
        );
    }
}
