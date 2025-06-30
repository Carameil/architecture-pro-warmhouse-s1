<?php

declare(strict_types=1);

namespace App\Service;

class RandomTemperatureGenerator implements TemperatureGeneratorInterface
{
    private const float MIN_TEMPERATURE = 18.0;
    private const float MAX_TEMPERATURE = 25.0;

    public function generateTemperature(): float
    {
        $range = self::MAX_TEMPERATURE - self::MIN_TEMPERATURE;
        $randomValue = mt_rand() / mt_getrandmax();

        return round(self::MIN_TEMPERATURE + ($randomValue * $range), 1);
    }
}
