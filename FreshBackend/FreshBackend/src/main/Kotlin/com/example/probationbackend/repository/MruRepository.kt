package com.example.probationbackend.repository

import com.example.probationbackend.model.Department
import com.example.probationbackend.model.Mru
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface MruRepository : JpaRepository<Mru, Long> {
    fun findByCode(code: String): Optional<Mru>
    fun findByDepartment(department: Department): List<Mru>
}
