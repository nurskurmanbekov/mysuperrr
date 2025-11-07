// src/main/kotlin/com/example/probationbackend/model/User.kt
package com.example.probationbackend.model

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "inn", nullable = false, unique = true)
    val inn: String,

    @Column(name = "password_hash", nullable = false)
    val passwordHash: String,

    @Column(name = "unique_id", nullable = false, unique = true)
    val uniqueId: String, // Связь с Traccar

    @Column(name = "fcm_token")
    var fcmToken: String?,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    // Поле для RBAC атрибутов (хранится как JSONB в PostgreSQL)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", columnDefinition = "jsonb")
    val attributes: Map<String, Any>? = null,

    // НОВОЕ: Тип пользователя
    @Column(name = "user_type", nullable = false)
    val userType: String, // 'employee' или 'probationer'

    // НОВОЕ: ID МРУ/района
    @Column(name = "mru_id")
    val mruId: String? // Может быть null, если не задано
)