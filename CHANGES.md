# Изменения в проекте мониторинга осужденных

## Обзор

Проект был полностью доработан с добавлением следующих функций:
1. **Геозоны** - создание зон для осужденных с уведомлениями при выходе
2. **Оффлайн GPS** - сохранение GPS данных без интернета и автоматическая синхронизация
3. **RBAC система** - иерархическая структура: Департамент → МРУ → Районы
4. **Админ панель** - управление сотрудниками и осужденными

---

## 1. БЭКЕНД (Spring Boot)

### Новые модели данных

#### Организационная структура
- **Department** - Центральный департамент
- **Mru** - Межрегиональные управления (4 шт)
- **District** - Районы (~50 шт, по 12-13 на МРУ)

#### Геозоны
- **GeoZone** - Полигоны для ограничения передвижения
  - `polygonCoordinates` - координаты в формате JSONB
  - `isActive` - активность геозоны
- **GeoZoneViolation** - Нарушения геозон
  - `violationType` - тип нарушения (EXIT/ENTRY)
  - `notificationSent` - флаг отправки уведомления

### Новые API endpoints

#### Административные (/api/admin)
```
GET    /departments               - Список департаментов
GET    /mrus                      - Список МРУ
GET    /districts                 - Список районов
GET    /departments/{id}/mrus     - МРУ департамента
GET    /mrus/{id}/districts       - Районы МРУ

POST   /employees                 - Создание сотрудника
PUT    /employees/{id}            - Обновление/перевод сотрудника
GET    /employees                 - Список сотрудников (с учетом RBAC)
DELETE /employees/{id}            - Удаление сотрудника

GET    /clients                   - Список осужденных (с учетом RBAC)
PUT    /clients/{id}/transfer     - Перевод осужденного в другой район
PUT    /clients/{id}              - Обновление данных осужденного
```

#### Геозоны (/api/geozones)
```
POST   /                          - Создание геозоны
PUT    /{id}                      - Обновление геозоны
DELETE /{id}                      - Удаление геозоны
GET    /client/{clientId}         - Геозоны осужденного
GET    /                          - Все геозоны
GET    /violations                - Все нарушения
GET    /violations/client/{id}    - Нарушения осужденного
```

### RBAC логика

#### Роли
1. **deptAdmin** - Администратор департамента
   - Видит всех сотрудников и осужденных
   - Полный доступ к управлению

2. **mruAdmin** - Администратор МРУ
   - Видит сотрудников своего МРУ
   - Видит осужденных всех районов своего МРУ

3. **inspector** - Инспектор района
   - Видит только себя
   - Видит осужденных своих районов (districtIds)

#### Реализация
- Фильтрация в `AdminService.getClients(currentUser)`
- Автоматическое определение прав по `attributes` в User
- Иерархическая проверка доступа

### Проверка геозон

При получении GPS данных (`/api/traccar/positions`):
1. Извлекаются координаты (lat, lon)
2. Находится клиент по uniqueId
3. Проверяются все активные геозоны клиента
4. Используется алгоритм Ray Casting для проверки точки в полигоне
5. При выходе создается `GeoZoneViolation`
6. Отправляется уведомление (TODO: добавить FCM токены сотрудников)

---

## 2. МОБИЛЬНОЕ ПРИЛОЖЕНИЕ

### Оффлайн GPS (iosandroid/src/services/gpsService.js)

#### Основные функции

**Хранение оффлайн:**
```javascript
saveOfflineLocation(positionData)
```
- Сохраняет GPS точку в AsyncStorage
- Ограничение: последние 1000 точек
- Формат: массив с полем `savedAt`

**Синхронизация:**
```javascript
syncOfflineData()
```
- Запускается при восстановлении сети
- Отправка порциями по 10 точек
- Автоматическое удаление успешно отправленных
- Периодическая синхронизация каждые 5 минут

**Мониторинг сети:**
```javascript
setupNetworkListener()
```
- Использует `@react-native-community/netinfo`
- Автоматическая синхронизация при появлении связи
- Флаг `isOnline` для проверки состояния

#### Зависимости (требуется установка)
```bash
npm install @react-native-community/netinfo
```

---

## 3. ФРОНТЕНД (React)

### Компоненты

#### GeoZoneManager (svezh/src/components/geozones/GeoZoneManager.tsx)
- Выбор осужденного
- Рисование полигонов на карте (Leaflet + Leaflet Draw)
- Просмотр существующих геозон
- Активация/деактивация геозон
- Удаление геозон

**Зависимости:**
```bash
npm install leaflet react-leaflet leaflet-draw react-leaflet-draw
npm install @types/leaflet @types/leaflet-draw
```

