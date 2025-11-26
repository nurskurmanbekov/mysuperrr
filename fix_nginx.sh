#!/bin/bash
# Скрипт автоматического исправления Nginx конфигурации

echo "======================================"
echo "🔧 ИСПРАВЛЕНИЕ NGINX КОНФИГУРАЦИИ"
echo "======================================"
echo ""

# Цвета
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Проверка что скрипт запущен с правами root
if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}Этот скрипт должен быть запущен с правами root${NC}"
    echo "Используйте: sudo ./fix_nginx.sh"
    exit 1
fi

# 1. Установка Nginx если не установлен
echo "Шаг 1: Проверка установки Nginx..."
if ! command -v nginx &> /dev/null; then
    echo "Nginx не установлен. Устанавливаю..."
    apt update
    apt install -y nginx
    echo -e "${GREEN}✓ Nginx установлен${NC}"
else
    echo -e "${GREEN}✓ Nginx уже установлен${NC}"
fi
echo ""

# 2. Остановка Nginx
echo "Шаг 2: Остановка Nginx..."
systemctl stop nginx 2>/dev/null || true
echo -e "${GREEN}✓ Nginx остановлен${NC}"
echo ""

# 3. Бэкап текущей конфигурации
echo "Шаг 3: Создание бэкапа текущей конфигурации..."
if [ -f /etc/nginx/nginx.conf ]; then
    BACKUP_NAME="/etc/nginx/nginx.conf.backup.$(date +%Y%m%d_%H%M%S)"
    cp /etc/nginx/nginx.conf "$BACKUP_NAME"
    echo -e "${GREEN}✓ Бэкап создан: $BACKUP_NAME${NC}"
else
    echo "Текущая конфигурация не найдена (первая установка)"
fi
echo ""

# 4. Применение новой конфигурации
echo "Шаг 4: Применение новой конфигурации..."
REPO_DIR="/home/user/mysuperrr"

if [ -f "$REPO_DIR/nginx.conf" ]; then
    cp "$REPO_DIR/nginx.conf" /etc/nginx/nginx.conf
    echo -e "${GREEN}✓ Конфигурация скопирована из $REPO_DIR/nginx.conf${NC}"
else
    echo -e "${RED}✗ Файл nginx.conf не найден в $REPO_DIR${NC}"
    echo "Убедитесь что вы находитесь в правильной директории"
    exit 1
fi
echo ""

# 5. Проверка синтаксиса
echo "Шаг 5: Проверка синтаксиса конфигурации..."
if nginx -t 2>&1; then
    echo -e "${GREEN}✓ Синтаксис конфигурации правильный${NC}"
else
    echo -e "${RED}✗ Ошибка в синтаксисе конфигурации${NC}"
    echo "Восстанавливаю бэкап..."
    if [ -n "$BACKUP_NAME" ] && [ -f "$BACKUP_NAME" ]; then
        cp "$BACKUP_NAME" /etc/nginx/nginx.conf
        echo "Бэкап восстановлен"
    fi
    exit 1
fi
echo ""

# 6. Запуск Nginx
echo "Шаг 6: Запуск Nginx..."
systemctl start nginx
systemctl enable nginx 2>/dev/null || true
echo -e "${GREEN}✓ Nginx запущен и добавлен в автозагрузку${NC}"
echo ""

# 7. Проверка работы
echo "Шаг 7: Проверка работы..."
sleep 2

echo "Проверка localhost..."
HEALTH_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost/health 2>/dev/null)
if [ "$HEALTH_CODE" = "200" ]; then
    echo -e "${GREEN}✓ Nginx отвечает на /health (код: $HEALTH_CODE)${NC}"
    curl -s http://localhost/health
else
    echo -e "${YELLOW}⚠ Nginx вернул код: $HEALTH_CODE${NC}"
fi

echo ""
echo "Проверка внешнего доступа..."
EXTERNAL_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://85.113.27.42/health 2>/dev/null)
if [ "$EXTERNAL_CODE" = "200" ]; then
    echo -e "${GREEN}✓ Сервер доступен извне: http://85.113.27.42/health${NC}"
elif [ "$EXTERNAL_CODE" = "404" ]; then
    echo -e "${YELLOW}⚠ Сервер доступен, но вернул 404${NC}"
    echo "Возможные причины:"
    echo "  - Backend (Spring Boot) не запущен на порту 8083"
    echo "  - Проблема с проксированием"
else
    echo -e "${YELLOW}⚠ Внешний доступ вернул код: $EXTERNAL_CODE${NC}"
fi
echo ""

# 8. Проверка Backend
echo "Шаг 8: Проверка Backend..."
if netstat -tulpn 2>/dev/null | grep -q ":8083 "; then
    echo -e "${GREEN}✓ Backend работает на порту 8083${NC}"
else
    echo -e "${RED}✗ Backend НЕ запущен на порту 8083${NC}"
    echo ""
    echo "Для запуска Backend:"
    echo "  cd $REPO_DIR/FreshBackend"
    echo "  ./gradlew bootRun"
fi
echo ""

# 9. Итог
echo "======================================"
echo "📋 ИТОГ"
echo "======================================"
echo ""
echo -e "${GREEN}✅ Nginx успешно настроен и запущен${NC}"
echo ""
echo "Доступные endpoints:"
echo "  • API: http://85.113.27.42/api/"
echo "  • Traccar: http://85.113.27.42/traccar/"
echo "  • Health: http://85.113.27.42/health"
echo ""
echo "Следующие шаги:"
echo "  1. Убедитесь что Backend запущен на этом сервере"
echo "  2. Мобильное приложение уже настроено на 85.113.27.42"
echo "  3. Протестируйте подключение с телефона"
echo ""
echo "Полезные команды:"
echo "  • Проверка логов: sudo tail -f /var/log/nginx/error.log"
echo "  • Перезагрузка: sudo systemctl reload nginx"
echo "  • Статус: sudo systemctl status nginx"
echo ""
echo "======================================"
