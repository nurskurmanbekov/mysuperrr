package com.example.probationbackend.service

import com.example.probationbackend.model.*
import com.example.probationbackend.repository.*
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AdminService(
    private val userRepository: UserRepository,
    private val clientRepository: ClientRepository,
    private val departmentRepository: DepartmentRepository,
    private val mruRepository: MruRepository,
    private val districtRepository: DistrictRepository,
    private val passwordEncoder: PasswordEncoder
) {

    // ============================================
    // УПРАВЛЕНИЕ ОРГАНИЗАЦИОННОЙ СТРУКТУРОЙ
    // ============================================

    fun getAllDepartments(): List<Department> = departmentRepository.findAll()
    fun getAllMrus(): List<Mru> = mruRepository.findAll()
    fun getAllDistricts(): List<District> = districtRepository.findAll()

    fun getMrusByDepartment(departmentId: Long): List<Mru> {
        val department = departmentRepository.findById(departmentId).orElseThrow {
            IllegalArgumentException("Department not found")
        }
        return mruRepository.findByDepartment(department)
    }

    fun getDistrictsByMru(mruId: Long): List<District> {
        val mru = mruRepository.findById(mruId).orElseThrow {
            IllegalArgumentException("MRU not found")
        }
        return districtRepository.findByMru(mru)
    }

    // ============================================
    // УПРАВЛЕНИЕ СОТРУДНИКАМИ
    // ============================================

    /**
     * Создание нового сотрудника
     */
    fun createEmployee(
        inn: String,
        password: String,
        uniqueId: String,
        role: String, // "deptAdmin", "mruAdmin", "inspector"
        districtId: Long? = null,
        districtIds: List<Long>? = null
    ): User {
        if (userRepository.findByInn(inn).isPresent) {
            throw IllegalArgumentException("User with INN $inn already exists")
        }

        val district = districtId?.let {
            districtRepository.findById(it).orElseThrow {
                IllegalArgumentException("District not found")
            }
        }

        // Формируем RBAC атрибуты
        val attributesMap = mutableMapOf<String, Any>()
        attributesMap["role"] = role

        if (role == "mruAdmin" && district != null) {
            // МРУ админ - сохраняем МРУ ID
            attributesMap["mruId"] = district.mru.id.toString()
        }

        if (role == "inspector" && districtIds != null) {
            // Инспектор - сохраняем список районов
            attributesMap["districtIds"] = districtIds
        }

        val user = User(
            inn = inn,
            passwordHash = passwordEncoder.encode(password),
            uniqueId = uniqueId,
            fcmToken = null,
            userType = "employee",
            mruId = district?.mru?.id?.toString(),
            attributes = attributesMap
        )

        return userRepository.save(user)
    }

    /**
     * Обновление сотрудника - перевод в другой район
     */
    fun updateEmployee(
        userId: Long,
        newDistrictId: Long? = null,
        newRole: String? = null,
        newDistrictIds: List<Long>? = null
    ): User {
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found")
        }

        if (user.userType != "employee") {
            throw IllegalArgumentException("User is not an employee")
        }

        val newDistrict = newDistrictId?.let {
            districtRepository.findById(it).orElseThrow {
                IllegalArgumentException("District not found")
            }
        }

        // Обновляем атрибуты
        val updatedAttributes = user.attributes?.toMutableMap() ?: mutableMapOf()

        if (newRole != null) {
            updatedAttributes["role"] = newRole
        }

        if (newDistrict != null) {
            updatedAttributes["mruId"] = newDistrict.mru.id.toString()
        }

        if (newDistrictIds != null) {
            updatedAttributes["districtIds"] = newDistrictIds
        }

        val updated = user.copy(
            mruId = newDistrict?.mru?.id?.toString() ?: user.mruId,
            attributes = updatedAttributes
        )

        return userRepository.save(updated)
    }

    /**
     * Получение всех сотрудников с учетом RBAC
     */
    fun getEmployees(currentUser: User): List<User> {
        val rbacAttrs = getRbacAttributes(currentUser)
        val role = rbacAttrs["role"] as? String ?: "inspector"

        return when (role) {
            "deptAdmin" -> {
                // Администратор департамента видит всех сотрудников
                userRepository.findByUserType("employee")
            }
            "mruAdmin" -> {
                // МРУ админ видит сотрудников своего МРУ
                val mruId = rbacAttrs["mruId"] as? String
                if (mruId != null) {
                    userRepository.findByMruIdAndUserType(mruId, "employee")
                } else {
                    emptyList()
                }
            }
            else -> {
                // Инспектор видит только себя
                listOf(currentUser)
            }
        }
    }

    /**
     * Удаление сотрудника
     */
    fun deleteEmployee(userId: Long) {
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found")
        }

        if (user.userType != "employee") {
            throw IllegalArgumentException("User is not an employee")
        }

        userRepository.deleteById(userId)
    }

    fun changeEmployeePassword(userId: Long, newPassword: String) {
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found")
        }

        if (user.userType != "employee") {
            throw IllegalArgumentException("User is not an employee")
        }

        val encodedPassword = passwordEncoder.encode(newPassword)
        val updatedUser = user.copy(passwordHash = encodedPassword)
        userRepository.save(updatedUser)
    }

    // ============================================
    // УПРАВЛЕНИЕ ОСУЖДЕННЫМИ
    // ============================================

    /**
     * Получение всех осужденных с учетом RBAC
     */
    fun getClients(currentUser: User): List<Client> {
        val rbacAttrs = getRbacAttributes(currentUser)
        val role = rbacAttrs["role"] as? String ?: "inspector"

        return when (role) {
            "deptAdmin" -> {
                // Администратор департамента видит всех осужденных
                clientRepository.findAll()
            }
            "mruAdmin" -> {
                // МРУ админ видит осужденных своих районов
                val mruId = rbacAttrs["mruId"] as? String
                if (mruId != null) {
                    val mru = mruRepository.findById(mruId.toLongOrNull() ?: 0).orElse(null)
                    if (mru != null) {
                        val districts = districtRepository.findByMru(mru)
                        clientRepository.findByDistrictIn(districts)
                    } else {
                        emptyList()
                    }
                } else {
                    emptyList()
                }
            }
            else -> {
                // Инспектор видит осужденных своих районов
                val districtIds = (rbacAttrs["districtIds"] as? List<*>)
                    ?.filterIsInstance<Int>()
                    ?.map { it.toLong() } ?: emptyList()

                if (districtIds.isNotEmpty()) {
                    val districts = districtRepository.findAllById(districtIds)
                    clientRepository.findByDistrictIn(districts)
                } else {
                    emptyList()
                }
            }
        }
    }

    /**
     * Перевод осужденного в другой район
     */
    fun transferClient(clientId: Long, newDistrictId: Long): Client {
        val client = clientRepository.findById(clientId).orElseThrow {
            IllegalArgumentException("Client not found")
        }

        val newDistrict = districtRepository.findById(newDistrictId).orElseThrow {
            IllegalArgumentException("District not found")
        }

        val updated = client.copy(district = newDistrict)
        return clientRepository.save(updated)
    }

    /**
     * Обновление информации об осужденном
     */
    fun updateClient(
        clientId: Long,
        fio: String? = null,
        districtId: Long? = null,
        // можно добавить другие поля
    ): Client {
        val client = clientRepository.findById(clientId).orElseThrow {
            IllegalArgumentException("Client not found")
        }

        val district = districtId?.let {
            districtRepository.findById(it).orElseThrow {
                IllegalArgumentException("District not found")
            }
        }

        val updated = client.copy(
            fio = fio ?: client.fio,
            district = district ?: client.district
        )

        return clientRepository.save(updated)
    }

    // ============================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ============================================

    private fun getRbacAttributes(user: User): Map<String, Any> {
        val attributes = mutableMapOf<String, Any>()

        val userRoleFromJson = user.attributes?.get("role") as? String
        val isAdministrator = user.inn == "admin_user"
        val userType = user.userType

        val effectiveRole = when {
            isAdministrator -> "deptAdmin"
            userType == "probationer" -> "probationer"
            else -> userRoleFromJson ?: "inspector"
        }

        attributes["role"] = effectiveRole
        attributes["administrator"] = isAdministrator

        val mruIdFromModel = user.mruId
        val mruIdFromJson = user.attributes?.get("mruId") as? String
        val effectiveMruId = mruIdFromModel ?: mruIdFromJson

        if (effectiveMruId != null) {
            attributes["mruId"] = effectiveMruId
        }

        val districtIdsFromJson = user.attributes?.get("districtIds") as? List<*>
        if (districtIdsFromJson != null) {
            val districtIds = districtIdsFromJson.filterIsInstance<Int>()
            attributes["districtIds"] = districtIds
        } else {
            attributes["districtIds"] = emptyList<Int>()
        }

        return attributes
    }
}
