#!/bin/bash

# Скрипт проверки портов и конфигурации Probation System

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

EXTERNAL_IP="85.113.27.42"

echo -e "${BLUE}╔════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  Probation System - Проверка конфигурации     ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════╝${NC}"
echo ""

# 1. Проверка внутреннего IP
echo -e "${YELLOW}[1/8] Проверка внутреннего IP...${NC}"
INTERNAL_IP=$(hostname -I | awk '{print $1}')
if [ -z "$INTERNAL_IP" ]; then
    echo -e "${RED}❌ Не удалось определить внутренний IP${NC}"
else
    echo -e "${GREEN}✅ Внутренний IP: $INTERNAL_IP${NC}"
fi
echo ""

# 2. Проверка Nginx
echo -e "${YELLOW}[2/8] Проверка Nginx...${NC}"
if systemctl is-active --quiet nginx; then
    echo -e "${GREEN}✅ Nginx запущен${NC}"
    NGINX_PORT=$(sudo netstat -tulpn 2>/dev/null | grep nginx | grep :80 | wc -l)
    if [ $NGINX_PORT -gt 0 ]; then
        echo -e "${GREEN}✅ Nginx слушает порт 80${NC}"
    else
        echo -e "${RED}❌ Nginx НЕ слушает порт 80${NC}"
    fi
else
    echo -e "${RED}❌ Nginx не запущен${NC}"
    echo -e "   Запустите: ${BLUE}sudo systemctl start nginx${NC}"
fi
echo ""

# 3. Проверка Spring Boot
echo -e "${YELLOW}[3/8] Проверка Spring Boot (порт 8083)...${NC}"
BACKEND_PORT=$(netstat -tuln 2>/dev/null | grep :8083 | wc -l)
if [ $BACKEND_PORT -gt 0 ]; then
    echo -e "${GREEN}✅ Spring Boot работает на порту 8083${NC}"
    # Проверяем доступность API
    if curl -s http://localhost:8083/api/ > /dev/null 2>&1; then
        echo -e "${GREEN}✅ API доступен локально${NC}"
    else
        echo -e "${YELLOW}⚠️  API не отвечает на localhost:8083/api/${NC}"
    fi
else
    echo -e "${RED}❌ Spring Boot не запущен на порту 8083${NC}"
fi
echo ""

# 4. Проверка Traccar
echo -e "${YELLOW}[4/8] Проверка Traccar (порт 8082)...${NC}"
TRACCAR_PORT=$(netstat -tuln 2>/dev/null | grep :8082 | wc -l)
if [ $TRACCAR_PORT -gt 0 ]; then
    echo -e "${GREEN}✅ Traccar работает на порту 8082${NC}"
else
    echo -e "${RED}❌ Traccar не запущен на порту 8082${NC}"
fi
echo ""

# 5. Проверка UFW (Firewall)
echo -e "${YELLOW}[5/8] Проверка Firewall (UFW)...${NC}"
if command -v ufw &> /dev/null; then
    UFW_STATUS=$(sudo ufw status | grep "Status:" | awk '{print $2}')
    if [ "$UFW_STATUS" = "active" ]; then
        echo -e "${GREEN}✅ UFW активен${NC}"

        # Проверяем правила
        PORT_80=$(sudo ufw status | grep "80/tcp" | grep "ALLOW" | wc -l)
        PORT_8082=$(sudo ufw status | grep "8082/tcp" | grep "DENY" | wc -l)
        PORT_8083=$(sudo ufw status | grep "8083/tcp" | grep "DENY" | wc -l)

        if [ $PORT_80 -gt 0 ]; then
            echo -e "${GREEN}✅ Порт 80 разрешен${NC}"
        else
            echo -e "${RED}❌ Порт 80 НЕ разрешен в firewall${NC}"
            echo -e "   Выполните: ${BLUE}sudo ufw allow 80/tcp${NC}"
        fi

        if [ $PORT_8082 -gt 0 ]; then
            echo -e "${GREEN}✅ Порт 8082 закрыт (правильно)${NC}"
        else
            echo -e "${YELLOW}⚠️  Порт 8082 не имеет правила DENY${NC}"
            echo -e "   Рекомендуется: ${BLUE}sudo ufw deny 8082/tcp${NC}"
        fi

        if [ $PORT_8083 -gt 0 ]; then
            echo -e "${GREEN}✅ Порт 8083 закрыт (правильно)${NC}"
        else
            echo -e "${YELLOW}⚠️  Порт 8083 не имеет правила DENY${NC}"
            echo -e "   Рекомендуется: ${BLUE}sudo ufw deny 8083/tcp${NC}"
        fi
    else
        echo -e "${YELLOW}⚠️  UFW неактивен${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  UFW не установлен${NC}"
