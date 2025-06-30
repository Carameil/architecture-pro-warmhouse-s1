<?php

declare(strict_types=1);

namespace App\Service;

class LocationMapper implements LocationMapperInterface
{
    private const array LOCATION_SENSOR_MAP = [
        'Living Room' => '1',
        'Bedroom' => '2',
        'Kitchen' => '3',
    ];

    private const array SENSOR_LOCATION_MAP = [
        '1' => 'Living Room',
        '2' => 'Bedroom',
        '3' => 'Kitchen',
    ];

    public function getSensorIdByLocation(string $location): string
    {
        return self::LOCATION_SENSOR_MAP[trim($location)] ?? '0';
    }

    public function getLocationBySensorId(string $sensorId): string
    {
        return self::SENSOR_LOCATION_MAP[$sensorId] ?? 'Unknown Location';
    }
}
