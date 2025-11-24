# 🔧 Настройка Port Forwarding на MikroTik через Winbox

## Ваша конфигурация

- **Внешний IP:** 85.113.27.42
- **Внутренний IP сервера:** (узнаем ниже)
- **Роутер:** MikroTik (управление через Winbox)
- **Порты:** 80 (HTTP), 443 (HTTPS - опционально)

---

## Шаг 1: Узнайте внутренний IP сервера

На сервере выполните:

```bash
hostname -I | awk '{print $1}'
```

Например: `192.168.88.100` (у MikroTik обычно подсеть `192.168.88.0/24`)

**Запишите этот IP!**

---

## Шаг 2: Подключитесь к MikroTik через Winbox

1. **Скачайте Winbox** (если еще нет):
   - https://mikrotik.com/download
   - Или с роутера: http://192.168.88.1/winbox.exe

2. **Запустите Winbox**

3. **Подключитесь:**
   - В поле "Connect To" введите: `192.168.88.1` (или IP вашего MikroTik)
   - Login: `admin` (или ваш логин)
   - Password: ваш пароль
   - Нажмите **Connect**

---

## Шаг 3: Настройка NAT (Port Forwarding)

### Вариант 1: Через GUI (графический интерфейс)

1. **Откройте раздел NAT:**
   ```
   IP → Firewall → NAT (вкладка)
   ```

2. **Добавьте новое правило:**
   - Нажмите **+ (Add New)**

3. **Вкладка "General":**
   ```
   Chain: dstnat
   Protocol: 6 (tcp)
   Dst. Port: 80
   In. Interface: ether1 (или ваш WAN интерфейс)
   ```

   **Важно:** Выберите правильный WAN интерфейс!
   - Обычно это `ether1` или `pppoe-out1`
   - Можно проверить в: **IP → Addresses** (интерфейс с внешним IP)

4. **Вкладка "Action":**
   ```
   Action: dst-nat
   To Addresses: [IP вашего сервера из Шага 1]
   To Ports: 80
   ```

   Например:
   ```
   To Addresses: 192.168.88.100
   To Ports: 80
   ```

5. **Нажмите "OK"** для сохранения

6. **Опционально: добавьте правило для HTTPS (порт 443)**
   - Повторите шаги 2-5, но вместо порта 80 используйте 443

### Вариант 2: Через Terminal (командная строка)

Откройте **New Terminal** в Winbox и выполните:

```bash
# Для HTTP (порт 80)
/ip firewall nat add chain=dstnat action=dst-nat \
  protocol=tcp dst-port=80 \
  in-interface=ether1 \
  to-addresses=192.168.88.100 to-ports=80 \
  comment="Probation System HTTP"

# Опционально: для HTTPS (порт 443)
/ip firewall nat add chain=dstnat action=dst-nat \
  protocol=tcp dst-port=443 \
  in-interface=ether1 \
  to-addresses=192.168.88.100 to-ports=443 \
  comment="Probation System HTTPS"
```

**Замените:**
- `ether1` - на ваш WAN интерфейс
- `192.168.88.100` - на IP вашего сервера

---

## Шаг 4: Проверка правила NAT

### Через GUI:

1. Откройте: **IP → Firewall → NAT**
2. Вы должны увидеть новое правило:
   ```
   Chain: dstnat
   Protocol: tcp
   Dst Port: 80
   Action: dst-nat
   To Addresses: 192.168.88.100:80
   ```

3. Убедитесь что:
   - ✅ Правило **НЕ** отключено (нет флага "X")
   - ✅ Указан правильный WAN интерфейс

### Через Terminal:

```bash
/ip firewall nat print
```

Вывод должен содержать:
```
0  chain=dstnat action=dst-nat to-addresses=192.168.88.100
   to-ports=80 protocol=tcp dst-port=80
   in-interface=ether1
```

---

## Шаг 5: Проверка Firewall Filter Rules

**ВАЖНО:** MikroTik может блокировать входящие соединения.

### Проверьте правила Firewall:

1. Откройте: **IP → Firewall → Filter Rules**

2. Найдите правила с:
   ```
   Chain: forward
   Action: drop/reject
   ```

