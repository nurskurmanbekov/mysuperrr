package com.example.probationbackend.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "geozone_violations")
data class GeoZoneViolation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "geozone_id", nullable = false)
    val geoZone: GeoZone, // Связь с геозоной

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    val client: Client, // Связь с осужденным

    @Column(name = "violation_type", nullable = false)
    val violationType: String, // Тип нарушения: "EXIT" или "ENTRY"

    @Column(name = "latitude", nullable = false)
    val latitude: Double, // Координаты нарушения

    @Column(name = "longitude", nullable = false)
    val longitude: Double,

    @Column(name = "notification_sent", nullable = false)
    var notificationSent: Boolean = false, // Отправлено ли уведомление

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
