package com.example.probationbackend.controller

import com.example.probationbackend.repository.ClientRepository
import com.example.probationbackend.service.GeoZoneService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/traccar")
class TraccarProxyController(
    private val geoZoneService: GeoZoneService,
    private val clientRepository: ClientRepository
) {

    // Простой метод для теста
    @GetMapping("/devices")
    fun getDevices(): ResponseEntity<*> {
        return ResponseEntity.ok(mapOf(
            "status" to "success",
            "message" to "Nginx + Spring Boot работают!",
            "devices" to listOf(
                mapOf("id" to 1, "name" to "Test Device 1"),
                mapOf("id" to 2, "name" to "Test Device 2")
            )
        ))
    }

    // Метод для приема GPS от мобильного приложения
    @PostMapping("/positions")
    fun receivePosition(@RequestBody positionData: Map<String, Any>): ResponseEntity<*> {
        println("Получены GPS данные: $positionData")

        // Извлекаем координаты
        val lat = positionData["lat"] as? Double
        val lon = positionData["lon"] as? Double
        val deviceId = positionData["id"] as? String

        // Проверяем геозоны если есть координаты
        if (lat != null && lon != null && deviceId != null) {
            try {
                // Находим клиента по uniqueId
                val client = clientRepository.findByUniqueId(deviceId).orElse(null)
                if (client != null) {
                    // Проверяем нарушения геозон
                    geoZoneService.checkGeoZoneViolations(client.id!!, lat, lon)
                }
            } catch (e: Exception) {
                println("Ошибка проверки геозон: ${e.message}")
            }
        }

        return ResponseEntity.ok(mapOf(
            "status" to "received",
            "deviceId" to positionData["deviceId"],
            "timestamp" to System.currentTimeMillis()
        ))
    }
}