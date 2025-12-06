package com.example.probationbackend.service

import com.example.probationbackend.dto.RegistryCreateRequest
import com.example.probationbackend.model.Client
import com.example.probationbackend.model.Article
import com.example.probationbackend.repository.ClientRepository
import com.example.probationbackend.repository.ArticleRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
@Transactional
class RegistryService(
    private val clientRepository: ClientRepository,
    private val articleRepository: ArticleRepository,
    private val authService: AuthService,
    private val passwordEncoder: PasswordEncoder,
    private val traccarService: TraccarService,
    private val photoStorageService: PhotoStorageService,
    private val districtRepository: com.example.probationbackend.repository.DistrictRepository
) {

    fun createClient(request: RegistryCreateRequest, photoFile: MultipartFile? = null): Client {
        // 1. Проверяем, существует ли уже клиент с таким ИНН
        if (request.noInn != true && !request.inn.isNullOrBlank() && clientRepository.findByInn(request.inn).isPresent) {
            throw IllegalArgumentException("Client with INN ${request.inn} already exists")
        }

        // 1.5. Проверяем, что пароль указан при создании
        if (request.appPassword.isNullOrBlank()) {
            throw IllegalArgumentException("Password is required when creating a client")
        }

        // 2. Хешируем пароль для приложения
        val encodedAppPassword = passwordEncoder.encode(request.appPassword)

        // 3. Генерируем uniqueId для GPS трекинга (используем ИНН если есть)
        val uniqueId = if (request.noInn != true && !request.inn.isNullOrBlank()) {
            request.inn // Используем ИНН как uniqueId для GPS трекинга
        } else {
            generateIdentifier() // Если нет ИНН, генерируем случайный ID
        }

        // 3.5. Находим район по названию unit (если указан)
        val district = if (!request.unit.isNullOrBlank()) {
            districtRepository.findAll().firstOrNull {
                it.name.equals(request.unit, ignoreCase = true)
            }
        } else null

        // 4. Сохраняем клиента (осуждённого) в БД
        val client = Client(
            fio = request.fio,
            uniqueId = uniqueId, // КРИТИЧЕСКИ ВАЖНО: uniqueId для GPS трекинга
            inn = if (request.noInn != true) request.inn else null,
            identifier = generateIdentifier(), // Идентификатор для внутреннего использования
            unit = request.unit ?: "Не указано",
            obsType = request.obsType,
            birthDate = request.birthDate,
            age = request.age, // Сохраняем возраст
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
            extraInfo = request.extraInfo,
            measures = request.measures,
            appPassword = encodedAppPassword,
            photoKey = null,
            district = district // Устанавливаем район
        )
        val savedClient = clientRepository.save(client)

        // 4.5. Добавляем статьи осуждения
        if (!request.articles.isNullOrEmpty()) {
            request.articles.forEach { articleDto ->
                val article = Article(
                    article = articleDto.article,
                    part = articleDto.part,
                    point = articleDto.point,
                    client = savedClient
                )
                savedClient.articles.add(article)
            }
            clientRepository.save(savedClient)
        }

        var photoKey: String? = null
        // 5. Если пришло эталонное фото, сохраняем его
        if (photoFile != null && !photoFile.isEmpty) {
            // Используем uniqueId как ключ для фото
            val photoFileName = savedClient.uniqueId ?: "client_${savedClient.id}"
            photoKey = photoStorageService.storePhoto(photoFile, photoFileName, "reference_faces")
            if (photoKey != null) {
                // Обновляем клиента с ключом фото
                savedClient.photoKey = photoKey
                clientRepository.save(savedClient)
            }
        }

        // 6. Если ИНН указан, создаём пользователя и устройство в Traccar
        if (request.noInn != true && !request.inn.isNullOrBlank()) {
            val mruIdForUser = request.unit
            try {
                val user = authService.createUser(request.inn, request.appPassword, uniqueId,  userType = "probationer",  mruId = mruIdForUser )
                // 7. Создаём устройство в Traccar
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

    fun updateClient(id: Long, request: RegistryCreateRequest, photoFile: MultipartFile? = null): Client {
        val existingClient = clientRepository.findById(id).orElseThrow {
            IllegalArgumentException("Client not found with id: $id")
        }

        // Обновляем данные клиента
        var photoKey = existingClient.photoKey

        // Если загружено новое фото, сохраняем его
        if (photoFile != null && !photoFile.isEmpty) {
            val photoFileName = existingClient.uniqueId ?: "client_${existingClient.id}"
            photoKey = photoStorageService.storePhoto(photoFile, photoFileName, "reference_faces")
        }

        // Находим район по названию unit (если указан и изменился)
        val district = if (!request.unit.isNullOrBlank()) {
            districtRepository.findAll().firstOrNull {
                it.name.equals(request.unit, ignoreCase = true)
            }
        } else existingClient.district

        // Обновляем пароль только если он указан
        val updatedPassword = if (!request.appPassword.isNullOrBlank()) {
            passwordEncoder.encode(request.appPassword)
        } else {
            existingClient.appPassword // Сохраняем старый пароль
        }

        val updatedClient = existingClient.copy(
            fio = request.fio,
            inn = if (request.noInn != true) request.inn else null,
            obsType = request.obsType,
            birthDate = request.birthDate,
            age = request.age, // Обновляем возраст
            unit = request.unit ?: existingClient.unit,
            photoKey = photoKey ?: existingClient.photoKey,
            regAddress = request.regAddress,
            factAddress = request.factAddress,
            passport = request.passport,
            contact1 = request.contact1,
            contact2 = request.contact2,
            degree = request.degree,
            measures = request.measures,
            obsStart = request.obsStart,
            obsEnd = request.obsEnd,
            udNumber = request.udNumber,
            erpNumber = request.erpNumber,
            sex = request.sex,
            extraInfo = request.extraInfo,
            appPassword = updatedPassword, // Обновляем пароль только если он указан
            district = district // Обновляем район
        )

        // Обновляем статьи осуждения
        // Удаляем старые статьи
        articleRepository.deleteByClientId(id)
        updatedClient.articles.clear()

        // Добавляем новые статьи
        if (!request.articles.isNullOrEmpty()) {
            request.articles.forEach { articleDto ->
                val article = Article(
                    article = articleDto.article,
                    part = articleDto.part,
                    point = articleDto.point,
                    client = updatedClient
                )
                updatedClient.articles.add(article)
            }
        }

        return clientRepository.save(updatedClient)
    }

    fun deleteClient(id: Long) {
        val client = clientRepository.findById(id).orElseThrow {
            IllegalArgumentException("Client not found with id: $id")
        }

        println("=== STARTING CLIENT DELETION ===")
        println("Client ID: ${client.id}, INN: ${client.inn}, uniqueId: ${client.uniqueId}, FIO: ${client.fio}")

        // Удаляем устройство из Traccar, если есть uniqueId
        if (client.uniqueId != null) {
            try {
                traccarService.deleteDeviceByUniqueId(client.uniqueId)
                println("✓ Traccar device ${client.uniqueId} deleted successfully")
            } catch (e: Exception) {
                println("✗ Warning: Failed to delete Traccar device ${client.uniqueId}: ${e.message}")
            }
        }

        // Удаляем пользователя из таблицы users, если он там есть (для сотрудников)
        // Клиенты (осужденные) авторизуются из таблицы clients, поэтому для них это не нужно
        if (!client.inn.isNullOrBlank()) {
            try {
                authService.deleteUserByInn(client.inn)
                println("✓ User account with INN ${client.inn} deleted from users table")
            } catch (e: Exception) {
                // Это нормально для клиентов - у них нет записи в users
                println("ℹ No user account found in users table for INN ${client.inn} (expected for clients)")
            }
        }

        // Удаляем клиента из БД - ЭТО ГЛАВНОЕ для блокировки доступа клиента
        println("Deleting client from clients table...")
        clientRepository.deleteById(id)
        clientRepository.flush() // Принудительно фиксируем изменения
        println("✓ Client with ID ${client.id} and INN ${client.inn} DELETED from clients table")

        // Проверяем, что клиент действительно удалён
        val stillExists = clientRepository.findById(id).isPresent
        if (stillExists) {
            println("✗✗✗ ERROR: Client still exists in database after deletion!")
            throw IllegalStateException("Failed to delete client from database")
        } else {
            println("✓✓✓ VERIFIED: Client no longer exists in database")
        }

        println("=== CLIENT DELETION COMPLETED ===")
    }

    // Вспомогательный метод для генерации идентификатора
    private fun generateIdentifier(): String {
        // Пример: "04-" + случайные цифры
        val randomDigits = (1000..9999).random().toString()
        return "04-$randomDigits"
    }
}