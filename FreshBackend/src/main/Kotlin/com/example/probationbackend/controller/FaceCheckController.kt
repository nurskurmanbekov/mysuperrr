package com.example.probationbackend.controller

import com.example.probationbackend.dto.FaceCheckRegisterTokenRequest
import com.example.probationbackend.dto.FaceCheckResultRequest
import com.example.probationbackend.model.FaceCheckEvent
import com.example.probationbackend.repository.FaceCheckEventRepository
import com.example.probationbackend.repository.UserRepository
import com.example.probationbackend.service.FaceCheckService
import com.example.probationbackend.service.FcmService
import com.example.probationbackend.service.PhotoStorageService
import com.example.probationbackend.service.TraccarService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@RestController
@RequestMapping("/api/facecheck")
class FaceCheckController(
    private val faceCheckService: FaceCheckService,
    private val photoStorageService: PhotoStorageService,
    private val fcmService: FcmService,
    private val userRepository: UserRepository,
    private val traccarService: TraccarService, // ИСПРАВЛЕНО: добавлена запятая
    private val faceCheckEventRepository: FaceCheckEventRepository
) {

    // Приём фото от мобильного приложения для верификации
    @PostMapping("/verify")
    fun verifyFace(
        @RequestParam("userId") userId: String,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<*> {
        val tempFile = java.io.File.createTempFile("selfie_", ".jpg")
        try {
            file.transferTo(tempFile)
            val result = faceCheckService.verifyFace(userId, tempFile)
            return ResponseEntity.ok(mapOf(
                "ok" to result.success,
                "message" to result.message,
                "distance" to result.distance
            ))
        } catch (e: Exception) {
            e.printStackTrace()
            return ResponseEntity.status(500).body(mapOf("error" to "Internal Server Error"))
        } finally {
            tempFile.delete()
        }
    }

    // Приём результата FaceID от мобильного приложения
    @PostMapping("/result")
    fun receiveResult(@RequestBody request: FaceCheckResultRequest): ResponseEntity<*> {
        try {
            // 1. Найти пользователя по userId (обычно это uniqueId/ИНН)
            val user = userRepository.findByUniqueId(request.user_id).orElse(null)
                ?: return ResponseEntity.badRequest().body(mapOf("error" to "User not found for userId: ${request.user_id}"))

            // 2. Найти deviceId по device_id (если device_id — это uniqueId)
            var deviceId: Long? = null
            if (request.device_id != null) {
                val deviceNode = traccarService.getDeviceByUniqueId(request.device_id)
                if (deviceNode != null) {
                    deviceId = deviceNode.get("id").asLong()
                } else {
                    println("Device not found for uniqueId: ${request.device_id}")
                }
            }

            // 3. Преобразовать taken_at (timestamp) в LocalDateTime
            val takenAt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(request.taken_at),
                ZoneOffset.UTC
            )

            // 4. Создать событие FaceCheckEvent
            val faceCheckEvent = FaceCheckEvent(
                userId = user.id!!,
                deviceId = deviceId,
                checkId = request.check_id,
                outcome = request.outcome,
                takenAt = takenAt,
                distance = request.distance,
                deadlineIso = request.deadline_iso,
                appVersion = request.app_version
            )

            // 5. Сохранить событие в БД
            faceCheckEventRepository.save(faceCheckEvent)

            println("Saved result event: ${faceCheckEvent.id} for user: ${request.user_id}")
            return ResponseEntity.ok(mapOf("status" to "received and saved"))
        } catch (e: Exception) {
            e.printStackTrace()
            return ResponseEntity.status(500).body(mapOf("error" to "Failed to save result: ${e.message}"))
        }
    }

    // Регистрация FCM-токена устройства
    @PostMapping("/register-token")
    fun registerToken(@RequestBody request: FaceCheckRegisterTokenRequest): ResponseEntity<Any> {
        try {
            // Найти пользователя по uniqueId (device_unique в DTO)
            val user = userRepository.findByUniqueId(request.device_unique).orElse(null)
                ?: return ResponseEntity.badRequest().body(mapOf("error" to "User not found for uniqueId: ${request.device_unique}"))

            // Обновить fcmToken в БД
            user.fcmToken = request.fcm_token
            userRepository.save(user)

            println("Registered FCM token ${request.fcm_token} for user: ${request.device_unique}")
            return ResponseEntity.ok(mapOf("status" to "success"))
        } catch (e: Exception) {
            e.printStackTrace()
            return ResponseEntity.status(500).body(mapOf("error" to "Failed to register token: ${e.message}"))
        }
    }
}