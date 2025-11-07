package com.example.probationbackend.repository

import com.example.probationbackend.model.Department
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface DepartmentRepository : JpaRepository<Department, Long> {
    fun findByCode(code: String): Optional<Department>
}
