package com.example.probationbackend.controller

import com.example.probationbackend.model.DevicePosition
import com.example.probationbackend.repository.ClientRepository
import com.example.probationbackend.repository.DevicePositionRepository
import com.example.probationbackend.service.GeoZoneService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

@RestController
@RequestMapping("/api/traccar")
class TraccarProxyController(
    private val geoZoneService: GeoZoneService,
    private val clientRepository: ClientRepository,
    private val devicePositionRepository: DevicePositionRepository
) {

    @Value("\${traccar.base-url}")
    private lateinit var traccarBaseUrl: String

    @Value("\${traccar.api.username}")
    private lateinit var traccarUsername: String

    @Value("\${traccar.api.password}")
    private lateinit var traccarPassword: String

    private val restTemplate = RestTemplate()

    /**
     * Получить список устройств (для совместимости с фронтом)
     * Теперь возвращаем данные из нашей БД, а не из Traccar
     */
    @GetMapping("/devices")
    fun getDevices(): ResponseEntity<*> {
        return ResponseEntity.ok(mapOf(
            "status" to "success",
            "message" to "Backend GPS Proxy работает",
            "devices" to listOf(
                mapOf("id" to 1, "name" to "Test Device 1"),
                mapOf("id" to 2, "name" to "Test Device 2")
            )
        ))
    }

    /**
     * ГЛАВНЫЙ ENDPOINT - Прием GPS от мобильного приложения
     *
     * Архитектура:
     * 1. Получаем GPS от мобильного приложения
     * 2. СОХРАНЯЕМ В СВОЮ БД (device_positions) - это главное!
     * 3. Опционально отправляем в Traccar (для визуализации)
     * 4. Проверяем геозоны
     * 5. Возвращаем успех мобильному приложению
     */
    @PostMapping("/positions")
    fun receivePosition(@RequestBody positionData: Map<String, Any>): ResponseEntity<*> {
        println("📍 Получены GPS данные: $positionData")

        // Извлекаем координаты
        val lat = positionData["lat"] as? Double
        val lon = positionData["lon"] as? Double
        val deviceUniqueId = positionData["id"] as? String
        val timestamp = (positionData["timestamp"] as? Number)?.toLong()
        val speed = (positionData["speed"] as? Number)?.toDouble() ?: 0.0
        val bearing = (positionData["bearing"] as? Number)?.toDouble() ?: 0.0
        val altitude = (positionData["altitude"] as? Number)?.toDouble() ?: 0.0
        val accuracy = (positionData["accuracy"] as? Number)?.toDouble() ?: 0.0
        val battery = (positionData["batt"] as? Number)?.toDouble() ?: 85.0

        if (lat == null || lon == null || deviceUniqueId == null) {
            return ResponseEntity.badRequest().body(mapOf(
                "status" to "error",
                "error" to "Missing required fields: lat, lon, id"
            ))
        }

        try {
            // 1. Находим клиента в нашей системе
            val client = clientRepository.findByUniqueId(deviceUniqueId).orElse(null)
            if (client == null) {
                println("⚠️ Клиент с uniqueId=$deviceUniqueId не найден в БД")
                return ResponseEntity.badRequest().body(mapOf(
                    "status" to "error",
                    "error" to "Device not found in system. Please create client first."
                ))
            }

            // 2. СОХРАНЯЕМ ПОЗИЦИЮ В НАШУ БД (главное!)
            val positionTime = if (timestamp != null) {
                LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault())
            } else {
                LocalDateTime.now()
            }

            val devicePosition = DevicePosition(
                deviceId = client.id!!,
                uniqueId = deviceUniqueId,
                latitude = lat,
                longitude = lon,
                speed = speed,
                bearing = bearing,
                altitude = altitude,
                accuracy = accuracy,
                battery = battery,
                timestamp = positionTime,
                serverTime = LocalDateTime.now(),
                status = "online",
                sentToTraccar = false
            )

            val savedPosition = devicePositionRepository.save(devicePosition)
            println("✅ Позиция сохранена в БД: ID=${savedPosition.id}, uniqueId=$deviceUniqueId")

            // 3. Проверяем геозоны (если настроены)
            try {
                geoZoneService.checkGeoZoneViolations(client.id!!, lat, lon)
            } catch (e: Exception) {
                println("⚠️ Ошибка проверки геозон: ${e.message}")
                // Не критично - продолжаем
            }

            // 4. Опционально: отправляем в Traccar (для визуализации на карте)
            var sentToTraccar = false
            try {
                ensureDeviceExists(deviceUniqueId)
                sentToTraccar = sendPositionToTraccar(
                    deviceUniqueId, lat, lon, timestamp, speed, bearing, altitude, accuracy, battery
                )

                // Обновляем флаг в БД
                if (sentToTraccar) {
                    devicePositionRepository.save(savedPosition.copy(sentToTraccar = true))
                }
            } catch (e: Exception) {
                println("⚠️ Не удалось отправить в Traccar: ${e.message}")
                // Не критично - главное что сохранили в свою БД
            }

            // 5. Возвращаем успех
            return ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "Position saved to database",
                "deviceId" to deviceUniqueId,
                "positionId" to savedPosition.id,
                "sentToTraccar" to sentToTraccar,
                "timestamp" to System.currentTimeMillis()
            ))

        } catch (e: Exception) {
            println("❌ Ошибка обработки GPS: ${e.message}")
            e.printStackTrace()
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf(
                "status" to "error",
                "error" to e.message
            ))
        }
    }

    /**
     * Проверка/создание устройства в Traccar
     * Traccar используется ТОЛЬКО для визуализации, не как источник данных
     */
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
                println("📱 Устройство $uniqueId не найдено в Traccar, создаем...")

                // Ищем имя клиента в нашей БД
                val client = clientRepository.findByUniqueId(uniqueId).orElse(null)
                val deviceName = client?.fio ?: "Client_$uniqueId"

                // Создаем устройство в Traccar
                val deviceData = mapOf(
                    "name" to deviceName,
                    "uniqueId" to uniqueId,
                    "category" to "person"
                )

                val createResponse = restTemplate.exchange(
                    "$traccarBaseUrl/api/devices",
                    HttpMethod.POST,
                    HttpEntity(deviceData, headers),
                    Map::class.java
                )

                println("✅ Устройство создано в Traccar: ${createResponse.body}")
            } else {
                println("✅ Устройство $uniqueId уже существует в Traccar")
            }
        } catch (e: Exception) {
            println("⚠️ Ошибка работы с Traccar API: ${e.message}")
            println("   Это нормально если Traccar не запущен - данные сохранены в нашу БД")
            // Не бросаем исключение - продолжаем работу
        }
    }

    /**
     * Отправка позиции в Traccar через OsmAnd протокол
     */
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

    /**
     * Создание заголовков с Basic Auth для Traccar API
     */
    private fun createAuthHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.setBasicAuth(traccarUsername, traccarPassword)
        return headers
    }
}
