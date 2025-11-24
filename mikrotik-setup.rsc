# MikroTik NAT Configuration для Probation System
# Скопируйте этот файл и выполните в MikroTik Terminal (Winbox)
#
# ПЕРЕД ЗАПУСКОМ ЗАМЕНИТЕ:
# - 192.168.88.100 на IP вашего сервера
# - ether1 на ваш WAN интерфейс
# - 192.168.88.0/24 на вашу подсеть

# ===================================
# НАСТРОЙКИ (ИЗМЕНИТЕ ПОД СВОЮ СЕТЬ)
# ===================================
:local serverIP "192.168.88.100"
:local wanInterface "ether1"
:local lanSubnet "192.168.88.0/24"

# ===================================
# 1. NAT ПРАВИЛА (Port Forwarding)
# ===================================

:put "Создаем NAT правила..."

# HTTP (порт 80)
/ip firewall nat add chain=dstnat action=dst-nat \
  protocol=tcp dst-port=80 \
  in-interface=$wanInterface \
  to-addresses=$serverIP to-ports=80 \
  comment="Probation System - HTTP"

:put "✓ NAT для HTTP (порт 80) создан"

# HTTPS (порт 443) - раскомментируйте если нужен SSL
# /ip firewall nat add chain=dstnat action=dst-nat \
#   protocol=tcp dst-port=443 \
#   in-interface=$wanInterface \
#   to-addresses=$serverIP to-ports=443 \
#   comment="Probation System - HTTPS"
#
# :put "✓ NAT для HTTPS (порт 443) создан"

# ===================================
# 2. FIREWALL FILTER RULES
# ===================================

:put "Создаем Firewall правила..."

# Разрешаем входящий HTTP
/ip firewall filter add chain=forward action=accept \
  protocol=tcp dst-port=80 \
  in-interface=$wanInterface \
  connection-state=new \
  place-before=0 \
  comment="Allow Probation HTTP"

:put "✓ Firewall правило для HTTP создано"

# Разрешаем входящий HTTPS - раскомментируйте если нужен SSL
# /ip firewall filter add chain=forward action=accept \
#   protocol=tcp dst-port=443 \
#   in-interface=$wanInterface \
#   connection-state=new \
#   place-before=0 \
#   comment="Allow Probation HTTPS"
#
# :put "✓ Firewall правило для HTTPS создано"

# ===================================
# 3. HAIRPIN NAT (опционально)
# ===================================
# Позволяет обращаться к внешнему IP изнутри локальной сети

:put "Создаем Hairpin NAT..."

/ip firewall nat add chain=srcnat action=masquerade \
  protocol=tcp dst-address=$serverIP dst-port=80 \
  src-address=$lanSubnet \
  comment="Probation Hairpin NAT"

:put "✓ Hairpin NAT создан"

# ===================================
# 4. ПРОВЕРКА КОНФИГУРАЦИИ
# ===================================

:put "==================== ПРОВЕРКА ===================="
:put ""
:put "NAT правила:"
/ip firewall nat print where comment~"Probation"
:put ""
:put "Firewall Filter правила:"
/ip firewall filter print where comment~"Probation"
:put ""
:put "==================== ГОТОВО ======================"
:put ""
:put "✅ Конфигурация применена!"
:put ""
:put "Проверьте работу:"
:put "1. На сервере: curl http://localhost/health"
:put "2. Извне: http://85.113.27.42/health"
:put "3. Онлайн: https://canyouseeme.org/ (IP: 85.113.27.42, Port: 80)"
:put ""
:put "Для удаления конфигурации выполните:"
:put "/ip firewall nat remove [find comment~\"Probation\"]"
:put "/ip firewall filter remove [find comment~\"Probation\"]"
