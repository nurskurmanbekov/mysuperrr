# 🗺️ GPS/Traccar Integration Architecture

## ✅ Решение проблем

### Проблема 401 Unauthorized
**Причина:** Неправильные credentials или Traccar сервер не запущен

**Решение:**
1. Проверьте что Traccar запущен на `localhost:8082`
2. Проверьте credentials в `application.properties`:
   ```properties
   traccar.base-url=http://localhost:8082
   traccar.api.username=admin
   traccar.api.password=admin
   ```
3. Используется `headers.setBasicAuth(username, password)` для аутентификации

**Важно:** Даже если Traccar не работает, система продолжает функционировать, сохраняя данные в собственную БД!

---

## 🏗️ Новая Архитектура

```
┌─────────────────┐
│  Mobile App     │
│  (React Native) │
└────────┬────────┘
         │ POST /api/traccar/positions
         │ (GPS данные каждые 10-20 сек)
         ▼
┌─────────────────────────────────────────┐
│     Spring Boot Backend                  │
│                                          │
│  1. TraccarProxyController               │
│     - Получает GPS от мобильного         │
│     - СОХРАНЯЕТ в свою БД (главное!)    │
│     - Опционально → Traccar             │
│                                          │
│  2. device_positions (таблица)           │
│     - Хранит все GPS координаты         │
│     - Статус online/offline             │
│     - Последняя батарея, скорость       │
│                                          │
│  3. PositionController                   │
│     - GET /api/positions/latest         │
│     - GET /api/positions/{id}/latest    │
│     - GET /api/positions/{id}/history   │
└────────┬────────────────────┬───────────┘
         │                    │
         │                    │ (опционально)
         │                    ▼
         │          ┌──────────────────┐
         │          │   Traccar        │
         │          │   (только        │
         │          │   визуализация)  │
         │          └──────────────────┘
         │
         │ GET /api/positions/latest
         ▼
┌─────────────────┐
│   Frontend      │
│   (React +      │
│   Leaflet Map)  │
└─────────────────┘
```

---

## 📊 Потоки данных

### 1. Мобильное приложение → Backend
```
POST http://85.113.27.42:8530/api/traccar/positions
Content-Type: application/json

{
  "id": "1234567890123",
  "lat": 42.88,
  "lon": 74.68,
  "speed": 0,
  "bearing": 0,
  "altitude": 0,
  "accuracy": 10,
  "batt": 85,
  "timestamp": 1732795200
}
```

**Ответ:**
```json
{
  "status": "success",
  "message": "Position saved to database",
  "deviceId": "1234567890123",
  "positionId": 12345,
  "sentToTraccar": true,
  "timestamp": 1732795200000
}
```

### 2. Backend → База данных
Backend автоматически сохраняет каждую GPS позицию в таблицу `device_positions`:

```sql
INSERT INTO device_positions (
  device_id,
  unique_id,
  latitude,
  longitude,
  speed,
  bearing,
  altitude,
  accuracy,
  battery,
  timestamp,
  server_time,
  status,
  sent_to_traccar
) VALUES (
  1,
  '1234567890123',
  42.88,
  74.68,
  0.0,
  0.0,
  0.0,
  10.0,
  85.0,
  '2025-11-28 12:00:00',
  '2025-11-28 12:00:05',
  'online',
  true
);
```

### 3. Backend → Traccar (опционально)
Если Traccar запущен, Backend отправляет данные через OsmAnd протокол:

```
GET http://localhost:8082/?id=1234567890123&lat=42.88&lon=74.68&timestamp=1732795200&speed=0&bearing=0&altitude=0&accuracy=10&batt=85
```

**Важно:** Если Traccar не работает - это НЕ критично! Данные уже сохранены в БД.

### 4. Frontend → Backend (получение данных)
Frontend получает координаты ТОЛЬКО от Backend, НЕ от Traccar:

```
GET http://85.113.27.42:8530/api/positions/latest
```

**Ответ:**
```json
{
  "status": "success",
  "count": 5,
  "positions": [
    {
      "uniqueId": "1234567890123",
      "latitude": 42.88,
      "longitude": 74.68,
      "speed": 0.0,
      "bearing": 0.0,
      "battery": 85.0,
      "timestamp": "2025-11-28T12:00:00",
      "status": "online",
      "sentToTraccar": true
    },
    ...
  ]
}
```

---

## 🗄️ База данных

### Таблица: device_positions

```sql
CREATE TABLE device_positions (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT NOT NULL,           -- ID клиента в системе
    unique_id VARCHAR(50) NOT NULL,      -- ИНН устройства
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    speed DOUBLE PRECISION DEFAULT 0,
    bearing DOUBLE PRECISION DEFAULT 0,
    altitude DOUBLE PRECISION DEFAULT 0,
    accuracy DOUBLE PRECISION DEFAULT 0,
    battery DOUBLE PRECISION DEFAULT 0,
    timestamp TIMESTAMP NOT NULL,        -- Время получения GPS от устройства
    server_time TIMESTAMP NOT NULL,      -- Время получения сервером
    status VARCHAR(20) NOT NULL,         -- online/offline
    sent_to_traccar BOOLEAN DEFAULT false,
    traccar_position_id BIGINT,

    FOREIGN KEY (device_id) REFERENCES clients(id)
);
```

**Индексы для производительности:**
- `idx_device_positions_device_id`
- `idx_device_positions_unique_id`
- `idx_device_positions_timestamp`
- `idx_device_positions_server_time`

---

## 🔌 API Endpoints

### Для мобильного приложения

#### POST /api/traccar/positions
Отправка GPS данных от устройства

**Request:**
```json
{
  "id": "1234567890123",
  "lat": 42.88,
  "lon": 74.68,
  "speed": 0,
  "bearing": 0,
  "altitude": 0,
  "accuracy": 10,
  "batt": 85,
  "timestamp": 1732795200
}
```

