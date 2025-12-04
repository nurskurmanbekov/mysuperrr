package com.example.probationbackend.controller

import com.example.probationbackend.service.PhotoStorageService
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/faces")
class FacePhotoController(
    private val photoStorageService: PhotoStorageService
) {

    // Serve photos with subDir in path: /api/faces/photos/{subDir}/{filename}
    @GetMapping("/photos/{subDir}/{filename:.+}")
    fun servePhotoWithSubDir(
        @PathVariable subDir: String,
        @PathVariable filename: String
    ): ResponseEntity<Resource> {
        return servePhoto("$subDir/$filename")
    }

    // Serve photos directly: /api/faces/{path}
    // This handles cases like /api/faces/reference_faces/12345.jpeg
    @GetMapping("/{path:.*}")
    fun servePhotoDirectPath(
        @PathVariable path: String
    ): ResponseEntity<Resource> {
        return servePhoto(path)
    }

    private fun servePhoto(path: String): ResponseEntity<Resource> {
        try {
            println("DEBUG FacePhotoController: Serving photo - path: $path")

            // Split path into subDir and filename
            val lastSlash = path.lastIndexOf('/')
            val subDir: String?
            val filename: String

            if (lastSlash > 0) {
                subDir = path.substring(0, lastSlash)
                filename = path.substring(lastSlash + 1)
            } else {
                subDir = null
                filename = path
            }

            println("DEBUG FacePhotoController: subDir: $subDir, filename: $filename")

            val resource = photoStorageService.loadPhotoAsResource(filename, subDir)

            if (resource == null || !resource.exists()) {
                println("ERROR FacePhotoController: Photo not found - $path")
                return ResponseEntity.notFound().build()
            }

            // Determine content type based on file extension
            val contentType = when {
                filename.endsWith(".jpg", ignoreCase = true) || filename.endsWith(".jpeg", ignoreCase = true) -> MediaType.IMAGE_JPEG
                filename.endsWith(".png", ignoreCase = true) -> MediaType.IMAGE_PNG
                else -> MediaType.APPLICATION_OCTET_STREAM
            }

            println("DEBUG FacePhotoController: Successfully serving photo - $path")
            return ResponseEntity.ok()
                .contentType(contentType)
                .body(resource)
        } catch (e: Exception) {
            println("ERROR FacePhotoController: Error serving photo - ${e.message}")
            e.printStackTrace()
            return ResponseEntity.status(500).build()
        }
    }
}
