// src/main/kotlin/com/example/probationbackend/service/JwtTokenProvider.kt

package com.example.probationbackend.service

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import java.util.concurrent.TimeUnit

@Component
class JwtTokenProvider(
    @Value("\${app.jwt.secret:myDefaultSecretKeyForDevPurposesOnly}") private val jwtSecret: String,
    @Value("\${app.jwt.expiration:86400000}") private val jwtExpiration: Long // 86400000 ms = 24 часа в миллисекундах
) {

    private val algorithm: Algorithm = Algorithm.HMAC256(jwtSecret)

    // Генерация токена для пользователя
    fun generateToken(user: com.example.probationbackend.model.User): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtExpiration)

        return JWT.create()
            .withSubject("UserToken") // Удобно для идентификации типа токена
            .withClaim("userId", user.id) // Сохраняем ID пользователя в токене
            .withClaim("inn", user.inn)   // Сохраняем INN (логин) в токене (опционально, но полезно)
            .withExpiresAt(expiryDate)    // Устанавливаем время истечения
            .withIssuedAt(now)            // Устанавливаем время выдачи
            .sign(algorithm)              // Подписываем токен
    }

    // Проверка токена и извлечение ID пользователя
    fun getUserIdFromToken(token: String): Long? {
        return try {
            val verifier: JWTVerifier = JWT.require(algorithm).build()
            val jwt: DecodedJWT = verifier.verify(token)
            jwt.getClaim("userId").asLong()
        } catch (e: Exception) {
            // Логирование ошибки (по желанию)
            // logger.error("JWT verification failed", e)
            null
        }
    }

    // Проверка токена и извлечение INN пользователя (опционально)
    fun getInnFromToken(token: String): String? {
        return try {
            val verifier: JWTVerifier = JWT.require(algorithm).build()
            val jwt: DecodedJWT = verifier.verify(token)
            jwt.getClaim("inn").asString()
        } catch (e: Exception) {
            // logger.error("JWT verification failed", e)
            null
        }
    }
}