#### AdminPanel (svezh/src/components/admin/AdminPanel.tsx)
- Управление сотрудниками:
  - Создание с выбором роли и района
  - Удаление
  - Просмотр списка
- Управление осужденными:
  - Перевод между районами
  - Просмотр с фильтрацией по RBAC

---

## 4. БАЗА ДАННЫХ

### Миграции (Flyway)

#### V6__Create_Departments_Mrus_Districts.sql
- Создание таблиц: departments, mrus, districts
- Добавление поля district_id в clients
- Внешние ключи с CASCADE

#### V7__Create_GeoZones.sql
- Таблица geozones с JSONB координатами
- Таблица geozone_violations
- Индексы для оптимизации

#### V8__Insert_Initial_Organization_Structure.sql
- 1 центральный департамент
- 4 МРУ (Север, Юг, Восток, Запад)
- 50 районов (по 12-13 на МРУ)

### Структура данных

```sql
departments (id, name, code)
    ↓
mrus (id, name, code, department_id)
    ↓
districts (id, name, code, mru_id)
    ↓
clients (id, ..., district_id)

users (id, ..., user_type, mru_id, attributes JSONB)

geozones (id, client_id, polygon_coordinates JSONB, is_active)
    ↓
geozone_violations (id, geozone_id, client_id, violation_type, lat, lon)
```

---

## 5. НАСТРОЙКА И ЗАПУСК

### Бэкенд
```bash
cd FreshBackend/FreshBackend
./gradlew clean build
./gradlew bootRun
```

### Фронтенд
```bash
cd svezh
npm install
npm install leaflet react-leaflet leaflet-draw react-leaflet-draw
npm run dev
```

### Мобильное приложение
```bash
cd iosandroid
npm install
npm install @react-native-community/netinfo
npx expo start
```

---

## 6. ИСПОЛЬЗОВАНИЕ

### Создание геозоны
1. Войти во фронтенд как администратор
2. Перейти в "Геозоны"
3. Выбрать осужденного
4. Ввести название геозоны
5. Нарисовать полигон на карте
6. Геозона автоматически сохранится

### Добавление сотрудника
1. Войти как администратор департамента
2. Перейти в "Админ панель" → "Сотрудники"
3. Заполнить форму:
   - ИНН (логин)
   - Пароль
   - Unique ID (для GPS)
   - Роль (deptAdmin/mruAdmin/inspector)
   - Район (для привязки)
4. Нажать "Создать"

### Перевод осужденного
1. Войти как администратор
2. Перейти в "Админ панель" → "Осужденные"
3. Выбрать новый район из выпадающего списка
4. Подтвердить перевод

---

## 7. ИСПРАВЛЕННЫЕ ОШИБКИ

1. ✅ SecurityConfig - все endpoints были открыты
2. ✅ build.gradle.kts - Flyway конфигурация не соответствовала БД
3. ✅ Client model - отсутствовала связь с районом
4. ✅ RegistryController - отсутствовала RBAC фильтрация
5. ✅ GPS Service - отсутствовало оффлайн хранение

---

## 8. АРХИТЕКТУРА

```
┌─────────────────┐
│  Mobile App     │ ← GPS данные (онлайн/оффлайн)
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│  Spring Boot    │ ← Проверка геозон
│  :8083          │ ← RBAC фильтрация
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│  PostgreSQL     │ ← Иерархическая структура
│  :5432          │ ← Геозоны (JSONB)
└─────────────────┘

┌─────────────────┐
│  React Frontend │ ← Админ панель
│  :5173          │ ← Геозоны (Leaflet)
└─────────────────┘
```

---

## 9. TODO (Дополнительные улучшения)

- [ ] Добавить FCM уведомления сотрудникам при нарушении
- [ ] Реализовать историю перемещений осужденного
- [ ] Добавить графики и аналитику
- [ ] Реализовать экспорт отчетов
- [ ] Добавить многоязычность
- [ ] Настроить HTTPS для продакшена
- [ ] Добавить тесты (JUnit для бэкенда, Jest для фронтенда)

---

## 10. КОНТАКТЫ И ПОДДЕРЖКА

При возникновении вопросов или проблем:
1. Проверить логи Spring Boot
2. Проверить логи мобильного приложения (Expo)
3. Проверить миграции Flyway
4. Проверить подключение к PostgreSQL

**База данных:** probationmob
**Порты:**
- Backend: 8083
- Frontend: 5173
- PostgreSQL: 5432

---

Проект готов к тестированию и развертыванию!
