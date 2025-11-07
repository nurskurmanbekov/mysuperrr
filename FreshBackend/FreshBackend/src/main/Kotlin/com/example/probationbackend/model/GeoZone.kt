package com.example.probationbackend.model

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(name = "geozones")
data class GeoZone(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "name", nullable = false)
    val name: String, // Название геозоны

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    val client: Client, // Связь с осужденным

    // Polygon данные в формате GeoJSON (массив координат [[lat, lng], [lat, lng], ...])
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "polygon_coordinates", columnDefinition = "jsonb", nullable = false)
    val polygonCoordinates: List<List<Double>>, // Координаты полигона

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true, // Активна ли геозона

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
