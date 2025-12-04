// src/main/kotlin/com/example/probationbackend/config/SecurityConfig.kt

package com.example.probationbackend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.util.*

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder() // Используем BCrypt для хеширования паролей
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        // Разрешаем все необходимые origins для фронтенда
        configuration.allowedOriginPatterns = listOf(
            "http://localhost:5173",
            "http://192.168.88.24:3000",
            "http://localhost:3000"
        )
        // Разрешаем все HTTP методы, включая OPTIONS для preflight
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH")
        // Разрешаем все заголовки
        configuration.allowedHeaders = listOf("*")
        // Разрешаем отправку credentials (cookies, authorization headers)
        configuration.allowCredentials = true
        // Заголовки, которые браузер может читать из ответа
        configuration.exposedHeaders = listOf("Authorization", "Content-Type")
        // Время кеширования preflight запросов (в секундах)
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfigurationSource()) } // Включаем CORS
            .csrf { it.disable() } // Отключаем CSRF, т.к. используем stateless аутентификацию (предположительно JWT или сессии)
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) } // Не создаём сессии
            .authorizeHttpRequests { authz ->
                authz
                  //  .requestMatchers("/api/login", "/api/session").permitAll() // Разрешаем аутентификацию
                   // .requestMatchers("/api/facecheck/verify", "/api/facecheck/result", "/api/facecheck/register-token").permitAll() // Разрешаем мобильному приложению доступ
                   // .requestMatchers("/api/faces/**").permitAll() // Разрешаем доступ к фото (если PhotoController используется)
                  //  .requestMatchers("/api/registry/**").hasRole("ADMIN") // Пример: только администраторы могут управлять реестром
                  //  .requestMatchers("/api/devices/**", "/api/events/**").authenticated() // Требуется аутентификация для доступа к устройствам/событиям (для фронтенда)
                   // .requestMatchers("/api/login", "/api/session").permitAll()
                   .anyRequest().permitAll() // Все остальные запросы требуют аутентификации
            }
            .httpBasic { } // Пока используем Basic Auth для тестирования, позже можно заменить на JWT

        return http.build()
    }
}