#!/bin/bash

# Скрипт установки и настройки Nginx для Probation System

echo "🚀 Установка и настройка Nginx..."

# Цвета для вывода
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Проверка root прав
if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}❌ Запустите скрипт с правами root: sudo ./nginx-setup.sh${NC}"
    exit 1
fi

echo -e "${YELLOW}📦 Шаг 1: Установка Nginx...${NC}"
apt update
apt install -y nginx

echo -e "${YELLOW}📝 Шаг 2: Копирование конфигурации...${NC}"
# Бэкап старой конфигурации
if [ -f /etc/nginx/nginx.conf ]; then
    cp /etc/nginx/nginx.conf /etc/nginx/nginx.conf.backup.$(date +%Y%m%d_%H%M%S)
fi

# Копируем нашу конфигурацию
cp /home/user/mysuperrr/nginx.conf /etc/nginx/nginx.conf

echo -e "${YELLOW}✅ Шаг 3: Проверка конфигурации...${NC}"
nginx -t

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Конфигурация корректна${NC}"
else
    echo -e "${RED}❌ Ошибка в конфигурации. Восстанавливаем backup...${NC}"
    cp /etc/nginx/nginx.conf.backup.* /etc/nginx/nginx.conf
    exit 1
fi

echo -e "${YELLOW}🔄 Шаг 4: Перезапуск Nginx...${NC}"
systemctl restart nginx
systemctl enable nginx

echo -e "${YELLOW}🔥 Шаг 5: Настройка firewall (ufw)...${NC}"
# Разрешаем HTTP и HTTPS
ufw allow 80/tcp
ufw allow 443/tcp

# Закрываем прямой доступ к портам (оставляем только через Nginx)
echo -e "${YELLOW}⚠️  Хотите закрыть прямой доступ к портам 8082 и 8083? (y/n)${NC}"
read -p "Это безопаснее, но если что-то пойдет не так - нужно будет открывать вручную: " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    ufw deny 8082/tcp
    ufw deny 8083/tcp
    echo -e "${GREEN}✅ Порты 8082 и 8083 закрыты для прямого доступа${NC}"
else
    echo -e "${YELLOW}⚠️  Порты 8082 и 8083 остались открытыми${NC}"
fi

echo -e "${YELLOW}📊 Шаг 6: Проверка статуса...${NC}"
systemctl status nginx --no-pager

echo ""
echo -e "${GREEN}✅ Nginx успешно настроен!${NC}"
echo ""
echo "📋 Полезные команды:"
echo "  sudo systemctl status nginx    - статус"
echo "  sudo systemctl restart nginx   - перезапуск"
echo "  sudo nginx -t                  - проверка конфигурации"
echo "  sudo tail -f /var/log/nginx/probation_access.log  - логи запросов"
echo "  sudo tail -f /var/log/nginx/probation_error.log   - логи ошибок"
echo ""
echo "🌐 Ваши URL:"
echo "  Backend API: http://85.113.27.42/api/"
echo "  Traccar UI:  http://85.113.27.42/traccar/"
echo "  Health:      http://85.113.27.42/health"
echo ""
echo "📱 Обновите мобильное приложение:"
echo "  iosandroid/src/utils/constants.js"
echo "  Замените ngrok URL на: http://85.113.27.42"
echo ""
