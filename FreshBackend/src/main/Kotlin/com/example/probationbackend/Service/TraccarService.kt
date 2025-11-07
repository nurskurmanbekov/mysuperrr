package com.example.probationbackend.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
class TraccarService(
    @Value("\${traccar.base-url:http://localhost:8091}") private val traccarBaseUrl: String,
    @Value("\${traccar.api.username:admin}") private val traccarUsername: String,
    @Value("\${traccar.api.password:admin}") private val traccarPassword: String,
    private val objectMapper: ObjectMapper
) {

    private val webClient: WebClient = WebClient.builder()
        .baseUrl(traccarBaseUrl)
        .defaultHeaders { headers ->
            headers.setBasicAuth(traccarUsername, traccarPassword)
            headers.contentType = MediaType.APPLICATION_JSON
        }
        .build()

    fun createDevice(uniqueId: String, name: String): JsonNode? {
        val devicePayload = mapOf(
            "name" to name,
            "uniqueId" to uniqueId,
            "status" to "unknown",
            "attributes" to mapOf<String, Any>()
        )

        return try {
            webClient.post()
                .uri("/api/devices")
                .bodyValue(devicePayload)
                .retrieve()
                .onStatus({ status -> status.is4xxClientError }) { response -> // ИСПРАВЛЕНО: лямбда
                    response.bodyToMono<String>()
                        .flatMap { body ->
                            throw RuntimeException("Client Error: ${response.statusCode()} - $body")
                        }
                }
                .onStatus({ status -> status.is5xxServerError }) { response -> // ИСПРАВЛЕНО: лямбда
                    response.bodyToMono<String>()
                        .flatMap { body ->
                            throw RuntimeException("Server Error: ${response.statusCode()} - $body")
                        }
                }
                .bodyToMono(String::class.java)
                .map { objectMapper.readTree(it) }
                .block()
        } catch (e: Exception) {
            println("Error creating device in Traccar: ${e.message}")
            null
        }
    }

    fun updateDeviceAttributes(uniqueId: String, attributes: Map<String, Any>): JsonNode? {
        val device = getDeviceByUniqueId(uniqueId) ?: return null

        val deviceMap = objectMapper.convertValue(device, MutableMap::class.java) as MutableMap<String, Any>
        val currentAttributes = (deviceMap["attributes"] as? Map<*, *> ?: emptyMap<String, Any>()).toMutableMap()
        currentAttributes.putAll(attributes)
        deviceMap["attributes"] = currentAttributes

        return try {
            webClient.put()
                .uri("/api/devices/${device.get("id").asLong()}")
                .bodyValue(deviceMap)
                .retrieve()
                .onStatus({ status -> status.is4xxClientError }) { response -> // ИСПРАВЛЕНО: лямбда
                    response.bodyToMono<String>()
                        .flatMap { body ->
                            throw RuntimeException("Client Error: ${response.statusCode()} - $body")
                        }
                }
                .onStatus({ status -> status.is5xxServerError }) { response -> // ИСПРАВЛЕНО: лямбда
                    response.bodyToMono<String>()
                        .flatMap { body ->
                            throw RuntimeException("Server Error: ${response.statusCode()} - $body")
                        }
                }
                .bodyToMono(String::class.java)
                .map { objectMapper.readTree(it) }
                .block()
        } catch (e: Exception) {
            println("Error updating device attributes in Traccar: ${e.message}")
            null
        }
    }

    fun getDeviceByUniqueId(uniqueId: String): JsonNode? {
        return try {
            webClient.get()
                .uri("/api/devices")
                .retrieve()
                .onStatus({ status -> status.is4xxClientError }) { response -> // ИСПРАВЛЕНО: лямбда
                    response.bodyToMono<String>()
                        .flatMap { body ->
                            throw RuntimeException("Client Error: ${response.statusCode()} - $body")
                        }
                }
                .onStatus({ status -> status.is5xxServerError }) { response -> // ИСПРАВЛЕНО: лямбда
                    response.bodyToMono<String>()
                        .flatMap { body ->
                            throw RuntimeException("Server Error: ${response.statusCode()} - $body")
                        }
                }
                .bodyToMono(String::class.java)
                .map { body ->
                    val devices = objectMapper.readTree(body)
                    if (devices.isArray) {
                        for (device in devices) {
                            if (device.get("uniqueId").asText() == uniqueId) {
                                return@map device
                            }
                        }
                    }
                    null
                }
                .block()
        } catch (e: Exception) {
            println("Error getting device from Traccar: ${e.message}")
            null
        }
    }

    // Упрощенные версии дополнительных методов
    fun getDevices(): JsonNode? {
        return try {
            webClient.get()
                .uri("/api/devices")
                .retrieve()
                .bodyToMono(String::class.java)
                .map { objectMapper.readTree(it) }
                .block()
        } catch (e: Exception) {
            println("Error getting devices from Traccar: ${e.message}")
            null
        }
    }

    fun getDeviceById(id: Long): JsonNode? {
        return try {
            webClient.get()
                .uri("/api/devices/$id")
                .retrieve()
                .bodyToMono(String::class.java)
                .map { objectMapper.readTree(it) }
                .block()
        } catch (e: Exception) {
            println("Error getting device by ID from Traccar: ${e.message}")
            null
        }
    }

    fun updateDevice(id: Long, device: Map<String, Any>): JsonNode? {
        return try {
            webClient.put()
                .uri("/api/devices/$id")
                .bodyValue(device)
                .retrieve()
                .bodyToMono(String::class.java)
                .map { objectMapper.readTree(it) }
                .block()
        } catch (e: Exception) {
            println("Error updating device in Traccar: ${e.message}")
            null
        }
    }

    // Метод для обновления атрибутов FaceID
    fun updateFaceIdAttributes(uniqueId: String, faceOk: Boolean, distance: Double?, message: String) {
        val nowIso = LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z"
        val attributes = mutableMapOf<String, Any>(
            "faceOk" to faceOk,
            "lastFaceAt" to nowIso,
            "lastFaceMsg" to message
        )
        if (distance != null) {
            attributes["lastFaceDist"] = distance
        }
        if (faceOk) {
            attributes["lastFaceOkAt"] = nowIso
            if (distance != null) {
                attributes["lastFaceOkDist"] = distance
            }
        } else {
            attributes["lastFaceFailAt"] = nowIso
        }

        updateDeviceAttributes(uniqueId, attributes)
    }
}