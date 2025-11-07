package com.example.probationbackend.repository

import com.example.probationbackend.model.District
import com.example.probationbackend.model.Mru
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface DistrictRepository : JpaRepository<District, Long> {
    fun findByCode(code: String): Optional<District>
    fun findByMru(mru: Mru): List<District>
    fun findByMruIn(mrus: List<Mru>): List<District>
}
