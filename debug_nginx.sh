#!/bin/bash
# Скрипт диагностики проблем с Nginx и Backend

echo "======================================"
echo "🔍 ДИАГНОСТИКА PROBATION SYSTEM"
echo "======================================"
echo ""

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 1. Проверка установки Nginx
echo "1. Проверка Nginx..."
if command -v nginx &> /dev/null; then
    echo -e "${GREEN}✓ Nginx установлен${NC}"
    nginx -v 2>&1
else
    echo -e "${RED}✗ Nginx НЕ установлен${NC}"
    echo "Решение: sudo apt update && sudo apt install -y nginx"
fi
echo ""

# 2. Проверка статуса Nginx
echo "2. Проверка статуса Nginx..."
if systemctl is-active --quiet nginx 2>/dev/null; then
    echo -e "${GREEN}✓ Nginx запущен${NC}"
else
    echo -e "${RED}✗ Nginx НЕ запущен${NC}"
    echo "Решение: sudo systemctl start nginx"
fi
echo ""

# 3. Проверка портов
echo "3. Проверка открытых портов..."
echo "Порт 80 (Nginx):"
if sudo netstat -tulpn 2>/dev/null | grep -q ":80 "; then
    sudo netstat -tulpn | grep ":80 "
    echo -e "${GREEN}✓ Порт 80 открыт${NC}"
else
    echo -e "${RED}✗ Порт 80 НЕ слушается${NC}"
fi

echo ""
echo "Порт 8083 (Spring Boot Backend):"
if netstat -tulpn 2>/dev/null | grep -q ":8083 "; then
    netstat -tulpn | grep ":8083 "
    echo -e "${GREEN}✓ Порт 8083 открыт (Backend работает)${NC}"
else
    echo -e "${RED}✗ Порт 8083 НЕ слушается (Backend не запущен)${NC}"
fi

echo ""
echo "Порт 8082 (Traccar):"
if netstat -tulpn 2>/dev/null | grep -q ":8082 "; then
    netstat -tulpn | grep ":8082 "
    echo -e "${GREEN}✓ Порт 8082 открыт (Traccar работает)${NC}"
else
    echo -e "${YELLOW}⚠ Порт 8082 НЕ слушается (Traccar не запущен)${NC}"
fi
echo ""

# 4. Проверка конфигурации Nginx
echo "4. Проверка конфигурации Nginx..."
if [ -f /etc/nginx/nginx.conf ]; then
    echo "Текущая конфигурация: /etc/nginx/nginx.conf"

    # Проверяем наличие наших настроек
    if grep -q "85.113.27.42" /etc/nginx/nginx.conf 2>/dev/null; then
        echo -e "${GREEN}✓ Найдена конфигурация для 85.113.27.42${NC}"
    else
        echo -e "${RED}✗ Конфигурация для 85.113.27.42 НЕ найдена${NC}"
        echo "Решение: Применить nginx.conf из репозитория"
    fi

    # Проверяем наличие location /api/
    if grep -q "location /api/" /etc/nginx/nginx.conf 2>/dev/null; then
        echo -e "${GREEN}✓ Найден location /api/${NC}"
    else
        echo -e "${RED}✗ Location /api/ НЕ настроен${NC}"
    fi

    # Проверяем синтаксис
    echo ""
    echo "Проверка синтаксиса конфигурации:"
    if sudo nginx -t 2>&1; then
        echo -e "${GREEN}✓ Синтаксис конфигурации правильный${NC}"
    else
        echo -e "${RED}✗ Ошибка в синтаксисе конфигурации${NC}"
    fi
else
    echo -e "${RED}✗ Файл /etc/nginx/nginx.conf не найден${NC}"
fi
echo ""

# 5. Проверка логов Nginx
echo "5. Последние ошибки Nginx:"
if [ -f /var/log/nginx/error.log ]; then
    echo "Последние 10 строк из error.log:"
    sudo tail -10 /var/log/nginx/error.log 2>/dev/null || echo "Логи недоступны"
else
    echo -e "${YELLOW}⚠ Лог-файл не найден${NC}"
