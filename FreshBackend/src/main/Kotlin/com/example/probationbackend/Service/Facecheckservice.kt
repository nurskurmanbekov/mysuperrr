package com.example.probationbackend.service

import com.example.probationbackend.model.FaceCheckEvent
import com.example.probationbackend.repository.FaceCheckEventRepository
import com.example.probationbackend.repository.UserRepository
import org.bytedeco.javacpp.FloatPointer
import org.bytedeco.javacpp.IntPointer
import org.bytedeco.opencv.global.opencv_imgcodecs.imread
import org.bytedeco.opencv.global.opencv_imgproc.*
import org.bytedeco.opencv.opencv_core.*
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime

@Service
@Transactional
class FaceCheckService(
    private val faceCheckEventRepository: FaceCheckEventRepository,
    private val userRepository: UserRepository,
    private val clientRepository: com.example.probationbackend.repository.ClientRepository,
    private val traccarService: TraccarService,
    private val photoStorageService: PhotoStorageService
) {

    private val faceCascade: CascadeClassifier = CascadeClassifier().apply {
        val resource = ClassPathResource("haarcascade_frontalface_default.xml")
        val tempFile = File.createTempFile("haarcascade", ".xml")
        resource.inputStream.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        load(tempFile.absolutePath)
        tempFile.delete()
    }

    fun verifyFace(userId: String, selfieFile: File): FaceVerificationResult {
        // Сначала ищем в users (администраторы), затем в clients (осуждённые)
        val user = userRepository.findByUniqueId(userId).orElse(null)
        if (user != null) {
            // Проверка для администратора
            val knownFaceResource = photoStorageService.loadPhotoAsResource(user.uniqueId, "reference_faces")
            if (knownFaceResource == null) {
                val knownFaceFile = File("./faces", "${user.uniqueId}.jpg")
                if (!knownFaceFile.exists()) {
                    return FaceVerificationResult(false, "Нет эталона для этого пользователя", null)
                }
                return performVerificationForUser(knownFaceFile, selfieFile, user)
            } else {
                val tempKnownFaceFile = File.createTempFile("known_face_", ".jpg")
                try {
                    knownFaceResource.inputStream.use { input ->
                        FileOutputStream(tempKnownFaceFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    return performVerificationForUser(tempKnownFaceFile, selfieFile, user)
                } finally {
                    tempKnownFaceFile.delete()
                }
            }
        }

        // Если не найден в users, ищем в clients
        val client = clientRepository.findByUniqueId(userId).orElse(null)
            ?: return FaceVerificationResult(false, "Пользователь не найден", null)

        println("DEBUG FaceID: Found client with uniqueId=${client.uniqueId}, id=${client.id}, photoKey=${client.photoKey}")

        // Проверка для клиента (осуждённого)
        // Используем photoKey из клиента для поиска эталонного фото
        if (client.photoKey.isNullOrBlank()) {
            println("ERROR FaceID: Client ${client.id} has no photoKey set")
            return FaceVerificationResult(false, "Нет эталонного фото для этого пользователя. Обратитесь к администратору для загрузки фото.", null)
        }

        // photoKey имеет формат "reference_faces/filename.ext", нам нужен только filename.ext
        val photoFileName = client.photoKey!!.substringAfterLast('/')
        println("DEBUG FaceID: Looking for photo file: $photoFileName in reference_faces")

        val knownFaceResource = photoStorageService.loadPhotoAsResource(photoFileName, "reference_faces")
        if (knownFaceResource == null) {
            println("ERROR FaceID: Failed to load photo resource for $photoFileName")
            return FaceVerificationResult(false, "Не удалось загрузить эталонное фото. Обратитесь к администратору.", null)
        }
        println("DEBUG FaceID: Successfully loaded photo resource")

        val tempKnownFaceFile = File.createTempFile("known_face_", ".jpg")
        try {
            knownFaceResource.inputStream.use { input ->
                FileOutputStream(tempKnownFaceFile).use { output ->
                    input.copyTo(output)
                }
            }
            return performVerificationForClient(tempKnownFaceFile, selfieFile, client)
        } finally {
            tempKnownFaceFile.delete()
        }
    }

    private fun performVerificationForUser(knownFaceFile: File, selfieFile: File, user: com.example.probationbackend.model.User): FaceVerificationResult {
        try {
            val knownMat = imread(knownFaceFile.absolutePath)
            val selfieMat = imread(selfieFile.absolutePath)

            if (knownMat.empty() || selfieMat.empty()) {
                return FaceVerificationResult(false, "Не удалось загрузить изображения", null)
            }

            val knownFaces = detectFaces(knownMat)
            val selfieFaces = detectFaces(selfieMat)

            // ИСПРАВЛЕНО: используем 0L вместо 0 для Long сравнения
            if (knownFaces.size() == 0L || selfieFaces.size() == 0L) {
                return FaceVerificationResult(false, "Лицо не найдено на фото", null)
            }

            val knownHistogram = calculateHistogram(knownMat, knownFaces.get(0))
            val selfieHistogram = calculateHistogram(selfieMat, selfieFaces.get(0))
            val distance = compareHistograms(knownHistogram, selfieHistogram)

            // ВАЖНО: Уменьшен порог для более строгой верификации
            // Bhattacharyya distance: 0 = идентичны, 1 = полностью разные
            val tolerance = 0.35  // Было 0.6 - слишком слабая верификация
            val match = distance <= tolerance

            println("═══════════════════════════════════════════")
            println("🔍 FACE ID VERIFICATION (User)")
            println("User ID: ${user.uniqueId}")
            println("Distance: %.4f".format(distance))
            println("Tolerance: $tolerance")
            println("Result: ${if (match) "✅ MATCH" else "❌ NO MATCH"}")
            println("═══════════════════════════════════════════")

            traccarService.updateFaceIdAttributes(user.uniqueId, match, distance,
                if (match) "Лицо распознано успешно" else "Лицо не распознано")

            val faceCheckEvent = FaceCheckEvent(
                userId = user.id!!,
                deviceId = null,
                outcome = if (match) "ok" else "failed",
                takenAt = LocalDateTime.now(),
                distance = distance,
                checkId = null,
                deadlineIso = null,
                appVersion = null
            )
            faceCheckEventRepository.save(faceCheckEvent)

            return FaceVerificationResult(match,
                if (match) "Лицо распознано успешно" else "Лицо не распознано",
                distance)

        } catch (e: Exception) {
            e.printStackTrace()
            return FaceVerificationResult(false, "Ошибка при обработке изображений: ${e.message}", null)
        }
    }

    private fun performVerificationForClient(knownFaceFile: File, selfieFile: File, client: com.example.probationbackend.model.Client): FaceVerificationResult {
        try {
            val knownMat = imread(knownFaceFile.absolutePath)
            val selfieMat = imread(selfieFile.absolutePath)

            if (knownMat.empty() || selfieMat.empty()) {
                return FaceVerificationResult(false, "Не удалось загрузить изображения", null)
            }

            val knownFaces = detectFaces(knownMat)
            val selfieFaces = detectFaces(selfieMat)

            if (knownFaces.size() == 0L || selfieFaces.size() == 0L) {
                return FaceVerificationResult(false, "Лицо не найдено на фото", null)
            }

            val knownHistogram = calculateHistogram(knownMat, knownFaces.get(0))
            val selfieHistogram = calculateHistogram(selfieMat, selfieFaces.get(0))
            val distance = compareHistograms(knownHistogram, selfieHistogram)

            // ВАЖНО: Уменьшен порог для более строгой верификации
            // Bhattacharyya distance: 0 = идентичны, 1 = полностью разные
            val tolerance = 0.35  // Было 0.6 - слишком слабая верификация
            val match = distance <= tolerance

            println("═══════════════════════════════════════════")
            println("🔍 FACE ID VERIFICATION (Client)")
            println("Client ID: ${client.id}, INN: ${client.inn}, uniqueId: ${client.uniqueId}")
            println("Distance: %.4f".format(distance))
            println("Tolerance: $tolerance")
            println("Result: ${if (match) "✅ MATCH" else "❌ NO MATCH"}")
            println("═══════════════════════════════════════════")

            // Обновляем атрибуты Traccar для клиента (если есть uniqueId)
            if (client.uniqueId != null) {
                traccarService.updateFaceIdAttributes(client.uniqueId!!, match, distance,
                    if (match) "Лицо распознано успешно" else "Лицо не распознано")
            }

            // Сохраняем событие проверки лица
            val faceCheckEvent = FaceCheckEvent(
                userId = client.id!!,
                deviceId = null,
                outcome = if (match) "ok" else "failed",
                takenAt = LocalDateTime.now(),
                distance = distance,
                checkId = null,
                deadlineIso = null,
                appVersion = null
            )
            faceCheckEventRepository.save(faceCheckEvent)

            return FaceVerificationResult(match,
                if (match) "Лицо распознано успешно" else "Лицо не распознано",
                distance)

        } catch (e: Exception) {
            e.printStackTrace()
            return FaceVerificationResult(false, "Ошибка при обработке изображений: ${e.message}", null)
        }
    }

    private fun detectFaces(mat: Mat): RectVector {
        val faces = RectVector()
        val grayMat = Mat()
        cvtColor(mat, grayMat, COLOR_BGR2GRAY)
        equalizeHist(grayMat, grayMat)
        faceCascade.detectMultiScale(grayMat, faces)
        return faces
    }

    private fun calculateHistogram(mat: Mat, rect: Rect): Mat {
        val faceMat = Mat(mat, rect)
        val hsvMat = Mat()
        cvtColor(faceMat, hsvMat, COLOR_BGR2HSV)
        val hist = Mat()
        val channels = IntPointer(0)
        val histSize = IntPointer(50)
        val ranges = FloatPointer(0f, 180f)

        val matVector = MatVector(1)
        matVector.put(0, hsvMat)
        calcHist(matVector, channels, Mat(), hist, histSize, ranges)

        return hist
    }

    private fun compareHistograms(hist1: Mat, hist2: Mat): Double {
        return compareHist(hist1, hist2, HISTCMP_BHATTACHARYYA)
    }

    data class FaceVerificationResult(val success: Boolean, val message: String, val distance: Double?)
}