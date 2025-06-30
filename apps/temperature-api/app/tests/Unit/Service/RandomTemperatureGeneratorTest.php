<?php

declare(strict_types=1);

namespace App\Tests\Unit\Service;

use App\Service\RandomTemperatureGenerator;
use PHPUnit\Framework\TestCase;

class RandomTemperatureGeneratorTest extends TestCase
{
    private RandomTemperatureGenerator $generator;

    protected function setUp(): void
    {
        $this->generator = new RandomTemperatureGenerator();
    }

    public function testGenerateTemperatureInValidRange(): void
    {
        for ($i = 0; $i < 100; $i++) {
            $temperature = $this->generator->generateTemperature();

            $this->assertIsFloat($temperature);
            $this->assertGreaterThanOrEqual(18.0, $temperature);
            $this->assertLessThanOrEqual(25.0, $temperature);
        }
    }

    public function testGenerateTemperatureWithCorrectPrecision(): void
    {
        for ($i = 0; $i < 50; $i++) {
            $temperature = $this->generator->generateTemperature();

            $this->assertSame(round($temperature, 1), $temperature);

            $decimalPart = $temperature - floor($temperature);
            $this->assertLessThanOrEqual(0.1, abs($decimalPart - round($decimalPart, 1)));
        }
    }

    public function testGenerateTemperatureVariability(): void
    {
        $temperatures = [];

        for ($i = 0; $i < 50; $i++) {
            $temperatures[] = $this->generator->generateTemperature();
        }

        $uniqueTemperatures = array_unique($temperatures);
        $this->assertGreaterThan(1, count($uniqueTemperatures), 'Generated temperatures should vary');
    }

    public function testTemperatureReturnsFloat(): void
    {
        $temperature = $this->generator->generateTemperature();
        $this->assertIsFloat($temperature);
    }
}
