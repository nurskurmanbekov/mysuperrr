package com.example.probationbackend.service

import com.example.probationbackend.model.FaceCheckEvent
import com.example.probationbackend.repository.FaceCheckEventRepository
import com.example.probationbackend.repository.UserRepository
import org.bytedeco.javacpp.FloatPointer
import org.bytedeco.javacpp.IntPointer
import org.bytedeco.opencv.global.opencv_imgcodecs.imread
import org.bytedeco.opencv.global.opencv_imgproc.*
import org.bytedeco.opencv.global.opencv_core.*
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

            // Проверка размеров лиц для дополнительной безопасности
            val knownFaceRect = knownFaces.get(0)
            val selfieFaceRect = selfieFaces.get(0)

            val knownFaceArea = knownFaceRect.width() * knownFaceRect.height()
            val selfieFaceArea = selfieFaceRect.width() * selfieFaceRect.height()
            val faceAreaRatio = Math.max(knownFaceArea, selfieFaceArea).toDouble() /
                               Math.min(knownFaceArea, selfieFaceArea).toDouble()

            println("═══════════════════════════════════════════")
            println("🔍 FACE ID VERIFICATION (User) - STRICT ALGORITHM")
            println("User ID: ${user.uniqueId}")
            println("Known face size: ${knownFaceRect.width()}x${knownFaceRect.height()} (area: $knownFaceArea)")
            println("Selfie face size: ${selfieFaceRect.width()}x${selfieFaceRect.height()} (area: $selfieFaceArea)")
            println("Face area ratio: %.2f".format(faceAreaRatio))

            // Проверка 1: Размеры лиц не должны слишком отличаться
            if (faceAreaRatio > 2.5) {
                println("❌ REJECTED: Face size difference too large (ratio: %.2f > 2.5)".format(faceAreaRatio))
                println("═══════════════════════════════════════════")
                return FaceVerificationResult(false,
                    "Размер лица слишком отличается от эталона. Приблизьтесь или отдалитесь от камеры.",
                    null)
            }

            val knownHistogram = calculateHistogram(knownMat, knownFaceRect)
            val selfieHistogram = calculateHistogram(selfieMat, selfieFaceRect)

            // Проверка 2: Сравнение по Bhattacharyya distance (основной метод)
            val bhattacharyyaDistance = compareHistograms(knownHistogram, selfieHistogram)

            // Проверка 3: Дополнительное сравнение по корреляции
            val correlationDistance = 1.0 - org.bytedeco.opencv.global.opencv_imgproc.compareHist(
                knownHistogram, selfieHistogram,
                org.bytedeco.opencv.global.opencv_imgproc.HISTCMP_CORREL
            )

            // КРИТИЧНО: Строгий порог для безопасности!
            // Bhattacharyya distance: 0 = идентичны, 1 = полностью разные
            // Используем СТРОГИЙ порог 0.20 вместо 0.45
            val bhattacharyyaTolerance = 0.20  // СТРОГИЙ порог - только очень похожие лица
            val correlationTolerance = 0.35     // Дополнительная проверка

            val bhattacharyyaMatch = bhattacharyyaDistance <= bhattacharyyaTolerance
            val correlationMatch = correlationDistance <= correlationTolerance
            val match = bhattacharyyaMatch && correlationMatch  // ОБА теста должны пройти!

            println("Bhattacharyya distance: %.4f (tolerance: $bhattacharyyaTolerance) - ${if (bhattacharyyaMatch) "✓ PASS" else "✗ FAIL"}".format(bhattacharyyaDistance))
            println("Correlation distance: %.4f (tolerance: $correlationTolerance) - ${if (correlationMatch) "✓ PASS" else "✗ FAIL"}".format(correlationDistance))
            println("Algorithm: Dual-check (3-channel HSV + Correlation)")
            println("Final Result: ${if (match) "✅ MATCH (both checks passed)" else "❌ NO MATCH"}")
            println("═══════════════════════════════════════════")

            traccarService.updateFaceIdAttributes(user.uniqueId, match, bhattacharyyaDistance,
                if (match) "Лицо распознано успешно" else "Лицо не распознано")

            val faceCheckEvent = FaceCheckEvent(
                userId = user.id!!,
                deviceId = null,
                outcome = if (match) "ok" else "failed",
                takenAt = LocalDateTime.now(),
                distance = bhattacharyyaDistance,
                checkId = null,
                deadlineIso = null,
                appVersion = null
            )
            faceCheckEventRepository.save(faceCheckEvent)

            return FaceVerificationResult(match,
                if (match) "Лицо распознано успешно" else "Лицо не распознано",
                bhattacharyyaDistance)

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

            // Проверка размеров лиц для дополнительной безопасности
            val knownFaceRect = knownFaces.get(0)
            val selfieFaceRect = selfieFaces.get(0)

            val knownFaceArea = knownFaceRect.width() * knownFaceRect.height()
            val selfieFaceArea = selfieFaceRect.width() * selfieFaceRect.height()
            val faceAreaRatio = Math.max(knownFaceArea, selfieFaceArea).toDouble() /
                               Math.min(knownFaceArea, selfieFaceArea).toDouble()

            println("═══════════════════════════════════════════")
            println("🔍 FACE ID VERIFICATION (Client) - STRICT ALGORITHM")
            println("Client ID: ${client.id}, INN: ${client.inn}, uniqueId: ${client.uniqueId}")
            println("Known face size: ${knownFaceRect.width()}x${knownFaceRect.height()} (area: $knownFaceArea)")
            println("Selfie face size: ${selfieFaceRect.width()}x${selfieFaceRect.height()} (area: $selfieFaceArea)")
            println("Face area ratio: %.2f".format(faceAreaRatio))

            // Проверка 1: Размеры лиц не должны слишком отличаться
            if (faceAreaRatio > 2.5) {
                println("❌ REJECTED: Face size difference too large (ratio: %.2f > 2.5)".format(faceAreaRatio))
                println("═══════════════════════════════════════════")
                return FaceVerificationResult(false,
                    "Размер лица слишком отличается от эталона. Приблизьтесь или отдалитесь от камеры.",
                    null)
            }

            val knownHistogram = calculateHistogram(knownMat, knownFaceRect)
            val selfieHistogram = calculateHistogram(selfieMat, selfieFaceRect)

            // Проверка 2: Сравнение по Bhattacharyya distance (основной метод)
            val bhattacharyyaDistance = compareHistograms(knownHistogram, selfieHistogram)

            // Проверка 3: Дополнительное сравнение по корреляции
            val correlationDistance = 1.0 - org.bytedeco.opencv.global.opencv_imgproc.compareHist(
                knownHistogram, selfieHistogram,
                org.bytedeco.opencv.global.opencv_imgproc.HISTCMP_CORREL
            )

            // КРИТИЧНО: Строгий порог для безопасности!
            // Bhattacharyya distance: 0 = идентичны, 1 = полностью разные
            // Используем СТРОГИЙ порог 0.20 вместо 0.45
            val bhattacharyyaTolerance = 0.20  // СТРОГИЙ порог - только очень похожие лица
            val correlationTolerance = 0.35     // Дополнительная проверка

            val bhattacharyyaMatch = bhattacharyyaDistance <= bhattacharyyaTolerance
            val correlationMatch = correlationDistance <= correlationTolerance
            val match = bhattacharyyaMatch && correlationMatch  // ОБА теста должны пройти!

            println("Bhattacharyya distance: %.4f (tolerance: $bhattacharyyaTolerance) - ${if (bhattacharyyaMatch) "✓ PASS" else "✗ FAIL"}".format(bhattacharyyaDistance))
            println("Correlation distance: %.4f (tolerance: $correlationTolerance) - ${if (correlationMatch) "✓ PASS" else "✗ FAIL"}".format(correlationDistance))
            println("Algorithm: Dual-check (3-channel HSV + Correlation)")
            println("Final Result: ${if (match) "✅ MATCH (both checks passed)" else "❌ NO MATCH"}")
            println("═══════════════════════════════════════════")

            // Обновляем атрибуты Traccar для клиента (если есть uniqueId)
            if (client.uniqueId != null) {
                traccarService.updateFaceIdAttributes(client.uniqueId!!, match, bhattacharyyaDistance,
                    if (match) "Лицо распознано успешно" else "Лицо не распознано")
            }

            // Сохраняем событие проверки лица
            val faceCheckEvent = FaceCheckEvent(
                userId = client.id!!,
                deviceId = null,
                outcome = if (match) "ok" else "failed",
                takenAt = LocalDateTime.now(),
                distance = bhattacharyyaDistance,
                checkId = null,
                deadlineIso = null,
                appVersion = null
            )
            faceCheckEventRepository.save(faceCheckEvent)

            return FaceVerificationResult(match,
                if (match) "Лицо распознано успешно" else "Лицо не распознано",
                bhattacharyyaDistance)

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

        // Используем HSV для анализа цвета
        val hsvMat = Mat()
        cvtColor(faceMat, hsvMat, COLOR_BGR2HSV)

        // ВАЖНО: Вычисляем гистограммы для ВСЕХ трёх каналов (H, S, V)
        // Раньше использовался только канал H - это слишком слабо!
        val histH = Mat()
        val histS = Mat()
        val histV = Mat()

        val channelsH = IntPointer(0) // Hue (оттенок)
        val channelsS = IntPointer(1) // Saturation (насыщенность)
        val channelsV = IntPointer(2) // Value (яркость)

        val histSize = IntPointer(50)
        val rangesH = FloatPointer(0f, 180f)
        val rangesS = FloatPointer(0f, 256f)
        val rangesV = FloatPointer(0f, 256f)

        val matVector = MatVector(1)
        matVector.put(0, hsvMat)

        // Вычисляем гистограммы для каждого канала
        calcHist(matVector, channelsH, Mat(), histH, histSize, rangesH)
        calcHist(matVector, channelsS, Mat(), histS, histSize, rangesS)
        calcHist(matVector, channelsV, Mat(), histV, histSize, rangesV)

        // Нормализуем гистограммы
        normalize(histH, histH, 0.0, 1.0, NORM_MINMAX, -1, Mat())
        normalize(histS, histS, 0.0, 1.0, NORM_MINMAX, -1, Mat())
        normalize(histV, histV, 0.0, 1.0, NORM_MINMAX, -1, Mat())

        // Объединяем все три гистограммы в одну
        val combinedHist = Mat()
        org.bytedeco.opencv.global.opencv_core.vconcat(MatVector(histH, histS, histV), combinedHist)

        return combinedHist
    }

    private fun compareHistograms(hist1: Mat, hist2: Mat): Double {
        return compareHist(hist1, hist2, HISTCMP_BHATTACHARYYA)
    }

    data class FaceVerificationResult(val success: Boolean, val message: String, val distance: Double?)
}