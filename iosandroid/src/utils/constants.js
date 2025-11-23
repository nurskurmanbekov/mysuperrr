// Базовые настройки API
export const API_CONFIG = {
  BASE_URL: 'http://85.113.27.42/api',  // Nginx → Spring Boot :8083
  TIMEOUT: 10000,
  RETRY_ATTEMPTS: 3,
  RETRY_DELAY: 1000,
};

// URL для Traccar GPS данных
export const TRACCAR_CONFIG = {
  // Nginx проксирует на Traccar :8082
  GPS_ENDPOINT: 'http://85.113.27.42',
  API_ENDPOINT: 'http://85.113.27.42/api/traccar',
  TRACCAR_PORT: 80,  // Через Nginx используем стандартный порт
};

// Коды ошибок
export const ERROR_CODES = {
  NETWORK_ERROR: 'NETWORK_ERROR',
  UNAUTHORIZED: 'UNAUTHORIZED',
  FORBIDDEN: 'FORBIDDEN',
  NOT_FOUND: 'NOT_FOUND',
  SERVER_ERROR: 'SERVER_ERROR',
  VALIDATION_ERROR: 'VALIDATION_ERROR',
  FACE_CHECK_FAILED: 'FACE_CHECK_FAILED',
  CAMERA_PERMISSION_DENIED: 'CAMERA_PERMISSION_DENIED',
  GALLERY_PERMISSION_DENIED: 'GALLERY_PERMISSION_DENIED',
};

// Сообщения об ошибках
export const ERROR_MESSAGES = {
  [ERROR_CODES.NETWORK_ERROR]: 'Ошибка сети. Проверьте подключение к интернету',
  [ERROR_CODES.UNAUTHORIZED]: 'Неверные учетные данные',
  [ERROR_CODES.FORBIDDEN]: 'Доступ запрещен',
  [ERROR_CODES.NOT_FOUND]: 'Ресурс не найден',
  [ERROR_CODES.SERVER_ERROR]: 'Ошибка сервера. Попробуйте позже',
  [ERROR_CODES.VALIDATION_ERROR]: 'Ошибка валидации данных',
  [ERROR_CODES.FACE_CHECK_FAILED]: 'Не удалось выполнить проверку лица',
  [ERROR_CODES.CAMERA_PERMISSION_DENIED]: 'Необходим доступ к камере для проверки Face-ID',
  [ERROR_CODES.GALLERY_PERMISSION_DENIED]: 'Необходим доступ к галерее для выбора фото',
};

// Настройки Face-ID
export const FACE_CHECK_CONFIG = {
  MAX_DISTANCE: 0.6, // Максимальное расстояние для успешной проверки
  MIN_FACE_SIZE: 100, // Минимальный размер лица в пикселях
  QUALITY: 0.8, // Качество фото (0-1)
  TIMEOUT: 30000, // Таймаут проверки в ms
};

// Типы уведомлений FCM
export const NOTIFICATION_TYPES = {
  FACE_CHECK: 'face_check',
  SYSTEM_ALERT: 'system_alert',
  DEVICE_UPDATE: 'device_update',
  SECURITY_ALERT: 'security_alert',
};

// Статусы проверок Face-ID
export const FACE_CHECK_OUTCOMES = {
  OK: 'ok',
  FAILED: 'failed',
  DECLINED: 'declined',
  FAILED_NETWORK: 'failed_network',
  LATE_OK: 'late_ok',
  LATE_FAILED: 'late_failed',
};

// Тексты для статусов проверок
export const FACE_CHECK_OUTCOME_TEXTS = {
  [FACE_CHECK_OUTCOMES.OK]: 'Успешно',
  [FACE_CHECK_OUTCOMES.FAILED]: 'Не пройдено',
  [FACE_CHECK_OUTCOMES.DECLINED]: 'Отклонено',
  [FACE_CHECK_OUTCOMES.FAILED_NETWORK]: 'Ошибка сети',
  [FACE_CHECK_OUTCOMES.LATE_OK]: 'Успешно (поздно)',
  [FACE_CHECK_OUTCOMES.LATE_FAILED]: 'Не пройдено (поздно)',
};

