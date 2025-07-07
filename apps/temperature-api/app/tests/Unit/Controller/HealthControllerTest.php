<?php

declare(strict_types=1);

namespace App\Tests\Unit\Controller;

use App\Controller\HealthController;
use PHPUnit\Framework\TestCase;
use Symfony\Component\HttpFoundation\JsonResponse;

class HealthControllerTest extends TestCase
{
    private HealthController $controller;

    protected function setUp(): void
    {
        $this->controller = new HealthController();
    }

    public function testHealthEndpoint(): void
    {
        $response = $this->controller->health();

        $this->assertInstanceOf(JsonResponse::class, $response);
        $this->assertSame(200, $response->getStatusCode());

        $data = json_decode($response->getContent(), true);

        $this->assertArrayHasKey('status', $data);
        $this->assertArrayHasKey('service', $data);
        $this->assertArrayHasKey('timestamp', $data);
        $this->assertArrayHasKey('version', $data);

        $this->assertSame('OK', $data['status']);
        $this->assertSame('temperature-api', $data['service']);
        $this->assertSame('1.0.0', $data['version']);

        $this->assertMatchesRegularExpression(
            '/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/',
            $data['timestamp']
        );
    }

    public function testHealthResponseStructure(): void
    {
        $response = $this->controller->health();
        $data = json_decode($response->getContent(), true);

        $expectedKeys = ['status', 'service', 'timestamp', 'version'];
        $this->assertSame($expectedKeys, array_keys($data));
    }

    public function testHealthResponseContentType(): void
    {
        $response = $this->controller->health();

        $this->assertTrue($response->headers->contains('Content-Type', 'application/json'));
    }
}
