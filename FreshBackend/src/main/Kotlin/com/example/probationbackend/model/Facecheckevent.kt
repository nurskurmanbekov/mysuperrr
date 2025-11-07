package com.example.probationbackend.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "face_check_events")
data class FaceCheckEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "check_id") // Связь с серверным заданием (если есть)
    val checkId: String? = null,

    @Column(name = "user_id", nullable = false) // ID пользователя (осуждённого)
    val userId: Long,

    @Column(name = "device_id") // ID устройства из Traccar
    val deviceId: Long? = null,

    @Column(name = "outcome", nullable = false) // 'ok', 'failed', 'declined', 'failed_network', 'late_ok', 'late_failed'
    val outcome: String,

    @Column(name = "taken_at", nullable = false) // Время получения фото/результата
    val takenAt: LocalDateTime,

    @Column(name = "distance") // Результат верификации (расстояние)
    val distance: Double? = null,

    @Column(name = "deadline_iso") // Срок проверки
    val deadlineIso: String? = null,

    @Column(name = "app_version") // Версия мобильного приложения
    val appVersion: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)