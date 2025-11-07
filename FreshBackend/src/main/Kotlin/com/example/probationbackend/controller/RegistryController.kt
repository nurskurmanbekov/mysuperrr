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

    // Добавьте другие методы (GET /{id}, PUT /{id}, DELETE /{id}) по необходимости
}