// src/main/kotlin/com/example/probationbackend/controller/RegistryController.kt

package com.example.probationbackend.controller

import com.example.probationbackend.dto.RegistryCreateRequest
import com.example.probationbackend.model.User
import com.example.probationbackend.repository.UserRepository
import com.example.probationbackend.security.JwtTokenProvider
import com.example.probationbackend.service.RegistryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/registry")
class RegistryController(
    private val registryService: RegistryService,
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository
) {

    @PostMapping
    fun createClient(
        @RequestHeader("Authorization") authHeader: String,
        @RequestPart("request") request: RegistryCreateRequest,
        @RequestPart("photo", required = false) photo: MultipartFile? // Фото как отдельная часть
    ): ResponseEntity<*> {
        // Проверка прав доступа: только deptAdmin может создавать клиентов
        val currentUser = getCurrentUser(authHeader)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))

        if (!isDeptAdmin(currentUser)) {
            return ResponseEntity.status(403).body(
                mapOf("error" to "Доступ запрещён. Только Администратор департамента может добавлять клиентов.")
            )
        }

        return try {
            val client = registryService.createClient(request, photo)
            ResponseEntity.ok(client)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @GetMapping
    fun getAllClients(): ResponseEntity<*> {
        val clients = registryService.findAllClients()
        return ResponseEntity.ok(clients)
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private fun getCurrentUser(authHeader: String?): User? {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null
        }
        val token = authHeader.substring(7)
        val userId = jwtTokenProvider.getUserIdFromToken(token) ?: return null
        return userRepository.findById(userId).orElse(null)
    }

    private fun isDeptAdmin(user: User): Boolean {
        val role = user.attributes?.get("role") as? String
        return role == "deptAdmin"
    }

    // Добавьте другие методы (GET /{id}, PUT /{id}, DELETE /{id}) по необходимости
}