fi
echo ""

# 6. Тестирование локальных подключений
echo "6. Тестирование локальных подключений..."

echo "Проверка Backend (localhost:8083):"
BACKEND_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8083/api/ 2>/dev/null)
if [ "$BACKEND_CODE" = "200" ] || [ "$BACKEND_CODE" = "401" ] || [ "$BACKEND_CODE" = "404" ]; then
    echo -e "${GREEN}✓ Backend отвечает на localhost:8083 (код: $BACKEND_CODE)${NC}"
else
    echo -e "${RED}✗ Backend НЕ отвечает на localhost:8083${NC}"
    echo "Решение: Запустить Spring Boot приложение"
fi

echo ""
echo "Проверка Nginx (localhost:80):"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost/health 2>/dev/null)
if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ Nginx отвечает на localhost:80/health (код: $HTTP_CODE)${NC}"
elif [ -n "$HTTP_CODE" ]; then
    echo -e "${YELLOW}⚠ Nginx отвечает, но код: $HTTP_CODE (ожидается 200)${NC}"
else
    echo -e "${RED}✗ Nginx НЕ отвечает на localhost:80${NC}"
fi
echo ""

# 7. Проверка внешнего доступа
echo "7. Проверка внешнего доступа..."
EXTERNAL_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://85.113.27.42/health 2>/dev/null)
if [ "$EXTERNAL_CODE" = "200" ]; then
    echo -e "${GREEN}✓ Сервер доступен извне: http://85.113.27.42/health (код: $EXTERNAL_CODE)${NC}"
elif [ "$EXTERNAL_CODE" = "404" ]; then
    echo -e "${YELLOW}⚠ Сервер доступен, но вернул 404 - конфигурация не применена${NC}"
else
    echo -e "${RED}✗ Сервер недоступен извне (код: $EXTERNAL_CODE)${NC}"
fi
echo ""

# 8. Итоговая диагностика
echo "======================================"
echo "📋 ИТОГОВАЯ ДИАГНОСТИКА"
echo "======================================"

ISSUES=0

# Проверяем все критичные компоненты
if ! command -v nginx &> /dev/null; then
    echo -e "${RED}🔴 Nginx не установлен${NC}"
    ISSUES=$((ISSUES+1))
fi

if ! systemctl is-active --quiet nginx 2>/dev/null; then
    echo -e "${RED}🔴 Nginx не запущен${NC}"
    ISSUES=$((ISSUES+1))
fi

if [ "$BACKEND_CODE" != "200" ] && [ "$BACKEND_CODE" != "401" ] && [ "$BACKEND_CODE" != "404" ]; then
    echo -e "${RED}🔴 Backend (Spring Boot) не запущен на порту 8083${NC}"
    ISSUES=$((ISSUES+1))
fi

if ! grep -q "85.113.27.42" /etc/nginx/nginx.conf 2>/dev/null; then
    echo -e "${RED}🔴 Nginx конфигурация не применена${NC}"
    ISSUES=$((ISSUES+1))
fi

if [ "$EXTERNAL_CODE" = "404" ]; then
    echo -e "${YELLOW}🟡 Сервер доступен извне, но возвращает 404 (нужно проверить конфигурацию)${NC}"
    ISSUES=$((ISSUES+1))
elif [ "$EXTERNAL_CODE" != "200" ]; then
    echo -e "${RED}🔴 Сервер недоступен извне${NC}"
    ISSUES=$((ISSUES+1))
fi

echo ""
if [ $ISSUES -eq 0 ]; then
    echo -e "${GREEN}✅ Система работает корректно!${NC}"
else
    echo -e "${YELLOW}⚠️  Найдено проблем: $ISSUES${NC}"
    echo ""
    echo "Рекомендуемые действия:"
    echo "1. Запустите: sudo ./fix_nginx.sh для автоматического исправления"
    echo "2. Или следуйте инструкциям в NGINX_SETUP.md"
fi

echo ""
echo "======================================"
echo "Дата проверки: $(date)"
echo "======================================"
