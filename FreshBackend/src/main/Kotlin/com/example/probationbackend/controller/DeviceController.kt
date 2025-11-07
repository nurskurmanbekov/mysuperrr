// src/main/kotlin/com/example/probationbackend/controller/DeviceController.kt

package com.example.probationbackend.controller

import com.example.probationbackend.service.TraccarService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.reactive.function.client.WebClientResponseException

@RestController
@RequestMapping("/api/devices")
class DeviceController(
    private val traccarService: TraccarService
) {

    @GetMapping
    fun getDevices(): ResponseEntity<*> {
        // Прокси к Traccar /api/devices
        // Пока просто возвращаем ответ от Traccar как есть
        // TODO: Добавить RBAC фильтрацию здесь или в TraccarService
        try {
            val devicesJson = traccarService.getDevices()
            return ResponseEntity.ok(devicesJson)
        } catch (e: WebClientResponseException) {
            return ResponseEntity.status(e.statusCode.value()).body(mapOf("error" to e.message))
        }
    }

    @GetMapping("/{id}")
    fun getDeviceById(@PathVariable id: Long): ResponseEntity<*> {
        // Прокси к Traccar /api/devices/{id}
        // TODO: Добавить RBAC проверку, что пользователь может видеть это устройство
        try {
            val deviceJson = traccarService.getDeviceById(id)
            return ResponseEntity.ok(deviceJson)
        } catch (e: WebClientResponseException) {
            return ResponseEntity.status(e.statusCode.value()).body(mapOf("error" to e.message))
        }
    }

    @PutMapping("/{id}")
    fun updateDevice(@PathVariable id: Long, @RequestBody device: Map<String, Any>): ResponseEntity<*> {
        // Прокси к Traccar /api/devices/{id}
        // TODO: Добавить RBAC проверку, что пользователь может обновлять это устройство
        try {
            val updatedDeviceJson = traccarService.updateDevice(id, device)
            return ResponseEntity.ok(updatedDeviceJson)
        } catch (e: WebClientResponseException) {
            return ResponseEntity.status(e.statusCode.value()).body(mapOf("error" to e.message))
        }
    }

    // Метод для поиска устройства по uniqueId (часто используется)
    @GetMapping(params = ["uniqueId"])
    fun getDeviceByUniqueId(@RequestParam uniqueId: String): ResponseEntity<*> {
        try {
            val device = traccarService.getDeviceByUniqueId(uniqueId)
            return if (device != null) {
                ResponseEntity.ok(listOf(device)) // Traccar возвращает массив в /api/devices
            } else {
                ResponseEntity.ok(listOf<Map<String, Any>>()) // Пустой массив, если не найдено
            }
        } catch (e: WebClientResponseException) {
            return ResponseEntity.status(e.statusCode.value()).body(mapOf("error" to e.message))
        }
    }
}