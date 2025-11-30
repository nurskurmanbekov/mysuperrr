// src/main/kotlin/com/example/probationbackend/controller/RegistryController.kt

package com.example.probationbackend.controller

import com.example.probationbackend.dto.RegistryCreateRequest
import com.example.probationbackend.service.RegistryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/registry")
class RegistryController(
    private val registryService: RegistryService
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
        @PathVariable id: Long,
        @RequestPart("request") request: RegistryCreateRequest,
        @RequestPart("photo", required = false) photo: MultipartFile?
    ): ResponseEntity<*> {
        // Все авторизованные пользователи могут редактировать клиентов
        return try {
            val client = registryService.updateClient(id, request, photo)
            ResponseEntity.ok(client)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @DeleteMapping("/{id}")
    fun deleteClient(
        @PathVariable id: Long
    ): ResponseEntity<*> {
        // Все авторизованные пользователи могут удалять клиентов
        return try {
            registryService.deleteClient(id)
            ResponseEntity.ok(mapOf("message" to "Client deleted successfully"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }
}