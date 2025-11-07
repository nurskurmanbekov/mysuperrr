package com.example.probationbackend.dto

data class LoginResponse(
    val token: String, // или JWT
    val userId: Long,
    val uniqueId: String
)