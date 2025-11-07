package com.example.probationbackend.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

@Service
class PhotoStorageService(
    @Value("\${app.photos.storage-dir:./photos}") private val storageDir: String
) {

    init {
        val dir = File(storageDir)
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }

    fun storePhoto(file: MultipartFile, key: String, subDir: String? = null): String? {
        if (file.isEmpty) {
            return null
        }

        val targetDir = if (subDir != null) File(storageDir, subDir) else File(storageDir)
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val fileName = "$key.${getExtension(file.originalFilename)}"
        val filePath = File(targetDir, fileName)

        try {
            file.transferTo(filePath)
            // Возвращаем относительный путь или ключ для доступа
            return if (subDir != null) "$subDir/$fileName" else fileName
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    fun loadPhotoAsResource(key: String, subDir: String? = null): org.springframework.core.io.Resource? {
        val fileName = key // Предполагаем, что ключ уже содержит расширение
        val filePath = if (subDir != null) Paths.get(storageDir, subDir, fileName) else Paths.get(storageDir, fileName)
        return try {
            val resource = org.springframework.core.io.UrlResource(filePath.toUri())
            if (resource.exists() && resource.isReadable) {
                resource
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getExtension(filename: String?): String {
        return filename?.substringAfterLast('.', "jpg") ?: "jpg" // по умолчанию jpg
    }
}