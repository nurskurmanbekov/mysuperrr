// src/main/kotlin/com/example/probationbackend/controller/AuthController.kt
package com.example.probationbackend.controller

import com.example.probationbackend.dto.LoginRequest
import com.example.probationbackend.model.User
import com.example.probationbackend.service.AuthService
import com.example.probationbackend.service.JwtTokenProvider // Импортируем
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val jwtTokenProvider: JwtTokenProvider,
    private val authService: AuthService
) {

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<*> {
        val user = authService.authenticate(request.email, request.password)
        return if (user != null) {
            val rbacAttributes = authService.getUserRbacAttributes(user)
            val userResponse = mapOf(
                "id" to user.id,
                "name" to user.inn,
                "email" to user.inn,
                "administrator" to (rbacAttributes["administrator"] as? Boolean ?: false),
                "attributes" to rbacAttributes
            )

            val token = jwtTokenProvider.generateToken(user)

            // Возврат токена и пользователя
            ResponseEntity.ok<Map<String, Any>>(mapOf("token" to token, "user" to userResponse))
        } else {
            // Возврат сообщения об ошибке
            ResponseEntity.status(401).body<Map<String, String>>(mapOf("message" to "Invalid credentials"))
        }
    }

    @GetMapping("/me")
    fun session(@RequestHeader("Authorization") authHeader: String?): ResponseEntity<*> {
        // Проверяем заголовок Authorization
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body<Map<String, String>>(mapOf("message" to "Missing or invalid Authorization header"))
        }
        val token = authHeader.substring(7) // Убираем "Bearer "

        return try {
            // Извлекаем ID пользователя из токена
            val userId = jwtTokenProvider.getUserIdFromToken(token)
            if (userId == null) {
                // ID не найден в токене или токен невалиден
                return ResponseEntity.status(401).body<Map<String, String>>(mapOf("message" to "Invalid or expired token"))
            }

            // Находим пользователя в базе по ID
            val user = authService.findUserById(userId)
            if (user != null) {
                // Получаем RBAC атрибуты
                val rbacAttributes = authService.getUserRbacAttributes(user)
                val userResponse = mapOf(
                    "id" to user.id,
                    "name" to user.inn,
                    "email" to user.inn,
                    "administrator" to (rbacAttributes["administrator"] as? Boolean ?: false),
                    "attributes" to rbacAttributes
                )
                ResponseEntity.ok<Map<String, Any>>(mapOf("user" to userResponse))
            } else {
                // Пользователь не найден в базе (токен устарел/пользователь удалён)
                ResponseEntity.status(401).body<Map<String, String>>(mapOf("message" to "User not found"))
            }
        } catch (e: Exception) {
            // Ошибка при проверке токена (например, подпись не совпадает)
            ResponseEntity.status(401).body<Map<String, String>>(mapOf("message" to "Token verification failed"))
        }
    }

    @PostMapping("/logout")
    fun logout(): ResponseEntity<Unit> {
        // Для JWT "logout" часто означает просто удаление токена из localStorage на фронте.
        // Если вы хотите инвалидировать токен на стороне сервера, нужно реализовать "чёрный список".
        // Пока просто возвращаем OK.
        return ResponseEntity.ok(Unit)
    }
}

// DTO остаются без изменений
data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String? = null,
    val user: User? = null,
    val message: String? = null
)