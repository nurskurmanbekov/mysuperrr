package com.example.probationbackend.controller

import com.example.probationbackend.repository.ClientRepository
import com.example.probationbackend.repository.DevicePositionRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

/**
 * API для получения GPS позиций устройств
 * Frontend получает данные ТОЛЬКО от Backend, НЕ напрямую из Traccar!
 *
 * Endpoints:
 * GET /api/positions/latest - последние позиции всех устройств
 * GET /api/positions/{uniqueId}/latest - последняя позиция конкретного устройства
 * GET /api/positions/{uniqueId}/history - история позиций устройства
 * GET /api/positions/track/{clientId} - трек для воспроизведения (TrackPlayback)
 */
@RestController
@RequestMapping("/api/positions")
class PositionController(
    private val devicePositionRepository: DevicePositionRepository,
    private val clientRepository: ClientRepository
) {

    /**
     * Получить последние позиции ВСЕХ устройств
     * Используется для отображения всех устройств на карте
     *
     * GET /api/positions/latest
     *
     * Response:
     * {
     *   "status": "success",
     *   "count": 5,
     *   "positions": [
     *     {
     *       "uniqueId": "1234567890123",
     *       "latitude": 42.88,
     *       "longitude": 74.68,
     *       "speed": 0.0,
     *       "timestamp": "2025-11-28T12:00:00",
     *       "status": "online",
     *       "battery": 85.0
     *     },
     *     ...
     *   ]
     * }
     */
    @GetMapping("/latest")
    fun getLatestPositions(): ResponseEntity<*> {
        val positions = devicePositionRepository.findLatestPositionsForAllDevices()

        val positionsData = positions.map { pos ->
            mapOf(
                "id" to pos.id,
                "deviceId" to pos.deviceId,
                "uniqueId" to pos.uniqueId,
                "latitude" to pos.latitude,
                "longitude" to pos.longitude,
                "speed" to pos.speed,
                "bearing" to pos.bearing,
                "altitude" to pos.altitude,
                "accuracy" to pos.accuracy,
                "battery" to pos.battery,
                "timestamp" to pos.timestamp.toString(),
                "serverTime" to pos.serverTime.toString(),
                "status" to if (pos.isOnline()) "online" else "offline",
                "sentToTraccar" to pos.sentToTraccar
            )
        }

        return ResponseEntity.ok(mapOf(
            "status" to "success",
            "count" to positions.size,
            "positions" to positionsData
        ))
    }

    /**
     * Получить последнюю позицию конкретного устройства
     *
     * GET /api/positions/{uniqueId}/latest
     *
     * Response:
     * {
     *   "status": "success",
     *   "position": {
     *     "uniqueId": "1234567890123",
     *     "latitude": 42.88,
     *     "longitude": 74.68,
     *     ...
     *   }
     * }
     */
    @GetMapping("/{uniqueId}/latest")
    fun getLatestPosition(@PathVariable uniqueId: String): ResponseEntity<*> {
        val position = devicePositionRepository.findTopByUniqueIdOrderByTimestampDesc(uniqueId)

        if (position.isEmpty) {
            return ResponseEntity.ok(mapOf(
                "status" to "not_found",
                "message" to "No position data for device: $uniqueId"
            ))
        }

        val pos = position.get()

        return ResponseEntity.ok(mapOf(
            "status" to "success",
            "position" to mapOf(
                "id" to pos.id,
                "deviceId" to pos.deviceId,
                "uniqueId" to pos.uniqueId,
                "latitude" to pos.latitude,
                "longitude" to pos.longitude,
                "speed" to pos.speed,
                "bearing" to pos.bearing,
                "altitude" to pos.altitude,
                "accuracy" to pos.accuracy,
                "battery" to pos.battery,
                "timestamp" to pos.timestamp.toString(),
                "serverTime" to pos.serverTime.toString(),
                "status" to if (pos.isOnline()) "online" else "offline",
                "sentToTraccar" to pos.sentToTraccar
            )
        ))
    }

    /**
     * Получить историю позиций устройства за период
     *
     * GET /api/positions/{uniqueId}/history?from=2025-11-28T00:00:00&to=2025-11-28T23:59:59
     *
     * Response:
     * {
     *   "status": "success",
     *   "count": 120,
     *   "positions": [...]
     * }
     */
    @GetMapping("/{uniqueId}/history")
    fun getPositionHistory(
        @PathVariable uniqueId: String,
        @RequestParam(required = false) from: String?,
        @RequestParam(required = false) to: String?
    ): ResponseEntity<*> {
        val startTime = from?.let { LocalDateTime.parse(it) } ?: LocalDateTime.now().minusDays(1)
        val endTime = to?.let { LocalDateTime.parse(it) } ?: LocalDateTime.now()

        val positions = devicePositionRepository.findByUniqueIdAndTimestampBetweenOrderByTimestampDesc(
            uniqueId, startTime, endTime
        )

        val positionsData = positions.map { pos ->
            mapOf(
                "latitude" to pos.latitude,
                "longitude" to pos.longitude,
                "speed" to pos.speed,
                "timestamp" to pos.timestamp.toString()
            )
        }

        return ResponseEntity.ok(mapOf(
            "status" to "success",
            "count" to positions.size,
            "from" to startTime.toString(),
            "to" to endTime.toString(),
            "positions" to positionsData
        ))
    }

    /**
     * Получить онлайн устройства (последняя позиция не старше 5 минут)
     *
     * GET /api/positions/online
     */
    @GetMapping("/online")
    fun getOnlineDevices(): ResponseEntity<*> {
        val fiveMinutesAgo = LocalDateTime.now().minusMinutes(5)
        val onlinePositions = devicePositionRepository.findOnlineDevices(fiveMinutesAgo)

        val positionsData = onlinePositions.map { pos ->
            mapOf(
                "uniqueId" to pos.uniqueId,
                "latitude" to pos.latitude,
                "longitude" to pos.longitude,
                "timestamp" to pos.timestamp.toString(),
                "status" to "online"
            )
        }

        return ResponseEntity.ok(mapOf(
            "status" to "success",
            "count" to onlinePositions.size,
            "devices" to positionsData
        ))
    }

    /**
     * Получить трек для воспроизведения (используется в TrackPlayback)
     * Принимает clientId (число) и возвращает массив позиций для анимации
     *
     * GET /api/positions/track/{clientId}?startTime=2025-11-28T07:26&endTime=2025-11-29T07:26
     *
     * Response: Array<Position>
     * [
     *   {
     *     "id": 1,
     *     "latitude": 42.88,
     *     "longitude": 74.68,
     *     "altitude": 800.0,
     *     "speed": 5.5,
     *     "accuracy": 10.0,
     *     "timestamp": "2025-11-28T12:00:00"
     *   },
     *   ...
     * ]
     */
    @GetMapping("/track/{clientId}")
    fun getTrack(
        @PathVariable clientId: Long,
        @RequestParam(required = true) startTime: String,
        @RequestParam(required = true) endTime: String
    ): ResponseEntity<*> {
        try {
            // 1. Найти клиента по ID
            val client = clientRepository.findById(clientId).orElse(null)
                ?: return ResponseEntity.status(404).body(
                    mapOf("error" to "Client not found with id: $clientId")
                )

            // 2. Получить uniqueId клиента
            val uniqueId = client.uniqueId
                ?: return ResponseEntity.status(404).body(
                    mapOf("error" to "Client $clientId has no uniqueId (GPS tracking not enabled)")
                )

            // 3. Парсинг дат (формат: 2025-11-28T07:26)
            val startDateTime = LocalDateTime.parse(startTime)
            val endDateTime = LocalDateTime.parse(endTime)

            // 4. Получить позиции из базы данных
            val positions = devicePositionRepository.findByUniqueIdAndTimestampBetweenOrderByTimestampDesc(
                uniqueId, startDateTime, endDateTime
            )

            // 5. Преобразовать в формат для TrackPlayback (отсортировать по времени ASC)
            val trackData = positions.reversed().map { pos ->
                mapOf(
                    "id" to pos.id,
                    "latitude" to pos.latitude,
                    "longitude" to pos.longitude,
                    "altitude" to pos.altitude,
                    "speed" to pos.speed,
                    "accuracy" to pos.accuracy,
                    "timestamp" to pos.timestamp.toString()
                )
            }

            return ResponseEntity.ok(trackData)

        } catch (e: Exception) {
            e.printStackTrace()
            return ResponseEntity.status(500).body(
                mapOf("error" to "Failed to load track: ${e.message}")
            )
        }
    }
}
