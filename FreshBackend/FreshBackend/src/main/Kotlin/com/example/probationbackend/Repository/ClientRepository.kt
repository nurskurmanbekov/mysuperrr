package com.example.probationbackend.repository

import com.example.probationbackend.model.Client
import com.example.probationbackend.model.District
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ClientRepository : JpaRepository<Client, Long> {
    fun findByInn(inn: String): Optional<Client>
    fun findByUniqueId(uniqueId: String): Optional<Client>
    fun findByDistrict(district: District): List<Client>
    fun findByDistrictIn(districts: List<District>): List<Client>
    fun findByFioContainingIgnoreCase(fio: String): List<Client>
}