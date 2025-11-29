package com.example.probationbackend.model

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Таблица для хранения последних GPS позиций устройств
 * Backend сам хранит координаты, чтобы Frontend не зависел от Traccar
 */
@Entity
@Table(
    name = "device_positions",
    indexes = [
        Index(name = "idx_device_id", columnList = "device_id"),
        Index(name = "idx_unique_id", columnList = "unique_id"),
        Index(name = "idx_timestamp", columnList = "timestamp")
    ]
)
data class DevicePosition(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    /**
     * ID клиента в нашей системе
     */
    @Column(name = "device_id", nullable = false)
    val deviceId: Long,

    /**
     * Уникальный ID устройства (ИНН для нашей системы)
     */
    @Column(name = "unique_id", nullable = false, length = 50)
    val uniqueId: String,

    /**
     * Широта (latitude)
     */
    @Column(nullable = false)
    val latitude: Double,

    /**
     * Долгота (longitude)
     */
    @Column(nullable = false)
    val longitude: Double,

    /**
     * Скорость (км/ч)
     */
    @Column(nullable = true)
    val speed: Double? = 0.0,

    /**
     * Направление движения (градусы)
     */
    @Column(nullable = true)
    val bearing: Double? = 0.0,

    /**
     * Высота над уровнем моря (метры)
     */
    @Column(nullable = true)
    val altitude: Double? = 0.0,

    /**
     * Точность определения координат (метры)
     */
    @Column(nullable = true)
    val accuracy: Double? = 0.0,

    /**
     * Уровень заряда батареи (%)
     */
    @Column(nullable = true)
    val battery: Double? = 0.0,

    /**
     * Время получения GPS данных
     */
    @Column(nullable = false)
    val timestamp: LocalDateTime,

    /**
     * Время получения данных сервером
     */
    @Column(name = "server_time", nullable = false)
    val serverTime: LocalDateTime = LocalDateTime.now(),

    /**
     * Статус: online/offline
     * Считается online если последние данные не старше 5 минут
     */
    @Column(nullable = false, length = 20)
    val status: String = "online",

    /**
     * Успешно ли отправлено в Traccar
     */
    @Column(name = "sent_to_traccar", nullable = false)
    val sentToTraccar: Boolean = false,

    /**
     * ID позиции в Traccar (если есть)
     */
    @Column(name = "traccar_position_id", nullable = true)
    val traccarPositionId: Long? = null
) {
    /**
     * Вычисляем статус на основе времени последнего обновления
     * Считается online если последнее обновление было не более 5 минут назад
     */
    fun isOnline(): Boolean {
        val fiveMinutesAgo = LocalDateTime.now().minusMinutes(5)
        return serverTime.isAfter(fiveMinutesAgo)
    }
}
