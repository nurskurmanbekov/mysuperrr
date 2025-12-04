// src/main/kotlin/com/example/probationbackend/dto/LoginRequest.kt

package com.example.probationbackend.dto

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank(message = "Логин не может быть пустым")
    val login: String,

    @field:NotBlank(message = "Пароль не может быть пустым")
    val password: String
)