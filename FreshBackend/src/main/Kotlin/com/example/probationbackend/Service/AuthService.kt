// src/main/kotlin/com/example/probationbackend/service/AuthService.kt
package com.example.probationbackend.service

import com.example.probationbackend.model.User
import com.example.probationbackend.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val objectMapper: ObjectMapper
) {

    fun authenticate(inn: String, rawPassword: String): User? {
        val user = userRepository.findByInn(inn).orElse(null) ?: return null
        return if (passwordEncoder.matches(rawPassword, user.passwordHash)) {
            user
        } else {
            null
        }
    }
    fun findUserById(id: Long): User? {
        return userRepository.findById(id).orElse(null)
    }

    // Новый метод для извлечения RBAC атрибутов из пользователя
    fun getUserRbacAttributes(user: User): Map<String, Any> {
        val attributes = mutableMapOf<String, Any>()

        // --- Определение роли ---
        // Вариант 1: Из поля attributes JSONB (для совместимости)
        val userRoleFromJson = user.attributes?.get("role") as? String
        // Вариант 2: Из поля inn (например, 'admin_user' - админ)
        val isAdministrator = user.inn == "admin_user" // Замените "admin_user" на реальный INN администратора
        // Вариант 3: Из поля userType (новый подход)
        val userType = user.userType // 'employee' или 'probationer'

        // Приоритет: userType > role из JSON > default
        val effectiveRole = when {
            isAdministrator -> "deptAdmin" // Администратор всегда админ
            userType == "probationer" -> "probationer" // Осужденный
            else -> userRoleFromJson ?: "inspector" // Сотрудник, роль из JSON или по умолчанию
        }

        attributes["role"] = effectiveRole
        attributes["administrator"] = isAdministrator

        // --- Определение mruId ---
        // Приоритет: mruId из модели > mruId из JSON > null
        val mruIdFromModel = user.mruId
        val mruIdFromJson = user.attributes?.get("mruId") as? String
        val effectiveMruId = mruIdFromModel ?: mruIdFromJson

        if (effectiveMruId != null) {
            attributes["mruId"] = effectiveMruId
        }

        // --- Определение districtIds ---
        // Приоритет: districtIds из JSON (если есть в attributes)
        val districtIdsFromJson = user.attributes?.get("districtIds") as? List<*>
        if (districtIdsFromJson != null) {
            val districtIds = districtIdsFromJson.filterIsInstance<Int>() // или Long
            attributes["districtIds"] = districtIds
        } else {
            attributes["districtIds"] = emptyList<Int>() // По умолчанию пустой список
        }

        // --- Другие атрибуты ---
        // Добавьте другие атрибуты по необходимости

        return attributes
    }

    fun createUser(inn: String, rawPassword: String, uniqueId: String, userType: String, mruId: String? = null, role: String? = null, districtIds: List<Int>? = null): User {
        if (userRepository.findByInn(inn).isPresent) {
            throw IllegalArgumentException("User with INN $inn already exists")
        }
        if (userRepository.findByUniqueId(uniqueId).isPresent) {
            throw IllegalArgumentException("User with uniqueId $uniqueId already exists")
        }

        val encodedPassword = passwordEncoder.encode(rawPassword)

        // Формируем JSONB attributes
        val attributesMap = mutableMapOf<String, Any>()
        if (!role.isNullOrBlank()) attributesMap["role"] = role
        // districtIds добавляем в JSONB, если нужно хранить там
        if (districtIds != null) attributesMap["districtIds"] = districtIds

        val newUser = User(
            inn = inn,
            passwordHash = encodedPassword,
            uniqueId = uniqueId,
            fcmToken = null,
            attributes = attributesMap,
            userType = userType, // Устанавливаем тип пользователя
            mruId = mruId // Устанавливаем ID МРУ/района
        )
        return userRepository.save(newUser)
    }
}