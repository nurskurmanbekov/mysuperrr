package com.example.probationbackend.model

import com.fasterxml.jackson.annotation.JsonManagedReference
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "clients")
data class Client(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "fio", nullable = false)
    val fio: String,

    @Column(name = "unique_id") // Добавьте это поле
    val uniqueId: String? = null,

    @Column(name = "inn", nullable = true, unique = true) // ← ИЗМЕНИТЕ на nullable = true
    val inn: String?,

    val identifier: String? = null, // Может быть null

    val unit: String? = null, // Может быть null

    @Column(name = "obs_type") // 'Электронный надзор', 'Мобильное приложение', 'Иное'
    val obsType: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    // Дополнительные поля из RegistryPage
    @Column(name = "birth_date")
    val birthDate: LocalDate? = null,

    @Column(name = "age")
    val age: Int? = null, // Возраст (вычисляется из birthDate, но хранится отдельно)

    @Column(name = "sex")
    val sex: String? = null, // 'М' или 'Ж'

    @Column(name = "passport")
    val passport: String? = null,

    @Column(name = "reg_address")
    val regAddress: String? = null,

    @Column(name = "fact_address")
    val factAddress: String? = null,

    @Column(name = "contact1")
    val contact1: String? = null,

    @Column(name = "contact2")
    val contact2: String? = null,

    @Column(name = "erp_number")
    val erpNumber: String? = null,

    @Column(name = "obs_start")
    val obsStart: LocalDate? = null,

    @Column(name = "obs_end")
    val obsEnd: LocalDate? = null,

    @Column(name = "degree")
    val degree: String? = null,

    @Column(name = "ud_number")
    val udNumber: String? = null,

    @OneToMany(mappedBy = "client", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    val articles: MutableList<Article> = mutableListOf(),

    @Column(name = "extra_info", columnDefinition = "TEXT")
    val extraInfo: String? = null,

    @Column(name = "measures", columnDefinition = "TEXT")
    val measures: String? = null,

    @Column(name = "app_password", nullable = false) // Пароль для приложения
    var appPassword: String, // Хранить только хэш!

    @Column(name = "photo_key") // Ключ для фото (например, уникальный ID или хэш)
    var photoKey: String? = null,

    // НОВОЕ: Связь с районом для RBAC
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id")
    var district: District? = null
)