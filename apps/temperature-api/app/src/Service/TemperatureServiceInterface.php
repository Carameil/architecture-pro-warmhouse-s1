<?php

declare(strict_types=1);

namespace App\Service;

use App\DTO\TemperatureDataDto;

interface TemperatureServiceInterface
{
    public function getTemperatureByLocation(string $location): TemperatureDataDto;

    public function getTemperatureBySensorId(string $sensorId): TemperatureDataDto;
}
