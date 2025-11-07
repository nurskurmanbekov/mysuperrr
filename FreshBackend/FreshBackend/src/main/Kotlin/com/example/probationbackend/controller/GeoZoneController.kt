package com.example.probationbackend.controller

import com.example.probationbackend.service.GeoZoneService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/geozones")
class GeoZoneController(
    private val geoZoneService: GeoZoneService
) {

    @PostMapping
    fun createGeoZone(@RequestBody request: CreateGeoZoneRequest): ResponseEntity<*> {
        return try {
            val geoZone = geoZoneService.createGeoZone(
                clientId = request.clientId,
                name = request.name,
                polygonCoordinates = request.polygonCoordinates
            )
            ResponseEntity.ok(geoZone)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @PutMapping("/{id}")
    fun updateGeoZone(
        @PathVariable id: Long,
        @RequestBody request: UpdateGeoZoneRequest
    ): ResponseEntity<*> {
        return try {
            val geoZone = geoZoneService.updateGeoZone(
                geoZoneId = id,
                name = request.name,
                polygonCoordinates = request.polygonCoordinates,
                isActive = request.isActive
            )
            ResponseEntity.ok(geoZone)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @DeleteMapping("/{id}")
    fun deleteGeoZone(@PathVariable id: Long): ResponseEntity<*> {
        return try {
            geoZoneService.deleteGeoZone(id)
            ResponseEntity.ok(mapOf("message" to "GeoZone deleted"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @GetMapping("/client/{clientId}")
    fun getGeoZonesByClient(@PathVariable clientId: Long): ResponseEntity<*> {
        return try {
            val geoZones = geoZoneService.getGeoZonesByClient(clientId)
            ResponseEntity.ok(geoZones)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @GetMapping
    fun getAllGeoZones(): ResponseEntity<*> {
        val geoZones = geoZoneService.getAllGeoZones()
        return ResponseEntity.ok(geoZones)
    }

    @GetMapping("/violations")
    fun getAllViolations(): ResponseEntity<*> {
        val violations = geoZoneService.getAllViolations()
        return ResponseEntity.ok(violations)
    }

    @GetMapping("/violations/client/{clientId}")
    fun getViolationsByClient(@PathVariable clientId: Long): ResponseEntity<*> {
        return try {
            val violations = geoZoneService.getViolationsByClient(clientId)
            ResponseEntity.ok(violations)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }
}

data class CreateGeoZoneRequest(
    val clientId: Long,
    val name: String,
    val polygonCoordinates: List<List<Double>>
)

data class UpdateGeoZoneRequest(
    val name: String? = null,
    val polygonCoordinates: List<List<Double>>? = null,
    val isActive: Boolean? = null
)
