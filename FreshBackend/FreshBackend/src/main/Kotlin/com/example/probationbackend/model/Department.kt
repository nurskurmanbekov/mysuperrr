package com.example.probationbackend.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "departments")
data class Department(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "name", nullable = false)
    val name: String, // Название департамента

    @Column(name = "code", unique = true, nullable = false)
    val code: String, // Уникальный код департамента (например, "CENTRAL")

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
