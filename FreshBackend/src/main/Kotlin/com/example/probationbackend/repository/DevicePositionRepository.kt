package com.example.probationbackend.repository

import com.example.probationbackend.model.DevicePosition
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface DevicePositionRepository : JpaRepository<DevicePosition, Long> {

    /**
     * Получить последнюю позицию устройства по uniqueId
     */
    fun findTopByUniqueIdOrderByTimestampDesc(uniqueId: String): Optional<DevicePosition>

    /**
     * Получить последнюю позицию устройства по deviceId
     */
    fun findTopByDeviceIdOrderByTimestampDesc(deviceId: Long): Optional<DevicePosition>

    /**
     * Получить все позиции устройства за период
     */
    fun findByUniqueIdAndTimestampBetweenOrderByTimestampDesc(
        uniqueId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): List<DevicePosition>

    /**
     * Получить последние позиции всех устройств
     */
    @Query("""
        SELECT dp FROM DevicePosition dp
        WHERE dp.id IN (
            SELECT MAX(dp2.id) FROM DevicePosition dp2
            GROUP BY dp2.uniqueId
        )
        ORDER BY dp.timestamp DESC
    """)
    fun findLatestPositionsForAllDevices(): List<DevicePosition>

    /**
     * Получить все онлайн устройства (последняя позиция не старше 5 минут)
     */
    @Query("""
        SELECT dp FROM DevicePosition dp
        WHERE dp.id IN (
            SELECT MAX(dp2.id) FROM DevicePosition dp2
            GROUP BY dp2.uniqueId
        )
        AND dp.serverTime > :cutoffTime
        ORDER BY dp.timestamp DESC
    """)
    fun findOnlineDevices(cutoffTime: LocalDateTime): List<DevicePosition>

    /**
     * Удалить старые позиции (старше N дней)
     */
    fun deleteByTimestampBefore(timestamp: LocalDateTime): Int
}
