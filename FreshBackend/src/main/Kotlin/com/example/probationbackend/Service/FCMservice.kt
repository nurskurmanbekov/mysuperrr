package com.example.probationbackend.service

import com.google.firebase.messaging.*
import org.springframework.stereotype.Service

@Service
class FcmService {

    fun sendFaceCheckNotification(fcmToken: String, checkId: String, deadlineIso: String?) {
        val message = Message.builder()
            .setToken(fcmToken)
            .putData("type", "face_check")
            .putData("check_id", checkId)
            .apply { if (deadlineIso != null) putData("deadline_iso", deadlineIso) }
            .setNotification(
                Notification.builder()
                    .setTitle("Проверка Face-ID")
                    .setBody("Нажмите, чтобы пройти проверку личности.")
                    .build()
            )
            .build()

        try {
            val response = FirebaseMessaging.getInstance().send(message)
            println("Successfully sent message: $response")
        } catch (e: FirebaseMessagingException) {
            println("Failed to send FCM message: ${e.message}")
            e.printStackTrace()
        }
    }
}