fi
echo ""

# 6. Проверка локальных endpoint'ов
echo -e "${YELLOW}[6/8] Проверка локальных endpoint'ов...${NC}"

# Health
if curl -s http://localhost/health | grep -q "OK"; then
    echo -e "${GREEN}✅ /health работает${NC}"
else
    echo -e "${RED}❌ /health не работает${NC}"
fi

# API через Nginx
if curl -s http://localhost/api/ > /dev/null 2>&1; then
    echo -e "${GREEN}✅ /api/ доступен через Nginx${NC}"
else
    echo -e "${RED}❌ /api/ не доступен через Nginx${NC}"
fi

# Traccar через Nginx
if curl -s http://localhost/traccar/ > /dev/null 2>&1; then
    echo -e "${GREEN}✅ /traccar/ доступен через Nginx${NC}"
else
    echo -e "${RED}❌ /traccar/ не доступен через Nginx${NC}"
fi
echo ""

# 7. Проверка внешнего доступа
echo -e "${YELLOW}[7/8] Проверка внешнего доступа (${EXTERNAL_IP})...${NC}"

# Health
if curl -s --max-time 5 http://${EXTERNAL_IP}/health | grep -q "OK"; then
    echo -e "${GREEN}✅ http://${EXTERNAL_IP}/health доступен извне${NC}"
else
    echo -e "${RED}❌ http://${EXTERNAL_IP}/health НЕ доступен извне${NC}"
    echo -e "   ${YELLOW}Возможные причины:${NC}"
    echo -e "   - Port Forwarding не настроен на роутере"
    echo -e "   - Firewall блокирует порт"
    echo -e "   - Провайдер блокирует порт 80"
fi

# API
if curl -s --max-time 5 http://${EXTERNAL_IP}/api/ > /dev/null 2>&1; then
    echo -e "${GREEN}✅ http://${EXTERNAL_IP}/api/ доступен извне${NC}"
else
    echo -e "${RED}❌ http://${EXTERNAL_IP}/api/ НЕ доступен извне${NC}"
fi
echo ""

# 8. Проверка занятых портов
echo -e "${YELLOW}[8/8] Список открытых портов на сервере:${NC}"
echo -e "${BLUE}Порт    Процесс${NC}"
sudo netstat -tulpn 2>/dev/null | grep LISTEN | awk '{print $4, $7}' | sed 's/.*://g' | sort -n | while read line; do
    PORT=$(echo $line | awk '{print $1}')
    PROCESS=$(echo $line | awk '{print $2}')

    # Подсветка важных портов
    if [ "$PORT" = "80" ] || [ "$PORT" = "443" ]; then
        echo -e "${GREEN}$PORT     $PROCESS${NC}"
    elif [ "$PORT" = "8082" ] || [ "$PORT" = "8083" ]; then
        echo -e "${YELLOW}$PORT    $PROCESS${NC}"
    else
        echo "$PORT    $PROCESS"
    fi
done
echo ""

# Итоговая сводка
echo -e "${BLUE}╔════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                ИТОГОВАЯ СВОДКА                 ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}Внутренний IP:${NC} $INTERNAL_IP"
echo -e "${GREEN}Внешний IP:${NC} $EXTERNAL_IP"
echo ""
echo -e "${BLUE}Для настройки роутера используйте:${NC}"
echo -e "  ${GREEN}Внешний порт:${NC} 80"
echo -e "  ${GREEN}Внутренний IP:${NC} $INTERNAL_IP"
echo -e "  ${GREEN}Внутренний порт:${NC} 80"
echo -e "  ${GREEN}Протокол:${NC} TCP"
echo ""
echo -e "${BLUE}Доступные URL после настройки Port Forwarding:${NC}"
echo -e "  ${GREEN}API:${NC} http://${EXTERNAL_IP}/api/"
echo -e "  ${GREEN}Traccar:${NC} http://${EXTERNAL_IP}/traccar/"
echo -e "  ${GREEN}Health:${NC} http://${EXTERNAL_IP}/health"
echo ""

# Проверка через онлайн сервис
echo -e "${YELLOW}Для проверки открытости порта извне используйте:${NC}"
echo -e "  ${BLUE}https://canyouseeme.org/${NC}"
echo -e "  IP: ${GREEN}${EXTERNAL_IP}${NC}, Port: ${GREEN}80${NC}"
echo ""
