package com.example.probationbackend.service

import org.springframework.stereotype.Service

/**
 * Firebase Cloud Messaging Service
 * Handles push notifications to mobile devices
 */
@Service
class FcmService {

    /**
     * Отправка push-уведомления на устройство по FCM токену
     *
     * @param fcmToken FCM токен устройства
     * @param title Заголовок уведомления
     * @param body Текст уведомления
     * @param data Дополнительные данные
     */
    fun sendNotification(
        fcmToken: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ): Boolean {
        // TODO: Реализовать отправку через Firebase Admin SDK
        // Пока логируем
        println("📲 FCM Notification:")
        println("   Token: ${fcmToken.take(20)}...")
        println("   Title: $title")
        println("   Body: $body")
        println("   Data: $data")

        return true
    }

    /**
     * Отправка уведомлений на несколько устройств
     */
    fun sendMulticastNotification(
        fcmTokens: List<String>,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ): Int {
        var successCount = 0

        fcmTokens.forEach { token ->
            if (sendNotification(token, title, body, data)) {
                successCount++
            }
        }

        println("📲 Sent $successCount/${fcmTokens.size} notifications")

        return successCount
    }
}
