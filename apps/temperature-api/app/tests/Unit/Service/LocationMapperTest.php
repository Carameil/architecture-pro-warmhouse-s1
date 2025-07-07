<?php

declare(strict_types=1);

namespace App\Tests\Unit\Service;

use App\Service\LocationMapper;
use PHPUnit\Framework\TestCase;

class LocationMapperTest extends TestCase
{
    private LocationMapper $locationMapper;

    protected function setUp(): void
    {
        $this->locationMapper = new LocationMapper();
    }

    public function testGetSensorIdByKnownLocation(): void
    {
        $this->assertSame('1', $this->locationMapper->getSensorIdByLocation('Living Room'));
        $this->assertSame('2', $this->locationMapper->getSensorIdByLocation('Bedroom'));
        $this->assertSame('3', $this->locationMapper->getSensorIdByLocation('Kitchen'));
    }

    public function testGetSensorIdByUnknownLocation(): void
    {
        $this->assertSame('0', $this->locationMapper->getSensorIdByLocation('Unknown Room'));
        $this->assertSame('0', $this->locationMapper->getSensorIdByLocation(''));
    }

    public function testGetSensorIdWithWhitespace(): void
    {
        $this->assertSame('1', $this->locationMapper->getSensorIdByLocation('  Living Room  '));
        $this->assertSame('2', $this->locationMapper->getSensorIdByLocation(' Bedroom '));
    }

    public function testGetLocationByKnownSensorId(): void
    {
        $this->assertSame('Living Room', $this->locationMapper->getLocationBySensorId('1'));
        $this->assertSame('Bedroom', $this->locationMapper->getLocationBySensorId('2'));
        $this->assertSame('Kitchen', $this->locationMapper->getLocationBySensorId('3'));
    }

    public function testGetLocationByUnknownSensorId(): void
    {
        $this->assertSame('Unknown Location', $this->locationMapper->getLocationBySensorId('999'));
        $this->assertSame('Unknown Location', $this->locationMapper->getLocationBySensorId('0'));
        $this->assertSame('Unknown Location', $this->locationMapper->getLocationBySensorId(''));
    }
}
