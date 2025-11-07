package com.example.probationbackend.controller

import com.example.probationbackend.service.PhotoStorageService
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Files
import java.nio.file.Paths

@RestController
@RequestMapping("/api/faces") // или /api/photos, как удобнее
class PhotoController(
    private val photoStorageService: PhotoStorageService
) {

    @GetMapping("/{key:.+}") // Регулярка позволяет захватить имя файла с расширением
    fun getPhoto(@PathVariable key: String): ResponseEntity<Resource> {
        // Пытаемся загрузить фото из хранилища (например, из папки reference_faces)
        val resource: Resource? = photoStorageService.loadPhotoAsResource(key, "reference_faces")

        if (resource != null && resource.exists()) {
            // Определяем тип контента (JPEG, PNG и т.д.) - Попробуем через путь к файлу
            val contentType = try {
                val filePath = Paths.get(resource.uri.toString())
                val detectedType = Files.probeContentType(filePath) // Может вернуть null
                MediaType.valueOf(detectedType ?: "application/octet-stream")
            } catch (e: Exception) {
                // Если не удалось определить через Files, возвращаем по умолчанию
                MediaType.IMAGE_JPEG // или application/octet-stream
            }

            return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CACHE_CONTROL, "max-age=3600") // Кеширование на 1 час (по желанию)
                .body(resource)
        } else {
            // Фото не найдено
            return ResponseEntity.notFound().build()
        }
    }
}