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
        val user = userRepository.findByUniqueId(userId).orElse(null)
            ?: return FaceVerificationResult(false, "Пользователь не найден", null)

        val knownFaceResource = photoStorageService.loadPhotoAsResource(user.uniqueId, "reference_faces")
        if (knownFaceResource == null) {
            val knownFaceFile = File("./faces", "${user.uniqueId}.jpg")
            if (!knownFaceFile.exists()) {
                return FaceVerificationResult(false, "Нет эталона для этого пользователя", null)
            }
            return performVerification(knownFaceFile, selfieFile, user)
        } else {
            val tempKnownFaceFile = File.createTempFile("known_face_", ".jpg")
            try {
                knownFaceResource.inputStream.use { input ->
                    FileOutputStream(tempKnownFaceFile).use { output ->
                        input.copyTo(output)
                    }
                }
                return performVerification(tempKnownFaceFile, selfieFile, user)
            } finally {
                tempKnownFaceFile.delete()
            }
        }
    }

    private fun performVerification(knownFaceFile: File, selfieFile: File, user: com.example.probationbackend.model.User): FaceVerificationResult {
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

            val tolerance = 0.6
            val match = distance <= tolerance

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