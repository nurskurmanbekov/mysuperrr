package com.example.probationbackend.repository

import com.example.probationbackend.model.Client
import com.example.probationbackend.model.GeoZone
import com.example.probationbackend.model.GeoZoneViolation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface GeoZoneViolationRepository : JpaRepository<GeoZoneViolation, Long> {
    fun findByClient(client: Client): List<GeoZoneViolation>
    fun findByGeoZone(geoZone: GeoZone): List<GeoZoneViolation>
    fun findByCreatedAtAfter(createdAt: LocalDateTime): List<GeoZoneViolation>
    fun findByNotificationSent(sent: Boolean): List<GeoZoneViolation>
}
