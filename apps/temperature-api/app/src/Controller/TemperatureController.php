<?php

declare(strict_types=1);

namespace App\Controller;

use App\Service\TemperatureServiceInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Annotation\Route;

class TemperatureController extends AbstractController
{
    public function __construct(
        private readonly TemperatureServiceInterface $temperatureService
    ) {
    }

    #[Route('/temperature', name: 'temperature', methods: ['GET'])]
    public function temperature(Request $request): JsonResponse
    {
        $location = $request->query->get('location', '');
        $temperatureData = $this->temperatureService->getTemperatureByLocation($location);

        return new JsonResponse([
            'location' => $temperatureData->location,
            'sensorId' => $temperatureData->sensorId,
            'temperature' => $temperatureData->value,
            'unit' => $temperatureData->unit,
            'timestamp' => date('Y-m-d H:i:s', strtotime($temperatureData->timestamp))
        ]);
    }

    #[Route('/temperature/{sensorId}', name: 'temperature_by_id', methods: ['GET'])]
    public function temperatureById(string $sensorId): JsonResponse
    {
        $temperatureData = $this->temperatureService->getTemperatureBySensorId($sensorId);

        return new JsonResponse($temperatureData->toArray());
    }
}
