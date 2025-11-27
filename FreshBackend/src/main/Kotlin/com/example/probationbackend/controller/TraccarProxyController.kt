package com.example.probationbackend.controller

import com.example.probationbackend.repository.ClientRepository
import com.example.probationbackend.service.GeoZoneService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate
import java.util.*

@RestController
@RequestMapping("/api/traccar")
class TraccarProxyController(
    private val geoZoneService: GeoZoneService,
    private val clientRepository: ClientRepository
) {

    @Value("\${traccar.base-url}")
    private lateinit var traccarBaseUrl: String

    @Value("\${traccar.api.username}")
    private lateinit var traccarUsername: String

    @Value("\${traccar.api.password}")
    private lateinit var traccarPassword: String

    private val restTemplate = RestTemplate()

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
        println("📍 Получены GPS данные: $positionData")

        // Извлекаем координаты
        val lat = positionData["lat"] as? Double
        val lon = positionData["lon"] as? Double
        val deviceId = positionData["id"] as? String
        val timestamp = (positionData["timestamp"] as? Number)?.toLong()
        val speed = (positionData["speed"] as? Number)?.toDouble() ?: 0.0
        val bearing = (positionData["bearing"] as? Number)?.toDouble() ?: 0.0
        val altitude = (positionData["altitude"] as? Number)?.toDouble() ?: 0.0
        val accuracy = (positionData["accuracy"] as? Number)?.toDouble() ?: 0.0
        val battery = (positionData["batt"] as? Number)?.toDouble() ?: 85.0

        if (lat == null || lon == null || deviceId == null) {
            return ResponseEntity.badRequest().body(mapOf(
                "error" to "Missing required fields: lat, lon, id"
            ))
        }

        try {
            // 1. Проверяем/создаем устройство в Traccar
            ensureDeviceExists(deviceId)

            // 2. Отправляем позицию в Traccar через OsmAnd протокол
            val sent = sendPositionToTraccar(deviceId, lat, lon, timestamp, speed, bearing, altitude, accuracy, battery)

            // 3. Проверяем геозоны если есть координаты
            val client = clientRepository.findByUniqueId(deviceId).orElse(null)
            if (client != null) {
                try {
                    geoZoneService.checkGeoZoneViolations(client.id!!, lat, lon)
                } catch (e: Exception) {
                    println("⚠️ Ошибка проверки геозон: ${e.message}")
                }
            }

            return ResponseEntity.ok(mapOf(
                "status" to if (sent) "success" else "partial",
                "message" to if (sent) "Position sent to Traccar" else "Position received but not sent to Traccar",
                "deviceId" to deviceId,
                "timestamp" to System.currentTimeMillis()
            ))

        } catch (e: Exception) {
            println("❌ Ошибка обработки GPS: ${e.message}")
            e.printStackTrace()
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf(
                "error" to e.message,
                "status" to "error"
            ))
        }
    }

    // Проверка/создание устройства в Traccar
    private fun ensureDeviceExists(uniqueId: String) {
        try {
            val headers = createAuthHeaders()

            // Проверяем существует ли устройство
            val getUrl = "$traccarBaseUrl/api/devices?uniqueId=$uniqueId"
            val getResponse = restTemplate.exchange(
                getUrl,
                HttpMethod.GET,
                HttpEntity<String>(headers),
                object : org.springframework.core.ParameterizedTypeReference<List<Map<String, Any>>>() {}
            )

            val devices = getResponse.body

            if (devices == null || devices.isEmpty()) {
                println("📱 Устройство $uniqueId не найдено, создаем...")

                // Создаем устройство
                val deviceData = mapOf(
                    "name" to "Client_$uniqueId",
                    "uniqueId" to uniqueId,
                    "category" to "person"
                )

                val createResponse = restTemplate.exchange(
                    "$traccarBaseUrl/api/devices",
                    HttpMethod.POST,
                    HttpEntity(deviceData, headers),
                    Map::class.java
                )

                println("✅ Устройство создано: ${createResponse.body}")
            } else {
                println("✅ Устройство $uniqueId уже существует")
            }
        } catch (e: Exception) {
            println("⚠️ Ошибка проверки/создания устройства: ${e.message}")
            // Не бросаем исключение - продолжаем работу
        }
    }

    // Отправка позиции в Traccar через OsmAnd протокол
    private fun sendPositionToTraccar(
        deviceId: String,
        lat: Double,
        lon: Double,
        timestamp: Long?,
        speed: Double,
        bearing: Double,
        altitude: Double,
        accuracy: Double,
        battery: Double
    ): Boolean {
        try {
            // OsmAnd протокол обычно на порту 5055
            // Но через localhost можно использовать порт 8082 с параметром osmand
            val ts = timestamp ?: (System.currentTimeMillis() / 1000)

            val osmandUrl = "$traccarBaseUrl" +
                    "/?id=$deviceId" +
                    "&lat=$lat" +
                    "&lon=$lon" +
                    "&timestamp=$ts" +
                    "&speed=$speed" +
                    "&bearing=$bearing" +
                    "&altitude=$altitude" +
                    "&accuracy=$accuracy" +
                    "&batt=$battery"

            println("🚀 Отправка в Traccar OsmAnd: $osmandUrl")

            val response = restTemplate.getForEntity(osmandUrl, String::class.java)

            println("📡 Traccar ответ: ${response.statusCode} - ${response.body}")

            return response.statusCode == HttpStatus.OK
        } catch (e: Exception) {
            println("❌ Ошибка отправки в Traccar: ${e.message}")
            return false
        }
    }

    // Создание заголовков с Basic Auth
    private fun createAuthHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val auth = "$traccarUsername:$traccarPassword"
        val encodedAuth = Base64.getEncoder().encodeToString(auth.toByteArray())
        headers.set("Authorization", "Basic $encodedAuth")

        return headers
    }
}