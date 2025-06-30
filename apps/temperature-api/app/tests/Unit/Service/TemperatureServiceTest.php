<?php

declare(strict_types=1);

namespace App\Tests\Unit\Service;

use App\DTO\TemperatureDataDto;
use App\Service\LocationMapperInterface;
use App\Service\TemperatureGeneratorInterface;
use App\Service\TemperatureService;
use PHPUnit\Framework\MockObject\Exception;
use PHPUnit\Framework\MockObject\MockObject;
use PHPUnit\Framework\TestCase;

class TemperatureServiceTest extends TestCase
{
    private TemperatureService $temperatureService;
    private LocationMapperInterface|MockObject $locationMapper;
    private TemperatureGeneratorInterface|MockObject $temperatureGenerator;

    /**
     * @throws Exception
     */
    protected function setUp(): void
    {
        $this->locationMapper = $this->createMock(LocationMapperInterface::class);
        $this->temperatureGenerator = $this->createMock(TemperatureGeneratorInterface::class);

        $this->temperatureService = new TemperatureService(
            $this->locationMapper,
            $this->temperatureGenerator
        );
    }

    public function testGetTemperatureByLocation(): void
    {
        $location = 'Living Room';
        $expectedSensorId = '1';
        $expectedTemperature = 22.5;

        $this->locationMapper
            ->expects($this->once())
            ->method('getSensorIdByLocation')
            ->with($location)
            ->willReturn($expectedSensorId);

        $this->temperatureGenerator
            ->expects($this->once())
            ->method('generateTemperature')
            ->willReturn($expectedTemperature);

        $result = $this->temperatureService->getTemperatureByLocation($location);

        $this->assertInstanceOf(TemperatureDataDto::class, $result);
        $this->assertSame($expectedTemperature, $result->value);
        $this->assertSame('celsius', $result->unit);
        $this->assertSame($location, $result->location);
        $this->assertSame('active', $result->status);
        $this->assertSame($expectedSensorId, $result->sensorId);
        $this->assertSame('temperature', $result->sensorType);
        $this->assertSame("Temperature reading from {$location}", $result->description);
    }

    public function testGetTemperatureByLocationWithEmptyLocation(): void
    {
        $location = '';
        $expectedSensorId = '0';
        $expectedTemperature = 19.0;

        $this->locationMapper
            ->expects($this->once())
            ->method('getSensorIdByLocation')
            ->with($location)
            ->willReturn($expectedSensorId);

        $this->temperatureGenerator
            ->expects($this->once())
            ->method('generateTemperature')
            ->willReturn($expectedTemperature);

        $result = $this->temperatureService->getTemperatureByLocation($location);

        $this->assertSame('Unknown', $result->location);
        $this->assertSame($expectedSensorId, $result->sensorId);
    }

    public function testGetTemperatureBySensorId(): void
    {
        $sensorId = '2';
        $expectedLocation = 'Bedroom';
        $expectedTemperature = 24.1;

        $this->locationMapper
            ->expects($this->once())
            ->method('getLocationBySensorId')
            ->with($sensorId)
            ->willReturn($expectedLocation);

        $this->temperatureGenerator
            ->expects($this->once())
            ->method('generateTemperature')
            ->willReturn($expectedTemperature);

        $result = $this->temperatureService->getTemperatureBySensorId($sensorId);

        $this->assertInstanceOf(TemperatureDataDto::class, $result);
        $this->assertSame($expectedTemperature, $result->value);
        $this->assertSame($expectedLocation, $result->location);
        $this->assertSame($sensorId, $result->sensorId);
        $this->assertSame("Temperature reading from {$expectedLocation}", $result->description);
    }
}