3. **Убедитесь** что есть правило разрешающее порт 80:

   **Если такого правила НЕТ - создайте его:**

   - Нажмите **+ (Add New)**
   - **General:**
     ```
     Chain: forward
     Protocol: 6 (tcp)
     Dst. Port: 80
     In. Interface: ether1 (WAN)
     Connection State: new
     ```
   - **Action:**
     ```
     Action: accept
     ```
   - Нажмите **OK**

   **ВАЖНО:** Переместите это правило **ВЫШЕ** правил с `action=drop`!
   - Правила применяются сверху вниз
   - Используйте кнопки **↑ ↓** для перемещения

### Через Terminal:

```bash
# Добавить правило разрешения (добавьте ПЕРЕД правилами drop)
/ip firewall filter add chain=forward action=accept \
  protocol=tcp dst-port=80 \
  in-interface=ether1 \
  connection-state=new \
  place-before=0 \
  comment="Allow Probation HTTP"

# Проверка
/ip firewall filter print
```

---

## Шаг 6: Проверка работы

### 6.1 С самого сервера:

```bash
# Проверка локально
curl http://localhost/health

# Проверка через внешний IP
curl http://85.113.27.42/health
```

### 6.2 С компьютера в той же сети:

```bash
# Проверка внутреннего IP
curl http://192.168.88.100/health

# ВАЖНО: NAT Hairpin/Loopback
# Если не работает http://85.113.27.42 изнутри сети - это нормально
# Для работы изнутри нужна настройка Hairpin NAT (см. ниже)
```

### 6.3 С внешнего устройства (не в вашей сети):

```bash
# Откройте в браузере или curl
curl http://85.113.27.42/health

# Должно вернуть:
OK - Backend:8083, Traccar:8082
```

### 6.4 Онлайн проверка:

Откройте: **https://canyouseeme.org/**
- IP: `85.113.27.42`
- Port: `80`
- Нажмите "Check Port"

Должно быть: **✅ Success: I can see your service**

---

## Шаг 7: NAT Hairpin (опционально)

Если нужно чтобы внешний IP работал изнутри локальной сети:

### Через Terminal:

```bash
# Hairpin NAT - доступ к внешнему IP изнутри сети
/ip firewall nat add chain=srcnat action=masquerade \
  protocol=tcp dst-address=192.168.88.100 dst-port=80 \
  src-address=192.168.88.0/24 \
  comment="Hairpin NAT for Probation"
```

Теперь `http://85.113.27.42` будет работать и изнутри сети.

---

## 📊 Полная схема NAT

```
Интернет (85.113.27.42:80)
        ↓
   MikroTik WAN (ether1)
        ↓
   [NAT Rule: dstnat]
   dst-port: 80 → to-addresses: 192.168.88.100:80
        ↓
   [Firewall Filter: forward]
   action: accept, dst-port: 80
        ↓
   MikroTik LAN (bridge/ether2-5)
        ↓
   Сервер (192.168.88.100:80)
        ↓
   Nginx
        ↓
   Spring Boot :8083 / Traccar :8082
```

---

## 🔍 Диагностика MikroTik

### Проверка счетчиков пакетов:

1. **IP → Firewall → NAT**
2. Смотрите колонки **Packets** и **Bytes**
3. Если счетчики растут - правило работает!

### Логи Firewall:

Временно включите логирование:

1. Откройте правило NAT
2. **Action** → **Log: yes**
3. Смотрите логи: **Log** (левая панель Winbox)

### Мониторинг трафика:

```bash
# Terminal
/tool traffic-monitor interface=ether1
```

### Проверка активных соединений:

```bash
# Terminal
/ip firewall connection print where dst-port=80
```

---

## 🚨 Troubleshooting

### Проблема: Порт закрыт при онлайн проверке

**Проверьте:**

1. **NAT правило создано?**
   ```bash
   /ip firewall nat print where dst-port=80
   ```

2. **Firewall не блокирует?**
   ```bash
   /ip firewall filter print where chain=forward
   ```

3. **Правильный WAN интерфейс?**
   ```bash
   # Узнайте WAN интерфейс
   /ip address print where network=85.113.27.42
   ```

4. **Внешний IP правильный?**
   ```bash
   /ip address print
   ```

### Проблема: NAT работает, но сервер не отвечает

```bash
# На сервере проверьте:
sudo systemctl status nginx
curl http://localhost/health

# Проверьте UFW
sudo ufw status
```

### Проблема: Работает локально, но не извне

**Возможные причины:**
1. ❌ Провайдер дает "серый" IP (за NAT)
2. ❌ Провайдер блокирует порт 80

