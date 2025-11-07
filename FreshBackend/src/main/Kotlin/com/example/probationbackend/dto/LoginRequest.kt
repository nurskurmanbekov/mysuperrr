// src/main/kotlin/com/example/probationbackend/dto/LoginRequest.kt

package com.example.probationbackend.dto

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank(message = "ИНН не может быть пустым")
    val email: String,

    @field:NotBlank(message = "Пароль не может быть пустым")
    val password: String
)