# 🎯 Полная инструкция по установке CompreFace

## 1. Запуск CompreFace

```bash
# Перейдите в директорию проекта
cd /home/user/mysuperrr

# Запустите CompreFace
docker-compose -f docker-compose-compreface.yml up -d

# Проверьте статус контейнеров
docker-compose -f docker-compose-compreface.yml ps

# Просмотр логов (если нужно)
docker-compose -f docker-compose-compreface.yml logs -f
```

## 2. Доступ к CompreFace UI

После запуска откройте браузер:
- URL: **http://localhost:8000**
- Первый запуск может занять 2-3 минуты (загрузка моделей ML)

## 3. Регистрация и получение API Key

### Шаг 1: Создайте аккаунт
1. Откройте http://localhost:8000
2. Нажмите "Sign Up"
3. Заполните форму:
   - Email: ваш email
   - Password: придумайте пароль
4. Нажмите "Sign Up"

### Шаг 2: Войдите в систему
1. Введите созданные credentials
2. Нажмите "Sign In"

### Шаг 3: Создайте Application
1. В главном меню нажмите "Create Application"
2. Введите название: **"Probation App"**
3. Нажмите "Create"

### Шаг 4: Создайте Recognition Service
1. В созданном приложении нажмите "Add Service"
2. Выберите тип: **"Recognition Service"**
3. Введите название: **"Face Recognition"**
4. Нажмите "Create"

### Шаг 5: Получите API Key
1. В созданном сервисе найдите раздел **"API Key"**
2. Скопируйте ключ (формат: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`)
3. **СОХРАНИТЕ ЕГО** - он понадобится для настройки

## 4. Настройка Backend

Откройте файл:
`FreshBackend/src/main/kotlin/com/example/probationbackend/controller/CompreFaceController.kt`

Найдите строку 24 и замените:
```kotlin
private val apiKey = "ВАШ_API_KEY_СЮДА"
```

## 5. Настройка Mobile App

Откройте файл:
`iosandroid/src/utils/constants.js`

Найдите строку 20 и замените:
```javascript
API_KEY: 'ВАШ_API_KEY_СЮДА',
```

## 6. Если CompreFace на другом сервере

### Если CompreFace на удаленном сервере или через ngrok:

**Backend** (`CompreFaceController.kt` строка 23):
```kotlin
private val compreFaceUrl = "http://ВАШ_СЕРВЕР:8000"
```

**Mobile** (`constants.js` строка 18):
```javascript
BASE_URL: 'https://ваш-ngrok-url.ngrok-free.dev/compreface',
```

## 7. Проверка работоспособности

### Тест через curl:
```bash
# Замените YOUR_API_KEY на ваш ключ
curl -X GET "http://localhost:8000/api/v1/recognition/faces" \
  -H "x-api-key: YOUR_API_KEY"

# Должен вернуть: {"faces":[]}
```

### Тест через браузер:
1. Откройте CompreFace UI (http://localhost:8000)
2. Перейдите в ваш сервис
3. Нажмите "Test"
4. Загрузите фото лица
5. Должно успешно распознать

## 8. Остановка и управление

```bash
# Остановить CompreFace
docker-compose -f docker-compose-compreface.yml down

# Остановить и удалить данные
docker-compose -f docker-compose-compreface.yml down -v

# Перезапустить
docker-compose -f docker-compose-compreface.yml restart

# Обновить образы
docker-compose -f docker-compose-compreface.yml pull
docker-compose -f docker-compose-compreface.yml up -d
```

## 9. Требования к системе

- **RAM**: минимум 8GB (рекомендуется 16GB)
- **Disk**: минимум 10GB свободного места
- **Docker**: версия 20.10+
- **Docker Compose**: версия 1.29+

## 10. Troubleshooting

### CompreFace не запускается:
```bash
# Проверьте логи
docker-compose -f docker-compose-compreface.yml logs compreface-core

# Проверьте порты
netstat -tulpn | grep 8000
```

### Медленная работа:
- Увеличьте RAM для Docker (минимум 8GB)
- Проверьте что модели загружены (первый запуск долгий)

### Ошибка подключения к БД:
```bash
# Перезапустите PostgreSQL
docker-compose -f docker-compose-compreface.yml restart compreface-postgres-db
```

## 11. Полезные ссылки

- Документация: https://github.com/exadel-inc/CompreFace
- API Reference: http://localhost:8000/api/v1/docs
- Swagger UI: http://localhost:8000/swagger-ui.html

## 12. Следующие шаги после установки

1. ✅ Запустите CompreFace
2. ✅ Создайте аккаунт и получите API Key
3. ✅ Обновите API Key в backend и mobile app
4. ✅ Перезапустите backend
5. ✅ Пересоберите mobile app
6. ✅ Протестируйте Face ID в приложении
