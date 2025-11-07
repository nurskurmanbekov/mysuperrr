package com.example.probationbackend.dto

data class FaceCheckRegisterTokenRequest(
    val device_unique: String, // uniqueId
    val fcm_token: String
)