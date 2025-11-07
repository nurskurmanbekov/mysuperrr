package com.example.probationbackend.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "districts")
data class District(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "name", nullable = false)
    val name: String, // Название района

    @Column(name = "code", unique = true, nullable = false)
    val code: String, // Уникальный код района

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mru_id", nullable = false)
    val mru: Mru, // Связь с МРУ

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