**Проверка "серого" IP:**

В Winbox Terminal:
```bash
/ip address print
```

Сравните IP из `/ip address` с `85.113.27.42`.
- Если совпадают - у вас "белый" IP ✅
- Если не совпадают - у вас "серый" IP ❌

**Если "серый" IP:**
- Обратитесь к провайдеру за "белым" IP
- Или используйте VPN туннель (Wireguard, OpenVPN)

---

## 📝 Полезные команды MikroTik

```bash
# Просмотр NAT правил
/ip firewall nat print

# Просмотр Filter правил
/ip firewall filter print

# Просмотр адресов
/ip address print

# Просмотр маршрутов
/ip route print

# Просмотр активных соединений
/ip firewall connection print

# Очистка счетчиков NAT
/ip firewall nat reset-counters-all

# Бэкап конфигурации
/export file=backup

# Просмотр интерфейсов
/interface print

# Статистика интерфейса
/interface monitor-traffic ether1
```

---

## ✅ Итоговый чек-лист для MikroTik

- [ ] Узнал внутренний IP сервера
- [ ] Подключился к MikroTik через Winbox
- [ ] Создал NAT правило (dstnat)
- [ ] Проверил что правило активно (нет X)
- [ ] Проверил Firewall Filter (forward → accept)
- [ ] Переместил accept правило выше drop
- [ ] Правило NAT имеет счетчик пакетов > 0
- [ ] Локально работает: `curl http://localhost/health`
- [ ] Внешне работает: `curl http://85.113.27.42/health`
- [ ] Онлайн проверка показывает "Port is open"
- [ ] Опционально: настроил Hairpin NAT
- [ ] Опционально: настроил порт 443 для HTTPS

---

## 🎯 Готовая конфигурация (копипаста)

Откройте Terminal в Winbox и выполните (замените IP и интерфейс):

```bash
# Переменные (ЗАМЕНИТЕ НА СВОИ!)
:local serverIP "192.168.88.100"
:local wanInterface "ether1"

# NAT для HTTP (порт 80)
/ip firewall nat add chain=dstnat action=dst-nat \
  protocol=tcp dst-port=80 \
  in-interface=$wanInterface \
  to-addresses=$serverIP to-ports=80 \
  comment="Probation HTTP"

# NAT для HTTPS (порт 443) - опционально
/ip firewall nat add chain=dstnat action=dst-nat \
  protocol=tcp dst-port=443 \
  in-interface=$wanInterface \
  to-addresses=$serverIP to-ports=443 \
  comment="Probation HTTPS"

# Firewall разрешение HTTP
/ip firewall filter add chain=forward action=accept \
  protocol=tcp dst-port=80 \
  in-interface=$wanInterface \
  connection-state=new \
  place-before=0 \
  comment="Allow Probation HTTP"

# Firewall разрешение HTTPS - опционально
/ip firewall filter add chain=forward action=accept \
  protocol=tcp dst-port=443 \
  in-interface=$wanInterface \
  connection-state=new \
  place-before=0 \
  comment="Allow Probation HTTPS"

# Hairpin NAT (опционально)
/ip firewall nat add chain=srcnat action=masquerade \
  protocol=tcp dst-address=$serverIP dst-port=80 \
  src-address=192.168.88.0/24 \
  comment="Hairpin NAT Probation"

# Проверка
/ip firewall nat print where comment~"Probation"
/ip firewall filter print where comment~"Probation"
```

**Не забудьте заменить:**
- `192.168.88.100` → ваш IP сервера
- `ether1` → ваш WAN интерфейс
- `192.168.88.0/24` → ваша локальная подсеть

---

## 🔗 Полезные ссылки

- **MikroTik Wiki NAT:** https://wiki.mikrotik.com/wiki/Manual:IP/Firewall/NAT
- **MikroTik Firewall:** https://wiki.mikrotik.com/wiki/Manual:IP/Firewall/Filter
- **MikroTik Forum:** https://forum.mikrotik.com/
- **Скачать Winbox:** https://mikrotik.com/download

---

## 📞 Если нужна помощь

1. Экспортируйте конфигурацию:
   ```bash
   /export file=config
   ```

2. Скачайте файл через Winbox:
   - **Files** → выберите `config.rsc` → **Download**

3. Можете прислать для анализа (уберите пароли!)
