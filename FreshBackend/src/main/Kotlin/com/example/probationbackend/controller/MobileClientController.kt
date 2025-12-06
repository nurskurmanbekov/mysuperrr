package com.example.probationbackend.controller

import com.example.probationbackend.model.User
import com.example.probationbackend.repository.UserRepository
import com.example.probationbackend.repository.ClientRepository
import com.example.probationbackend.service.JwtTokenProvider
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/mobile")
class MobileClientController(
    private val clientRepository: ClientRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository
) {

    @GetMapping("/profile")
    fun getProfile(@RequestHeader("Authorization") authHeader: String): ResponseEntity<*> {
        val currentUser = getCurrentUser(authHeader) ?: return ResponseEntity.status(401).body(
            mapOf("error" to "Unauthorized")
        )

        // Получаем данные клиента из реестра по ИНН
        val client = clientRepository.findByInn(currentUser.inn).orElse(null)
            ?: return ResponseEntity.status(404).body(
                mapOf("error" to "Client not found in registry")
            )

        // Возвращаем только необходимые данные
        val profileData = mapOf(
            "fio" to client.fio,
            "inn" to client.inn,
            "age" to client.age,
            "birthDate" to client.birthDate,
            "uniqueId" to client.uniqueId,
            "obsType" to client.obsType,
            "obsStart" to client.obsStart,
            "obsEnd" to client.obsEnd,
            "photoKey" to client.photoKey,
            "articles" to client.articles.map { article ->
                mapOf(
                    "id" to article.id,
                    "article" to article.article,
                    "part" to article.part,
                    "point" to article.point
                )
            }
        )

        return ResponseEntity.ok(profileData)
    }

    private fun getCurrentUser(authHeader: String?): User? {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null
        }

        val token = authHeader.substring(7)
        val inn = jwtTokenProvider.getInnFromToken(token) ?: return null

        return userRepository.findByInn(inn).orElse(null)
    }
}
