// src/main/kotlin/com/example/probationbackend/controller/RegistryController.kt

package com.example.probationbackend.controller

import com.example.probationbackend.dto.RegistryCreateRequest
import com.example.probationbackend.model.User
import com.example.probationbackend.repository.UserRepository
import com.example.probationbackend.service.JwtTokenProvider
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
        @RequestPart("request") request: RegistryCreateRequest,
        @RequestPart("photo", required = false) photo: MultipartFile? // Фото как отдельная часть
    ): ResponseEntity<*> {
        // Все авторизованные пользователи могут создавать клиентов (inspectors, mruAdmin, deptAdmin)
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

    @PutMapping("/{id}")
    fun updateClient(
        @RequestHeader("Authorization") authHeader: String,
        @PathVariable id: Long,
        @RequestPart("request") request: RegistryCreateRequest,
        @RequestPart("photo", required = false) photo: MultipartFile?
    ): ResponseEntity<*> {
        val currentUser = getCurrentUser(authHeader)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))

        if (!isDeptAdmin(currentUser)) {
            return ResponseEntity.status(403).body(
                mapOf("error" to "Доступ запрещён. Только Администратор департамента может редактировать клиентов.")
            )
        }

        return try {
            val client = registryService.updateClient(id, request, photo)
            ResponseEntity.ok(client)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @DeleteMapping("/{id}")
    fun deleteClient(
        @RequestHeader("Authorization") authHeader: String,
        @PathVariable id: Long
    ): ResponseEntity<*> {
        val currentUser = getCurrentUser(authHeader)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))

        if (!isDeptAdmin(currentUser)) {
            return ResponseEntity.status(403).body(
                mapOf("error" to "Доступ запрещён. Только Администратор департамента может удалять клиентов.")
            )
        }

        return try {
            registryService.deleteClient(id)
            ResponseEntity.ok(mapOf("message" to "Client deleted successfully"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
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
}