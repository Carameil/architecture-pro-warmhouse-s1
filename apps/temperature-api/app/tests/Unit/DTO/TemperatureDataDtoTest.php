<?php

declare(strict_types=1);

namespace App\Tests\Unit\DTO;

use App\DTO\TemperatureDataDto;
use PHPUnit\Framework\TestCase;

class TemperatureDataDtoTest extends TestCase
{
    public function testCreateTemperatureDataDto(): void
    {
        $dto = new TemperatureDataDto(
            value: 23.5,
            unit: 'celsius',
            timestamp: '2025-06-30T20:00:00+00:00',
            location: 'Living Room',
            status: 'active',
            sensorId: '1',
            sensorType: 'temperature',
            description: 'Temperature reading from Living Room'
        );

        $this->assertSame(23.5, $dto->value);
        $this->assertSame('celsius', $dto->unit);
        $this->assertSame('2025-06-30T20:00:00+00:00', $dto->timestamp);
        $this->assertSame('Living Room', $dto->location);
        $this->assertSame('active', $dto->status);
        $this->assertSame('1', $dto->sensorId);
        $this->assertSame('temperature', $dto->sensorType);
        $this->assertSame('Temperature reading from Living Room', $dto->description);
    }

    public function testToArrayReturnsCorrectStructure(): void
    {
        $dto = new TemperatureDataDto(
            value: 21.0,
            unit: 'celsius',
            timestamp: '2025-06-30T20:00:00+00:00',
            location: 'Kitchen',
            status: 'active',
            sensorId: '3',
            sensorType: 'temperature',
            description: 'Temperature reading from Kitchen'
        );

        $expected = [
            'value' => 21.0,
            'unit' => 'celsius',
            'timestamp' => '2025-06-30T20:00:00+00:00',
            'location' => 'Kitchen',
            'status' => 'active',
            'sensor_id' => '3',
            'sensor_type' => 'temperature',
            'description' => 'Temperature reading from Kitchen',
        ];

        $this->assertSame($expected, $dto->toArray());
    }
}
