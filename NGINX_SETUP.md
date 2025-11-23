# 🚀 Установка Nginx для Probation System

## Быстрая установка (5 минут)

### Шаг 1: Установка Nginx

```bash
sudo apt update
sudo apt install -y nginx
```

### Шаг 2: Применение конфигурации

```bash
# Создаем бэкап старой конфигурации
sudo cp /etc/nginx/nginx.conf /etc/nginx/nginx.conf.backup

# Копируем нашу конфигурацию
sudo cp /home/user/mysuperrr/nginx.conf /etc/nginx/nginx.conf

# Проверяем конфигурацию
sudo nginx -t
```

Если видите `syntax is ok` и `test is successful` - продолжаем.

### Шаг 3: Перезапуск Nginx

```bash
sudo systemctl restart nginx
sudo systemctl enable nginx
sudo systemctl status nginx
```

### Шаг 4: Настройка Firewall

```bash
# Открываем порт 80 (HTTP)
sudo ufw allow 80/tcp

# Опционально: если планируете SSL
sudo ufw allow 443/tcp

# Закрываем прямой доступ к портам (рекомендуется)
sudo ufw deny 8082/tcp
sudo ufw deny 8083/tcp

# Проверяем правила
sudo ufw status
```

### Шаг 5: Проверка работы

```bash
# Проверка healthcheck
curl http://85.113.27.42/health

# Проверка API
curl http://85.113.27.42/api/

# Проверка Traccar UI
curl http://85.113.27.42/traccar/
```

---

## 📋 URL-адреса

После установки у вас будут доступны:

- **Backend API:** `http://85.113.27.42/api/`
- **Traccar Web UI:** `http://85.113.27.42/traccar/`
- **Health Check:** `http://85.113.27.42/health`
- **OsmAnd GPS:** `http://85.113.27.42/?id=xxx&lat=xxx&lon=xxx...`

---

## 🔧 Настройка мобильного приложения

Обновите файл `iosandroid/src/utils/constants.js`:

```javascript
export const API_CONFIG = {
  BASE_URL: 'http://85.113.27.42/api',  // Было: ngrok URL
  TIMEOUT: 10000,
  RETRY_ATTEMPTS: 3,
  RETRY_DELAY: 1000,
};

export const TRACCAR_CONFIG = {
  GPS_ENDPOINT: 'http://85.113.27.42',
  API_ENDPOINT: 'http://85.113.27.42/api/traccar',
  TRACCAR_PORT: 80,  // Было: 8082
};
```

---

## 📊 Полезные команды

### Управление Nginx

```bash
# Перезапуск
sudo systemctl restart nginx

# Остановка
sudo systemctl stop nginx

# Запуск
sudo systemctl start nginx

# Проверка конфигурации
sudo nginx -t

# Перезагрузка без разрыва соединений
sudo systemctl reload nginx
```

### Просмотр логов

```bash
# Все логи в реальном времени
sudo tail -f /var/log/nginx/access.log

# Только ошибки
sudo tail -f /var/log/nginx/error.log

# Последние 100 строк
sudo tail -n 100 /var/log/nginx/access.log

# Поиск ошибок
sudo grep "error" /var/log/nginx/error.log
```

### Диагностика проблем

```bash
# Проверка портов
sudo netstat -tulpn | grep nginx
sudo netstat -tulpn | grep :80
sudo netstat -tulpn | grep :8083
sudo netstat -tulpn | grep :8082

# Проверка процессов
ps aux | grep nginx

# Тест доступности
curl -I http://85.113.27.42/health
```

---

## 🔒 Настройка HTTPS (опционально, но рекомендуется)

### Если у вас есть домен (например, probation.example.com):

```bash
# Установка Certbot
sudo apt install -y certbot python3-certbot-nginx

# Получение SSL сертификата
sudo certbot --nginx -d probation.example.com

# Certbot автоматически обновит конфигурацию nginx
```

### Если домена нет - используете IP:

Для IP-адреса SSL сложнее (нужен самоподписанный сертификат). Рекомендую:
1. Получить бесплатный домен (например, на freenom.com)
2. Настроить A-запись на ваш IP 85.113.27.42
3. Получить Let's Encrypt сертификат

---

## ⚠️ Troubleshooting

### Nginx не запускается

```bash
# Проверьте синтаксис конфигурации
sudo nginx -t

# Проверьте логи ошибок
sudo tail -f /var/log/nginx/error.log

# Проверьте не занят ли порт 80
sudo netstat -tulpn | grep :80

# Если порт занят Apache - остановите его
sudo systemctl stop apache2
```

### API не отвечает

```bash
# Проверьте работает ли Spring Boot на 8083
curl http://localhost:8083/api/

# Проверьте логи nginx
sudo tail -f /var/log/nginx/error.log

# Проверьте не блокирует ли firewall
sudo ufw status
```

### Traccar не открывается

```bash
# Проверьте работает ли Traccar на 8082
curl http://localhost:8082

# Проверьте статус сервиса
sudo systemctl status traccar  # или как называется ваш сервис

# Проверьте логи nginx
sudo tail -f /var/log/nginx/error.log
```

### GPS данные не доходят

```bash
# Проверьте логи nginx - должны быть запросы с параметрами id, lat, lon
sudo tail -f /var/log/nginx/access.log | grep "id="

# Проверьте логи Spring Boot
cd /home/user/mysuperrr/FreshBackend
tail -f logs/spring-boot-application.log

# Проверьте что устройство создано в Traccar
curl -u admin:admin http://localhost:8082/api/devices
```

---

## 📝 Структура системы

```
Internet (Mobile App)
        ↓
  85.113.27.42:80 (Nginx)
        ↓
    ┌───┴───┐
    ↓       ↓
/api/    /traccar/
    ↓       ↓
localhost:8083  localhost:8082
(Spring Boot)   (Traccar)
```

---

## 🎯 Следующие шаги

1. ✅ Установить Nginx
2. ✅ Применить конфигурацию
3. ✅ Настроить firewall
4. ⬜ Обновить мобильное приложение (constants.js)
5. ⬜ Перезапустить Spring Boot с новой конфигурацией
6. ⬜ Протестировать GPS отслеживание
7. ⬜ (Опционально) Настроить SSL

---

## 💡 Преимущества этой конфигурации

- ✅ Один порт (80) вместо нескольких
- ✅ CORS настроен правильно
- ✅ WebSocket поддержка для Traccar
- ✅ Логирование всех запросов
- ✅ Безопасность (закрыты прямые порты)
- ✅ Готовность к SSL
- ✅ Простая диагностика
