package com.example.probationbackend.repository

import com.example.probationbackend.model.Article
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ArticleRepository : JpaRepository<Article, Long> {
    fun deleteByClientId(clientId: Long)
}
