package com.example.probationbackend.controller

import com.example.probationbackend.model.Position
import com.example.probationbackend.repository.ClientRepository
import com.example.probationbackend.repository.PositionRepository
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/positions")
class PositionController(
    private val positionRepository: PositionRepository,
    private val clientRepository: ClientRepository
) {

    @GetMapping("/track/{clientId}")
    fun getClientTrack(
        @PathVariable clientId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startTime: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endTime: LocalDateTime
    ): ResponseEntity<List<PositionDTO>> {
        val positions = positionRepository.findTrack(clientId, startTime, endTime)
        val dtos = positions.map { position ->
            PositionDTO(
                id = position.id!!,
                latitude = position.latitude,
                longitude = position.longitude,
                altitude = position.altitude,
                speed = position.speed,
                accuracy = position.accuracy,
                timestamp = position.timestamp.toString()
            )
        }
        return ResponseEntity.ok(dtos)
    }

    @PostMapping
    fun savePosition(@RequestBody request: SavePositionRequest): ResponseEntity<*> {
        val client = clientRepository.findById(request.clientId).orElse(null)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "Client not found"))

        val position = Position(
            client = client,
            latitude = request.latitude,
            longitude = request.longitude,
            altitude = request.altitude,
            speed = request.speed,
            accuracy = request.accuracy,
            timestamp = if (request.timestamp != null) LocalDateTime.parse(request.timestamp) else LocalDateTime.now()
        )

        val saved = positionRepository.save(position)
        return ResponseEntity.ok(mapOf("id" to saved.id, "status" to "saved"))
    }
}

data class PositionDTO(
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val speed: Double?,
    val accuracy: Double?,
    val timestamp: String
)

data class SavePositionRequest(
    val clientId: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val speed: Double? = null,
    val accuracy: Double? = null,
    val timestamp: String? = null
)
