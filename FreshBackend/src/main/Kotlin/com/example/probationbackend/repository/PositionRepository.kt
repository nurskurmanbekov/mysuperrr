package com.example.probationbackend.repository

import com.example.probationbackend.model.Client
import com.example.probationbackend.model.Position
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface PositionRepository : JpaRepository<Position, Long> {

    fun findByClientAndTimestampBetweenOrderByTimestampAsc(
        client: Client,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): List<Position>

    fun findByClientOrderByTimestampDesc(client: Client): List<Position>

    @Query("SELECT p FROM Position p WHERE p.client.id = :clientId AND p.timestamp >= :startTime AND p.timestamp <= :endTime ORDER BY p.timestamp ASC")
    fun findTrack(clientId: Long, startTime: LocalDateTime, endTime: LocalDateTime): List<Position>
}
