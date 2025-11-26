# Решение для Backend на Windows

## Ситуация
Backend запущен на Windows машине, и нужно сделать его доступным через 85.113.27.42

## Вариант 1: Прямой проброс портов (Быстрое решение)

### Шаг 1: Откройте порт 8083 в Windows Firewall

```powershell
# Запустите PowerShell от имени администратора и выполните:
New-NetFirewallRule -DisplayName "Spring Boot Backend" -Direction Inbound -Protocol TCP -LocalPort 8083 -Action Allow
```

Или через GUI:
1. Откройте **Панель управления** → **Система и безопасность** → **Брандмауэр Windows**
2. **Дополнительные параметры** → **Правила для входящих подключений**
3. **Создать правило** → **Для порта** → TCP, порт 8083
4. **Разрешить подключение**

### Шаг 2: Пробросьте порт на роутере

1. Откройте админку роутера (обычно http://192.168.1.1)
2. Найдите раздел **Port Forwarding / Virtual Server**
3. Добавьте правило:
   - **Внешний порт:** 8083
   - **Внутренний IP:** [IP вашей Windows машины]
   - **Внутренний порт:** 8083
   - **Протокол:** TCP

### Шаг 3: Обновите мобильное приложение

```javascript
// iosandroid/src/utils/constants.js
export const API_CONFIG = {
  BASE_URL: 'http://85.113.27.42:8083/api',  // Прямое подключение
  TIMEOUT: 10000,
  RETRY_ATTEMPTS: 3,
  RETRY_DELAY: 1000,
};
```

### Шаг 4: Проверка

```bash
# С любого устройства в интернете:
curl http://85.113.27.42:8083/api/
```

---

## Вариант 2: Установка Nginx на Windows (Рекомендуется)

### Преимущества:
- ✅ Один порт (80) вместо 8083
- ✅ Можно добавить SSL
- ✅ Централизованное управление

### Установка:

1. **Скачайте Nginx для Windows:**
   - http://nginx.org/en/download.html
   - Выберите "Stable version" для Windows

2. **Распакуйте в C:\nginx**

3. **Замените C:\nginx\conf\nginx.conf на нашу конфигурацию:**

```nginx
# Упрощённая конфигурация для Windows
worker_processes 1;

events {
    worker_connections 1024;
}

http {
    include mime.types;
    default_type application/octet-stream;

    server {
        listen 80;
        server_name 85.113.27.42;

        # Backend API
        location /api/ {
            proxy_pass http://127.0.0.1:8083/api/;
            proxy_http_version 1.1;

            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

            # CORS
            add_header 'Access-Control-Allow-Origin' '*' always;
            add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS' always;
            add_header 'Access-Control-Allow-Headers' 'DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization' always;

            if ($request_method = 'OPTIONS') {
                add_header 'Access-Control-Allow-Origin' '*';
                add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS';
                add_header 'Access-Control-Allow-Headers' 'DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization';
                add_header 'Access-Control-Max-Age' 1728000;
                add_header 'Content-Type' 'text/plain; charset=utf-8';
                add_header 'Content-Length' 0;
                return 204;
            }
        }

        # Health check
        location /health {
            return 200 "OK - Backend:8083\n";
            add_header Content-Type text/plain;
        }

        # Root
        location / {
            return 200 '<!DOCTYPE html><html><body><h1>Probation System</h1><p>API: <a href="/api/">/api/</a></p></body></html>';
            add_header Content-Type text/html;
        }
    }
}
```

4. **Запустите Nginx:**

```cmd
cd C:\nginx
start nginx
```

5. **Проверьте что работает:**

```cmd
curl http://localhost/health
curl http://localhost/api/
```

6. **Пробросьте порт 80 на роутере** (вместо 8083)

7. **Автозапуск Nginx как службы:**

Используйте **NSSM** (Non-Sucking Service Manager):
```cmd
# Скачайте NSSM: https://nssm.cc/download
nssm install nginx "C:\nginx\nginx.exe"
nssm start nginx
```

### Управление Nginx на Windows:

```cmd
# Запуск
cd C:\nginx
start nginx

# Остановка
nginx -s stop

# Перезагрузка конфигурации
nginx -s reload

# Проверка конфигурации
nginx -t
```

---

## Вариант 3: Использовать ngrok (Для быстрого тестирования)

```cmd
# Скачайте ngrok: https://ngrok.com/download
# Запустите:
ngrok http 8083

# Используйте полученный URL в приложении:
BASE_URL: 'https://xxxxx.ngrok-free.app/api'
```

⚠️ **Внимание:** ngrok бесплатная версия меняет URL при каждом запуске!

---

## Вариант 4: Backend на отдельном Linux сервере (Лучшее решение)

Если у вас есть VPS или можете поднять Linux сервер:

1. Разверните Backend на Linux сервере с IP 85.113.27.42
2. Установите Nginx на том же сервере
3. Используйте наши готовые скрипты (fix_nginx.sh)

---

## Рекомендации

**Для разработки:**
- ✅ Вариант 1 (прямой порт 8083) - быстро, но небезопасно
- ✅ Вариант 3 (ngrok) - удобно для тестирования с телефона

**Для продакшена:**
- ✅ Вариант 2 (Nginx на Windows) - приемлемо
- ✅✅✅ Вариант 4 (Linux сервер) - **лучший вариант!**

---

## Что сейчас сделать?

1. **Узнайте внутренний IP вашей Windows машины:**
   ```cmd
   ipconfig | findstr IPv4
   ```

2. **Проверьте что Backend отвечает локально:**
   ```cmd
   curl http://localhost:8083/api/
   ```

3. **Выберите один из вариантов выше**

4. **Пробросьте нужный порт на роутере**

5. **Проверьте извне:**
   ```bash
   curl http://85.113.27.42:[ПОРТ]/api/
   ```

---

## Проверка портов на Windows

```powershell
# Проверить что Backend слушает 8083:
netstat -ano | findstr :8083

# Проверить что Nginx слушает 80 (если установлен):
netstat -ano | findstr :80
```

---

## Если нужна помощь

Напишите:
1. Где запущен Backend? (Windows/Linux/VPS)
2. Что такое 85.113.27.42? (Ваш внешний IP/VPS/другое)
3. Есть ли отдельный сервер или всё на одной машине?

И я помогу выбрать оптимальное решение!
