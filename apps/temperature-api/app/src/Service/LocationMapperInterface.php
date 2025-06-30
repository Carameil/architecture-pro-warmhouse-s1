<?php

declare(strict_types=1);

namespace App\Service;

interface LocationMapperInterface
{
    public function getSensorIdByLocation(string $location): string;

    public function getLocationBySensorId(string $sensorId): string;
}
