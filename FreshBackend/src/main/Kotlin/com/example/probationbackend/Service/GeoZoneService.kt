package com.example.probationbackend.service

import com.example.probationbackend.model.Client
import com.example.probationbackend.model.GeoZone
import com.example.probationbackend.model.GeoZoneViolation
import com.example.probationbackend.repository.ClientRepository
import com.example.probationbackend.repository.GeoZoneRepository
import com.example.probationbackend.repository.GeoZoneViolationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.abs

@Service
@Transactional
class GeoZoneService(
    private val geoZoneRepository: GeoZoneRepository,
    private val geoZoneViolationRepository: GeoZoneViolationRepository,
    private val clientRepository: ClientRepository,
    private val fcmService: FcmService
) {

    fun createGeoZone(clientId: Long, name: String, polygonCoordinates: List<List<Double>>): GeoZone {
        val client = clientRepository.findById(clientId).orElseThrow {
            IllegalArgumentException("Client not found with id: $clientId")
        }

        val geoZone = GeoZone(
            name = name,
            client = client,
            polygonCoordinates = polygonCoordinates,
            isActive = true
        )

        return geoZoneRepository.save(geoZone)
    }

    fun updateGeoZone(geoZoneId: Long, name: String?, polygonCoordinates: List<List<Double>>?, isActive: Boolean?): GeoZone {
        val geoZone = geoZoneRepository.findById(geoZoneId).orElseThrow {
            IllegalArgumentException("GeoZone not found with id: $geoZoneId")
        }

        val updated = geoZone.copy(
            name = name ?: geoZone.name,
            polygonCoordinates = polygonCoordinates ?: geoZone.polygonCoordinates,
            isActive = isActive ?: geoZone.isActive
        )

        return geoZoneRepository.save(updated)
    }

    fun deleteGeoZone(geoZoneId: Long) {
        geoZoneRepository.deleteById(geoZoneId)
    }

    fun getGeoZonesByClient(clientId: Long): List<GeoZone> {
        val client = clientRepository.findById(clientId).orElseThrow {
            IllegalArgumentException("Client not found with id: $clientId")
        }
        return geoZoneRepository.findByClient(client)
    }

    fun getAllGeoZones(): List<GeoZone> {
        return geoZoneRepository.findAll()
    }

    /**
     * Проверяет нахождение GPS координат внутри всех активных геозон клиента
     * и создает нарушения при выходе за пределы
     */
    fun checkGeoZoneViolations(clientId: Long, latitude: Double, longitude: Double) {
        val client = clientRepository.findById(clientId).orElse(null) ?: return

        val activeGeoZones = geoZoneRepository.findByClientAndIsActive(client, true)

        for (geoZone in activeGeoZones) {
            val isInside = isPointInPolygon(latitude, longitude, geoZone.polygonCoordinates)

            if (!isInside) {
                // Создаем запись о нарушении
                val violation = GeoZoneViolation(
                    geoZone = geoZone,
                    client = client,
                    violationType = "EXIT",
                    latitude = latitude,
                    longitude = longitude,
                    notificationSent = false
                )
                geoZoneViolationRepository.save(violation)

                // Отправляем уведомление
                sendViolationNotification(client, geoZone, "EXIT")
            }
        }
    }

    /**
     * Проверяет, находится ли точка внутри полигона
     * Использует алгоритм Ray Casting
     */
    private fun isPointInPolygon(latitude: Double, longitude: Double, polygon: List<List<Double>>): Boolean {
        var inside = false
        var j = polygon.size - 1

        for (i in polygon.indices) {
            val xi = polygon[i][1] // longitude
            val yi = polygon[i][0] // latitude
            val xj = polygon[j][1]
            val yj = polygon[j][0]

            val intersect = ((yi > latitude) != (yj > latitude)) &&
                    (longitude < (xj - xi) * (latitude - yi) / (yj - yi) + xi)

            if (intersect) {
                inside = !inside
            }
            j = i
        }

        return inside
    }

    /**
     * Отправка уведомления о нарушении геозоны
     */
    private fun sendViolationNotification(client: Client, geoZone: GeoZone, violationType: String) {
        // Находим всех сотрудников района клиента для отправки уведомлений
        // Это будет реализовано в AdminService

        val title = "Нарушение геозоны"
        val body = "Осужденный ${client.fio} покинул разрешенную зону: ${geoZone.name}"

        // TODO: Получить FCM токены сотрудников района и отправить уведомления
        // Пока логируем
        println("VIOLATION: $title - $body")
    }

    fun getViolationsByClient(clientId: Long): List<GeoZoneViolation> {
        val client = clientRepository.findById(clientId).orElseThrow {
            IllegalArgumentException("Client not found with id: $clientId")
        }
        return geoZoneViolationRepository.findByClient(client)
    }

    fun getAllViolations(): List<GeoZoneViolation> {
        return geoZoneViolationRepository.findAll()
    }
}
