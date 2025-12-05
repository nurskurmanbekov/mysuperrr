package com.example.probationbackend.model

import com.fasterxml.jackson.annotation.JsonBackReference
import jakarta.persistence.*

@Entity
@Table(name = "articles")
data class Article(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @JsonBackReference
    var client: Client? = null,

    @Column(name = "article")
    val article: String? = null,

    @Column(name = "part")
    val part: String? = null,

    @Column(name = "point")
    val point: String? = null
)