// Настройки камеры
export const CAMERA_CONFIG = {
  RATIO: '1:1',
  TYPE: 'front', // 'front' | 'back'
  FLASH_MODE: 'off', // 'on' | 'off' | 'auto'
  AUTO_FOCUS: 'on',
  WHITE_BALANCE: 'auto',
};

// Настройки хранилища
export const STORAGE_KEYS = {
  AUTH_TOKEN: 'authToken',
  USER_SETTINGS: 'userSettings',
  LAST_LOGIN_INN: 'lastLoginINN',
  FIRST_LAUNCH: 'firstLaunch',
  FCM_TOKEN: 'fcmToken',
};

// Цвета приложения
export const COLORS = {
  PRIMARY: '#007AFF',
  SECONDARY: '#6C757D',
  SUCCESS: '#4CAF50',
  ERROR: '#F44336',
  WARNING: '#FF9800',
  INFO: '#2196F3',
  
  BACKGROUND: '#F5F5F5',
  SURFACE: '#FFFFFF',
  TEXT_PRIMARY: '#333333',
  TEXT_SECONDARY: '#666666',
  TEXT_DISABLED: '#999999',
  BORDER: '#DDDDDD',
  
  // Семантические цвета
  FACE_CHECK_SUCCESS: '#4CAF50',
  FACE_CHECK_ERROR: '#F44336',
  FACE_CHECK_PENDING: '#FF9800',
};

// Размеры и отступы
export const SPACING = {
  XS: 4,
  S: 8,
  M: 16,
  L: 24,
  XL: 32,
  XXL: 48,
};

// Размеры шрифтов
export const FONT_SIZES = {
  XS: 12,
  S: 14,
  M: 16,
  L: 18,
  XL: 20,
  XXL: 24,
  XXXL: 32,
};

// Настройки навигации
export const NAVIGATION = {
  ANIMATION_DURATION: 300,
  HEADER_HEIGHT: 56,
  TAB_BAR_HEIGHT: 56,
};

// Версия приложения
export const APP_VERSION = '1.0.0';
export const BUILD_NUMBER = '1';

// Флаги для разработки
export const IS_DEVELOPMENT = __DEV__;
export const LOG_NETWORK_REQUESTS = __DEV__;
export const ENABLE_DEBUG_MENU = __DEV__;

// Максимальные значения
export const LIMITS = {
  MAX_PHOTO_SIZE: 10 * 1024 * 1024, // 10MB
  MAX_FACE_CHECK_ATTEMPTS: 3,
  SESSION_TIMEOUT: 24 * 60 * 60 * 1000, // 24 часа
};

// URL для разных сред
export const ENV_URLS = {
  DEVELOPMENT: 'http://localhost:8080/api',
  STAGING: 'https://staging-your-api.com/api',
  PRODUCTION: 'https://your-production-api.com/api',
};

// Получение текущего URL API на основе среды
export const getApiBaseUrl = () => {
  if (IS_DEVELOPMENT) {
    return ENV_URLS.DEVELOPMENT;
  }
  // Здесь можно добавить логику для определения среды
  return ENV_URLS.PRODUCTION;
};

export default {
  API_CONFIG,
  ERROR_CODES,
  ERROR_MESSAGES,
  FACE_CHECK_CONFIG,
  NOTIFICATION_TYPES,
  FACE_CHECK_OUTCOMES,
  FACE_CHECK_OUTCOME_TEXTS,
  CAMERA_CONFIG,
  STORAGE_KEYS,
  COLORS,
  SPACING,
  FONT_SIZES,
  NAVIGATION,
  APP_VERSION,
  BUILD_NUMBER,
  IS_DEVELOPMENT,
  LOG_NETWORK_REQUESTS,
  ENABLE_DEBUG_MENU,
  LIMITS,
  ENV_URLS,
  getApiBaseUrl,
};