package com.example.probationbackend.repository

import com.example.probationbackend.model.FaceCheckEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface FaceCheckEventRepository : JpaRepository<FaceCheckEvent, Long> {
    fun findByUserId(userId: Long): List<FaceCheckEvent>
    fun findByDeviceId(deviceId: Long): List<FaceCheckEvent>
    fun findByCheckId(checkId: String): Optional<FaceCheckEvent>
    // Примеры для фильтрации по времени, результату и т.д.
    // fun findByTakenAtBetweenAndOutcome(from: LocalDateTime, to: LocalDateTime, outcome: String): List<FaceCheckEvent>
}