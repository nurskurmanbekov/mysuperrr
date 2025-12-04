package com.example.probationbackend.controller

import com.example.probationbackend.model.User
import com.example.probationbackend.repository.UserRepository
import com.example.probationbackend.service.AdminService
import com.example.probationbackend.service.JwtTokenProvider
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin")
class AdminController(
    private val adminService: AdminService,
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository
) {

    // ============================================
    // ОРГАНИЗАЦИОННАЯ СТРУКТУРА
    // ============================================

    @GetMapping("/departments")
    fun getAllDepartments(): ResponseEntity<*> {
        val departments = adminService.getAllDepartments()
        return ResponseEntity.ok(departments)
    }

    @GetMapping("/mrus")
    fun getAllMrus(): ResponseEntity<*> {
        val mrus = adminService.getAllMrus()
        return ResponseEntity.ok(mrus)
    }

    @GetMapping("/districts")
    fun getAllDistricts(): ResponseEntity<*> {
        val districts = adminService.getAllDistricts()
        return ResponseEntity.ok(districts)
    }

    @GetMapping("/departments/{departmentId}/mrus")
    fun getMrusByDepartment(@PathVariable departmentId: Long): ResponseEntity<*> {
        return try {
            val mrus = adminService.getMrusByDepartment(departmentId)
            ResponseEntity.ok(mrus)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @GetMapping("/mrus/{mruId}/districts")
    fun getDistrictsByMru(@PathVariable mruId: Long): ResponseEntity<*> {
        return try {
            val districts = adminService.getDistrictsByMru(mruId)
            ResponseEntity.ok(districts)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    // ============================================
    // УПРАВЛЕНИЕ СОТРУДНИКАМИ
    // ============================================

    @PostMapping("/employees")
    fun createEmployee(
        @RequestHeader("Authorization") authHeader: String,
        @RequestBody request: CreateEmployeeRequest
    ): ResponseEntity<*> {
        val currentUser = getCurrentUser(authHeader) ?: return ResponseEntity.status(401).body(
            mapOf("error" to "Unauthorized")
        )

        if (!isDeptAdmin(currentUser)) {
            return ResponseEntity.status(403).body(
                mapOf("error" to "Доступ запрещён. Только Администратор департамента может создавать сотрудников.")
            )
        }

        return try {
            val employee = adminService.createEmployee(
                inn = request.inn,
                password = request.password,
                uniqueId = request.uniqueId,
                role = request.role,
                districtId = request.districtId,
                districtIds = request.districtIds
            )
            ResponseEntity.ok(employee)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @PutMapping("/employees/{employeeId}")
    fun updateEmployee(
        @RequestHeader("Authorization") authHeader: String,
        @PathVariable employeeId: Long,
        @RequestBody request: UpdateEmployeeRequest
    ): ResponseEntity<*> {
        val currentUser = getCurrentUser(authHeader) ?: return ResponseEntity.status(401).body(
            mapOf("error" to "Unauthorized")
        )

        if (!isDeptAdmin(currentUser)) {
            return ResponseEntity.status(403).body(
                mapOf("error" to "Доступ запрещён. Только Администратор департамента может редактировать сотрудников.")
            )
        }

        return try{
            val employee = adminService.updateEmployee(
                userId = employeeId,
                newDistrictId = request.districtId,
                newRole = request.role,
                newDistrictIds = request.districtIds
            )
            ResponseEntity.ok(employee)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @GetMapping("/employees")
    fun getEmployees(@RequestHeader("Authorization") authHeader: String): ResponseEntity<*> {
        val currentUser = getCurrentUser(authHeader) ?: return ResponseEntity.status(401).body(
            mapOf("error" to "Unauthorized")
        )

        val employees = adminService.getEmployees(currentUser)
        return ResponseEntity.ok(employees)
    }

    @DeleteMapping("/employees/{employeeId}")
    fun deleteEmployee(
        @RequestHeader("Authorization") authHeader: String,
        @PathVariable employeeId: Long
    ): ResponseEntity<*> {
        val currentUser = getCurrentUser(authHeader) ?: return ResponseEntity.status(401).body(
            mapOf("error" to "Unauthorized")
        )

        if (!isDeptAdmin(currentUser)) {
            return ResponseEntity.status(403).body(
                mapOf("error" to "Доступ запрещён. Только Администратор департамента может удалять сотрудников.")
            )
        }

        return try {
            adminService.deleteEmployee(employeeId)
            ResponseEntity.ok(mapOf("message" to "Employee deleted"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @PutMapping("/employees/{employeeId}/password")
    fun changeEmployeePassword(
        @RequestHeader("Authorization") authHeader: String,
        @PathVariable employeeId: Long,
        @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<*> {
        val currentUser = getCurrentUser(authHeader) ?: return ResponseEntity.status(401).body(
            mapOf("error" to "Unauthorized")
        )

        if (!isDeptAdmin(currentUser)) {
            return ResponseEntity.status(403).body(
                mapOf("error" to "Доступ запрещён. Только Администратор департамента может менять пароли.")
            )
        }

        return try {
            adminService.changeEmployeePassword(employeeId, request.newPassword)
            ResponseEntity.ok(mapOf("message" to "Password changed successfully"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    // ============================================
    // УПРАВЛЕНИЕ ОСУЖДЕННЫМИ
    // ============================================

    @GetMapping("/clients")
    fun getClients(@RequestHeader("Authorization") authHeader: String): ResponseEntity<*> {
        val currentUser = getCurrentUser(authHeader) ?: return ResponseEntity.status(401).body(
            mapOf("error" to "Unauthorized")
        )

        // ВАЖНО: Блокируем клиентов (осужденных) от доступа к веб-интерфейсу
        if (currentUser.userType == "probationer") {
            println("✗ ADMIN ACCESS BLOCKED: Client (probationer) ${currentUser.inn} attempted to access admin panel")
            return ResponseEntity.status(403).body(
                mapOf(
                    "message" to "Доступ запрещён. Клиенты могут использовать только мобильное приложение.",
                    "error" to "PROBATIONER_WEB_ACCESS_DENIED"
                )
            )
        }

        val clients = adminService.getClients(currentUser)
        return ResponseEntity.ok(clients)
    }

    @PutMapping("/clients/{clientId}/transfer")
    fun transferClient(
        @RequestHeader("Authorization") authHeader: String,
        @PathVariable clientId: Long,
        @RequestBody request: TransferClientRequest
    ): ResponseEntity<*> {
        val currentUser = getCurrentUser(authHeader) ?: return ResponseEntity.status(401).body(
            mapOf("error" to "Unauthorized")
        )

        return try {
            val client = adminService.transferClient(clientId, request.districtId)
            ResponseEntity.ok(client)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @PutMapping("/clients/{clientId}")
    fun updateClient(
        @RequestHeader("Authorization") authHeader: String,
        @PathVariable clientId: Long,
        @RequestBody request: UpdateClientRequest
    ): ResponseEntity<*> {
        val currentUser = getCurrentUser(authHeader) ?: return ResponseEntity.status(401).body(
            mapOf("error" to "Unauthorized")
        )

        return try {
            val client = adminService.updateClient(
                clientId = clientId,
                fio = request.fio,
                districtId = request.districtId
            )
            ResponseEntity.ok(client)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    // ============================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ============================================

    private fun getCurrentUser(authHeader: String?): User? {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null
        }
        val token = authHeader.substring(7)
        val userId = jwtTokenProvider.getUserIdFromToken(token) ?: return null
        return userRepository.findById(userId).orElse(null)
    }

    private fun isDeptAdmin(user: User): Boolean {
        val role = user.attributes?.get("role") as? String
        return role == "deptAdmin"
    }
}

// DTO классы

data class CreateEmployeeRequest(
    val inn: String,
    val password: String,
    val uniqueId: String,
    val role: String, // "deptAdmin", "mruAdmin", "inspector"
    val districtId: Long? = null,
    val districtIds: List<Long>? = null
)

data class UpdateEmployeeRequest(
    val districtId: Long? = null,
    val role: String? = null,
    val districtIds: List<Long>? = null
)

data class TransferClientRequest(
    val districtId: Long
)

data class UpdateClientRequest(
    val fio: String? = null,
    val districtId: Long? = null
)

data class ChangePasswordRequest(
    val newPassword: String
)
