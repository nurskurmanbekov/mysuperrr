package com.example.probationbackend.repository

import com.example.probationbackend.model.Client
import com.example.probationbackend.model.GeoZone
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface GeoZoneRepository : JpaRepository<GeoZone, Long> {
    fun findByClient(client: Client): List<GeoZone>
    fun findByClientAndIsActive(client: Client, isActive: Boolean): List<GeoZone>
}
