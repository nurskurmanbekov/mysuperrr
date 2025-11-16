package com.example.probationbackend.controller

import com.example.probationbackend.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/test")
class TestController(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @GetMapping("/check-admin")
    fun checkAdmin(): Map<String, Any> {
        val user = userRepository.findByInn("admin_user").orElse(null)

        if (user == null) {
            return mapOf(
                "exists" to false,
                "message" to "Пользователь admin_user не найден в базе"
            )
        }

        val testPassword = "admin123"
        val passwordMatches = passwordEncoder.matches(testPassword, user.passwordHash)

        return mapOf(
            "exists" to true,
            "inn" to user.inn,
            "passwordHash" to user.passwordHash,
            "testPassword" to testPassword,
            "passwordMatches" to passwordMatches,
            "userType" to user.userType,
            "attributes" to (user.attributes ?: emptyMap<String, Any>())
        )
    }

    @GetMapping("/generate-hash")
    fun generateHash(@RequestParam password: String): Map<String, String> {
        val hash = passwordEncoder.encode(password)
        return mapOf(
            "password" to password,
            "hash" to hash
        )
    }
}