**Response:**
```json
{
  "status": "success",
  "message": "Position saved to database",
  "deviceId": "1234567890123",
  "positionId": 12345,
  "sentToTraccar": true
}
```

### Для Frontend

#### GET /api/positions/latest
Получить последние позиции всех устройств

**Response:**
```json
{
  "status": "success",
  "count": 5,
  "positions": [...]
}
```

#### GET /api/positions/{uniqueId}/latest
Получить последнюю позицию устройства

**Response:**
```json
{
  "status": "success",
  "position": {
    "uniqueId": "1234567890123",
    "latitude": 42.88,
    "longitude": 74.68,
    "status": "online"
  }
}
```

#### GET /api/positions/{uniqueId}/history
Получить историю перемещений

**Query Params:**
- `from` - начало периода (ISO datetime)
- `to` - конец периода (ISO datetime)

**Response:**
```json
{
  "status": "success",
  "count": 120,
  "from": "2025-11-28T00:00:00",
  "to": "2025-11-28T23:59:59",
  "positions": [...]
}
```

#### GET /api/positions/online
Получить только онлайн устройства (< 5 минут)

---

## 🚀 Запуск и тестирование

### 1. Запустите Backend
```bash
cd FreshBackend
./gradlew bootRun
```

### 2. (Опционально) Запустите Traccar
```bash
# Если используете Docker
docker run -d --name traccar -p 8082:8082 -p 5055:5055 traccar/traccar

# Или скачайте с https://www.traccar.org/download/
```

### 3. Тест: Отправка GPS от мобильного приложения
```bash
curl -X POST http://localhost:8083/api/traccar/positions \
  -H "Content-Type: application/json" \
  -d '{
    "id": "1234567890123",
    "lat": 42.88,
    "lon": 74.68,
    "speed": 0,
    "bearing": 0,
    "altitude": 0,
    "accuracy": 10,
    "batt": 85,
    "timestamp": 1732795200
  }'
```

**Ожидаемый ответ:**
```json
{
  "status": "success",
  "message": "Position saved to database",
  "positionId": 1
}
```

### 4. Тест: Получение позиций
```bash
curl http://localhost:8083/api/positions/latest
```

**Ожидаемый ответ:**
```json
{
  "status": "success",
  "count": 1,
  "positions": [
    {
      "uniqueId": "1234567890123",
      "latitude": 42.88,
      "longitude": 74.68,
      "status": "online"
    }
  ]
}
```

---

## ✅ Преимущества новой архитектуры

1. **Надежность:** Данные сохраняются в свою БД независимо от Traccar
2. **Производительность:** Frontend получает данные напрямую от Backend
3. **Offline/Online статус:** Автоматически определяется по времени последней позиции
4. **История:** Полная история перемещений сохраняется в БД
5. **Масштабируемость:** Легко добавить кэширование, индексы, аналитику
6. **Независимость:** Traccar опционален, используется только для визуализации

---

## 📝 Настройка Frontend

### React + Leaflet пример:

```typescript
// services/positionApi.ts
export const positionAPI = {
  // Получить последние позиции всех устройств
  getLatest: () =>
    api.get('/positions/latest'),

  // Получить историю устройства
  getHistory: (uniqueId: string, from: string, to: string) =>
    api.get(`/positions/${uniqueId}/history`, { params: { from, to } }),

  // Получить только онлайн
  getOnline: () =>
    api.get('/positions/online'),
};

// components/DeviceMap.tsx
const DeviceMap = () => {
  const [positions, setPositions] = useState([]);

  useEffect(() => {
    const fetchPositions = async () => {
      const response = await positionAPI.getLatest();
      setPositions(response.data.positions);
    };

    // Обновляем каждые 10 секунд
    const interval = setInterval(fetchPositions, 10000);
    fetchPositions();

    return () => clearInterval(interval);
  }, []);

  return (
    <MapContainer center={[42.88, 74.68]} zoom={13}>
      <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
      {positions.map(pos => (
        <Marker
          key={pos.uniqueId}
          position={[pos.latitude, pos.longitude]}
        >
          <Popup>
            {pos.uniqueId} - {pos.status}
            <br />Battery: {pos.battery}%
          </Popup>
        </Marker>
      ))}
    </MapContainer>
  );
};
```

---

## 🔧 Конфигурация

### application.properties
```properties
# Server
server.port=8083

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/probationmob
spring.datasource.username=postgres
spring.datasource.password=556055

# Traccar Integration (опционально)
traccar.base-url=http://localhost:8082
traccar.api.username=admin
traccar.api.password=admin
```

---

## 📦 Структура файлов

```
FreshBackend/
├── src/main/kotlin/
│   └── com/example/probationbackend/
│       ├── controller/
│       │   ├── TraccarProxyController.kt  ✅ Прием GPS
│       │   └── PositionController.kt      ✅ API для Frontend
│       ├── model/
│       │   └── DevicePosition.kt          ✅ Entity
│       ├── repository/
│       │   └── DevicePositionRepository.kt ✅ JPA Repository
│       └── service/
│           ├── FcmService.kt              ✅ Уведомления
│           └── GeoZoneService.kt          ✅ Проверка геозон
└── src/main/resources/
    └── db/migration/
        └── V13__Create_Device_Positions_Table.sql ✅ Migration
```

---

## 🎯 Итого

**Теперь у вас:**
✅ Backend сам хранит координаты в БД
✅ Frontend получает данные только от Backend
✅ Traccar опционален (только для визуализации)
✅ Offline/Online статус работает корректно
✅ История перемещений сохраняется
✅ Система работает даже если Traccar упал

**Backend - единый источник правды!** 🚀
