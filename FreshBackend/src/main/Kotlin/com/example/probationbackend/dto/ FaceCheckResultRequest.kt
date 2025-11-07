package com.example.probationbackend.dto

import java.time.LocalDateTime

data class FaceCheckResultRequest(
    val user_id: String, // обычно это uniqueId
    val device_id: String, // обычно это uniqueId
    val check_id: String?, // может быть null
    val outcome: String, // 'ok', 'failed', 'declined', 'failed_network', 'late_ok', 'late_failed'
    val taken_at: Long, // timestamp
    val distance: Double?, // может быть null
    val deadline_iso: String?, // может быть null
    val app_version: String?
)