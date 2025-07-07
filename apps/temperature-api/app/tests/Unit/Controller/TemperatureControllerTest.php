<?php

declare(strict_types=1);

namespace App\Tests\Unit\Controller;

use App\Controller\TemperatureController;
use App\DTO\TemperatureDataDto;
use App\Service\TemperatureServiceInterface;
use PHPUnit\Framework\MockObject\Exception;
use PHPUnit\Framework\MockObject\MockObject;
use PHPUnit\Framework\TestCase;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;

class TemperatureControllerTest extends TestCase
{
    private TemperatureController $controller;
    private TemperatureServiceInterface|MockObject $temperatureService;

    /**
     * @throws Exception
     */
    protected function setUp(): void
    {
        $this->temperatureService = $this->createMock(TemperatureServiceInterface::class);
        $this->controller = new TemperatureController($this->temperatureService);
    }

    public function testTemperatureEndpointWithLocation(): void
    {
        $location = 'Living Room';
        $request = new Request(['location' => $location]);

        $temperatureDto = new TemperatureDataDto(
            value: 22.5,
            unit: 'celsius',
            timestamp: '2025-06-30T20:00:00+00:00',
            location: $location,
            status: 'active',
            sensorId: '1',
            sensorType: 'temperature',
            description: 'Temperature reading from Living Room'
        );

        $this->temperatureService
            ->expects($this->once())
            ->method('getTemperatureByLocation')
            ->with($location)
            ->willReturn($temperatureDto);

        $response = $this->controller->temperature($request);

        $this->assertInstanceOf(JsonResponse::class, $response);
        $this->assertSame(200, $response->getStatusCode());

        $data = json_decode($response->getContent(), true);
        $this->assertSame('Living Room', $data['location']);
        $this->assertSame('1', $data['sensorId']);
        $this->assertSame(22.5, $data['temperature']);
        $this->assertSame('celsius', $data['unit']);
        $this->assertArrayHasKey('timestamp', $data);
    }

    public function testTemperatureEndpointWithoutLocation(): void
    {
        $request = new Request();

        $temperatureDto = new TemperatureDataDto(
            value: 20.0,
            unit: 'celsius',
            timestamp: '2025-06-30T20:00:00+00:00',
            location: 'Unknown',
            status: 'active',
            sensorId: '0',
            sensorType: 'temperature',
            description: 'Temperature reading from Unknown'
        );

        $this->temperatureService
            ->expects($this->once())
            ->method('getTemperatureByLocation')
            ->with('')
            ->willReturn($temperatureDto);

        $response = $this->controller->temperature($request);

        $this->assertInstanceOf(JsonResponse::class, $response);
        $data = json_decode($response->getContent(), true);
        $this->assertSame('Unknown', $data['location']);
        $this->assertSame('0', $data['sensorId']);
    }

    public function testTemperatureByIdEndpoint(): void
    {
        $sensorId = '2';

        $temperatureDto = new TemperatureDataDto(
            value: 24.1,
            unit: 'celsius',
            timestamp: '2025-06-30T20:00:00+00:00',
            location: 'Bedroom',
            status: 'active',
            sensorId: $sensorId,
            sensorType: 'temperature',
            description: 'Temperature reading from Bedroom'
        );

        $this->temperatureService
            ->expects($this->once())
            ->method('getTemperatureBySensorId')
            ->with($sensorId)
            ->willReturn($temperatureDto);

        $response = $this->controller->temperatureById($sensorId);

        $this->assertInstanceOf(JsonResponse::class, $response);
        $this->assertSame(200, $response->getStatusCode());

        $data = json_decode($response->getContent(), true);

        $expectedData = [
            'value' => 24.1,
            'unit' => 'celsius',
            'timestamp' => '2025-06-30T20:00:00+00:00',
            'location' => 'Bedroom',
            'status' => 'active',
            'sensor_id' => '2',
            'sensor_type' => 'temperature',
            'description' => 'Temperature reading from Bedroom',
        ];

        $this->assertSame($expectedData, $data);
    }

    public function testLegacyFormatCompatibility(): void
    {
        $request = new Request(['location' => 'Kitchen']);

        $temperatureDto = new TemperatureDataDto(
            value: 23.0,
            unit: 'celsius',
            timestamp: '2025-06-30T20:00:00+00:00',
            location: 'Kitchen',
            status: 'active',
            sensorId: '3',
            sensorType: 'temperature',
            description: 'Temperature reading from Kitchen'
        );

        $this->temperatureService
            ->method('getTemperatureByLocation')
            ->willReturn($temperatureDto);

        $response = $this->controller->temperature($request);
        $data = json_decode($response->getContent(), true);

        $this->assertArrayHasKey('location', $data);
        $this->assertArrayHasKey('sensorId', $data);
        $this->assertArrayHasKey('temperature', $data);
        $this->assertArrayHasKey('unit', $data);
        $this->assertArrayHasKey('timestamp', $data);

        $this->assertMatchesRegularExpression('/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/', $data['timestamp']);
    }
}
