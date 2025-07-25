<?php

declare(strict_types=1);

namespace App\Service;

interface TemperatureGeneratorInterface
{
    public function generateTemperature(): float;
}
