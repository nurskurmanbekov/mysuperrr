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
        configuration.allowedOriginPatterns = listOf("http://localhost:5173") // В продакшене укажите конкретные домены
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
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