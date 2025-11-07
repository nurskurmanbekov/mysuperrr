package com.example.probationbackend.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "mrus")
data class Mru(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "name", nullable = false)
    val name: String, // Название МРУ

    @Column(name = "code", unique = true, nullable = false)
    val code: String, // Уникальный код МРУ (например, "MRU_NORTH")

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    val department: Department, // Связь с департаментом

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
