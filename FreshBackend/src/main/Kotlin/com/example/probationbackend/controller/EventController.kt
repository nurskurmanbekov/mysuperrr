// src/main/kotlin/com/example/probationbackend/controller/EventController.kt

package com.example.probationbackend.controller

import com.example.probationbackend.model.FaceCheckEvent
import com.example.probationbackend.repository.FaceCheckEventRepository
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/events")
class EventController(
    private val faceCheckEventRepository: FaceCheckEventRepository
) {

    @GetMapping
    fun getEvents(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: java.time.LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: java.time.LocalDateTime?,
        @RequestParam(required = false) deviceId: Long?
    ): ResponseEntity<*> {
        // Простой пример фильтрации. В реальности может быть сложнее (RBAC, разные типы событий).
        val events = if (from != null && to != null) {
            faceCheckEventRepository.findAll().filter { event ->
                val eventTime = event.takenAt
                eventTime.isAfter(from) && eventTime.isBefore(to) &&
                        (deviceId == null || event.deviceId == deviceId)
            }
        } else {
            faceCheckEventRepository.findAll().filter { deviceId == null || it.deviceId == deviceId }
        }
        // Конвертируем в формат, ожидаемый фронтендом (см. `faceEventsConv` в фронтенде)
        val convertedEvents = events.map { e ->
            mapOf(
                "id" to (e.checkId ?: e.id),
                "type" to if (e.outcome == "ok") "faceOk" else "faceIdFail",
                "eventTime" to e.takenAt.toString(), // ISO строка
                "deviceId" to e.deviceId,
                "attributes" to mapOf(
                    "message" to if (e.outcome == "ok") "FaceID пройден" else "FaceID не пройден",
                    "distance" to e.distance
                ),
                "userId" to e.userId // или получать из User по userId
            )
        }
        return ResponseEntity.ok(convertedEvents)
    }
}