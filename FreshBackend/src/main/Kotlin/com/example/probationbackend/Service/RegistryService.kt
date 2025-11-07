package com.example.probationbackend.service

import com.example.probationbackend.dto.RegistryCreateRequest
import com.example.probationbackend.model.Client
import com.example.probationbackend.repository.ClientRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
@Transactional
class RegistryService(
    private val clientRepository: ClientRepository,
    private val authService: AuthService,
    private val passwordEncoder: PasswordEncoder,
    private val traccarService: TraccarService,
    private val photoStorageService: PhotoStorageService
) {

    fun createClient(request: RegistryCreateRequest, photoFile: MultipartFile? = null): Client {
        // 1. Проверяем, существует ли уже клиент с таким ИНН
        if (request.noInn != true && request.inn != null && clientRepository.findByInn(request.inn).isPresent) {
            throw IllegalArgumentException("Client with INN ${request.inn} already exists")
        }

        // 2. Хешируем пароль для приложения
        val encodedAppPassword = passwordEncoder.encode(request.appPassword)

        // 3. Сохраняем клиента (осуждённого) в БД
        val client = Client(
            fio = request.fio,
            inn = if (request.noInn != true) request.inn else null, // ИСПРАВЛЕНО: request.inn может быть null
            identifier = generateIdentifier(), // ИСПРАВЛЕНО: создаем идентификатор
            unit = request.unit ?: "Не указано", // ИСПРАВЛЕНО: значение по умолчанию
            obsType = request.obsType,
            birthDate = request.birthDate,
            sex = request.sex,
            passport = request.passport,
            regAddress = request.regAddress,
            factAddress = request.factAddress,
            contact1 = request.contact1,
            contact2 = request.contact2,
            erpNumber = request.erpNumber,
            obsStart = request.obsStart,
            obsEnd = request.obsEnd,
            degree = request.degree,
            udNumber = request.udNumber,
            code = request.code,
            article = request.article,
            part = request.part,
            point = request.point,
            extraInfo = request.extraInfo,
            measures = request.measures,
            appPassword = encodedAppPassword,
            photoKey = null
        )
        val savedClient = clientRepository.save(client)

        var photoKey: String? = null
        // 4. Если пришло эталонное фото, сохраняем его
        if (photoFile != null && !photoFile.isEmpty) {
            // Используем ИНН как ключ для фото (или client.id если ИНН нет)
            val photoFileName = request.inn ?: "client_${savedClient.id}"
            photoKey = photoStorageService.storePhoto(photoFile, photoFileName, "reference_faces")
            if (photoKey != null) {
                // Обновляем клиента с ключом фото
                savedClient.photoKey = photoKey
                clientRepository.save(savedClient)
            }
        }

        // 5. Если ИНН указан, создаём пользователя и устройство в Traccar
        if (request.noInn != true && request.inn != null) {
            val mruIdForUser = request.unit
            val uniqueId = request.inn
            try {
                val user = authService.createUser(request.inn, request.appPassword, uniqueId,  userType = "probationer",  mruId = mruIdForUser )
                // 6. Создаём устройство в Traccar
                traccarService.createDevice(uniqueId, request.fio)
            } catch (e: Exception) {
                // Логируем ошибку, но не прерываем создание клиента
                println("Warning: Failed to create user/device for INN ${request.inn}: ${e.message}")
            }
        }

        return savedClient
    }

    fun findClientById(id: Long): Client? {
        return clientRepository.findById(id).orElse(null)
    }

    fun findAllClients(): List<Client> {
        return clientRepository.findAll()
    }

    fun findClientsByUnit(unit: String): List<Client> {
        return clientRepository.findAll() // TODO: Добавить метод в репозиторий для фильтрации
    }

    // Вспомогательный метод для генерации идентификатора
    private fun generateIdentifier(): String {
        // Пример: "04-" + случайные цифры
        val randomDigits = (1000..9999).random().toString()
        return "04-$randomDigits"
    }
}