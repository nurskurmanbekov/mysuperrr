import * as Location from 'expo-location';
import * as TaskManager from 'expo-task-manager';
import * as NetInfo from '@react-native-community/netinfo';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Platform } from 'react-native';
import { TRACCAR_CONFIG } from '../utils/constants';

const GPS_TASK_NAME = 'BACKGROUND_LOCATION_TASK';
const OFFLINE_GPS_KEY = 'OFFLINE_GPS_DATA';

class GPSService {
  constructor() {
    this.isTracking = false;
    this.userId = null;
    this.isOnline = true;
    this.syncInterval = null;

    // Подписка на изменения сети
    this.setupNetworkListener();
  }

  // Настройка слушателя сети
  setupNetworkListener() {
    NetInfo.addEventListener(state => {
      const wasOffline = !this.isOnline;
      this.isOnline = state.isConnected;

      console.log('Network status changed:', this.isOnline ? 'Online' : 'Offline');

      // Если стали онлайн после оффлайна - синхронизируем
      if (wasOffline && this.isOnline) {
        console.log('Network restored, syncing offline data...');
        this.syncOfflineData();
      }
    });
  }

  // Определяем фоновую задачу
  defineBackgroundTask() {
    TaskManager.defineTask(GPS_TASK_NAME, async ({ data, error }) => {
      if (error) {
        console.log('GPS task error:', error);
        return;
      }
      
      if (data) {
        const { locations } = data;
        const location = locations[0];
        
        if (location && this.userId) {
          await this.sendLocationToTraccar(this.userId, location);
        }
      }
    });
  }

  // Запрос разрешений
  async requestPermissions() {
    try {
      const { status } = await Location.requestForegroundPermissionsAsync();
      
      if (status !== 'granted') {
        console.log('Foreground location permission denied');
        return false;
      }

      const backgroundStatus = await Location.requestBackgroundPermissionsAsync();
      
      if (backgroundStatus.status !== 'granted') {
        console.log('Background location permission denied');
        return false;
      }

      return true;
    } catch (error) {
      console.log('Permission error:', error);
      return false;
    }
  }

  // Запуск отслеживания
  async startTracking(userId) {
    try {
      this.userId = userId;
      
      const hasPermission = await this.requestPermissions();
      if (!hasPermission) {
        throw new Error('Location permission not granted');
      }

      // Настройка отслеживания в фоне
      await Location.startLocationUpdatesAsync(GPS_TASK_NAME, {
        accuracy: Location.Accuracy.BestForNavigation,
        distanceInterval: 50, // Метры
        timeInterval: 30000, // 30 секунд
        deferredUpdatesInterval: 30000,
        deferredUpdatesDistance: 50,
        foregroundService: {
          notificationTitle: 'Отслеживание местоположения',
          notificationBody: 'Активно',
          notificationColor: '#007AFF',
        },
      });

      this.isTracking = true;
      console.log('GPS tracking started for user:', userId);

      // Запускаем периодическую синхронизацию
      this.startPeriodicSync();

      // Также слушаем обновления в реальном времени
      this.locationSubscription = await Location.watchPositionAsync(
        {
          accuracy: Location.Accuracy.High,
          distanceInterval: 50,
          timeInterval: 15000,
        },
        (location) => {
          this.sendLocationToTraccar(userId, location);
        }
      );

    } catch (error) {
      console.log('Start tracking error:', error);
      throw error;
    }
  }

  // Отправка данных в Traccar через Nginx
  async sendLocationToTraccar(userId, location) {
    try {
      const positionData = {
        id: userId, // uniqueId устройства (ИНН)
        lat: location.coords.latitude,
        lon: location.coords.longitude,
        speed: location.coords.speed || 0,
        bearing: location.coords.heading || 0,
        altitude: location.coords.altitude || 0,
        accuracy: location.coords.accuracy || 0,
        batt: 85,
        timestamp: new Date(location.timestamp).getTime(),
        attributes: {
          provider: 'mobile',
          batteryLevel: 85,
          activity: 'moving',
        }
      };

      console.log('Sending location to Traccar via Nginx:', positionData);

      // Проверяем наличие сети
      if (this.isOnline) {
        // Отправляем через Nginx на порт 80
        const success = await this.sendToTraccarServer(positionData);

        if (!success) {
          // Если отправка не удалась - сохраняем оффлайн
          await this.saveOfflineLocation(positionData);
        }
      } else {
        // Сети нет - сохраняем оффлайн
        console.log('No network, saving location offline');
        await this.saveOfflineLocation(positionData);
      }

    } catch (error) {
      console.log('Send to Traccar error:', error);
      // Сохраняем оффлайн при ошибке
      await this.saveOfflineLocation(positionData);
    }
  }

  // Сохранение GPS данных оффлайн
  async saveOfflineLocation(positionData) {
    try {
      // Получаем существующие оффлайн данные
      const existingDataStr = await AsyncStorage.getItem(OFFLINE_GPS_KEY);
      const existingData = existingDataStr ? JSON.parse(existingDataStr) : [];

      // Добавляем новые данные
      existingData.push({
        ...positionData,
        savedAt: new Date().toISOString()
      });

      // Сохраняем обратно (ограничиваем до 1000 точек)
      const limitedData = existingData.slice(-1000);
      await AsyncStorage.setItem(OFFLINE_GPS_KEY, JSON.stringify(limitedData));

      console.log(`Saved location offline. Total offline points: ${limitedData.length}`);
    } catch (error) {
      console.log('Error saving offline location:', error);
    }
  }

  // Синхронизация оффлайн данных
  async syncOfflineData() {
    try {
      const offlineDataStr = await AsyncStorage.getItem(OFFLINE_GPS_KEY);

      if (!offlineDataStr) {
        console.log('No offline data to sync');
        return;
      }

      const offlineData = JSON.parse(offlineDataStr);

      if (offlineData.length === 0) {
        console.log('No offline data to sync');
        return;
      }

      console.log(`Starting sync of ${offlineData.length} offline GPS points`);

      // Отправляем данные порциями по 10 точек
      const batchSize = 10;
      let successCount = 0;
      let failedData = [];

      for (let i = 0; i < offlineData.length; i += batchSize) {
        const batch = offlineData.slice(i, i + batchSize);

        for (const positionData of batch) {
          const success = await this.sendToTraccarServer(positionData);

          if (success) {
            successCount++;
          } else {
            failedData.push(positionData);
          }

          // Небольшая задержка между запросами
          await new Promise(resolve => setTimeout(resolve, 100));
        }
      }

      console.log(`Sync completed. Success: ${successCount}, Failed: ${failedData.length}`);

      // Сохраняем только неотправленные данные
      if (failedData.length > 0) {
        await AsyncStorage.setItem(OFFLINE_GPS_KEY, JSON.stringify(failedData));
      } else {
        await AsyncStorage.removeItem(OFFLINE_GPS_KEY);
      }

    } catch (error) {
      console.log('Error syncing offline data:', error);
    }
  }

  // Получение количества оффлайн точек
  async getOfflinePointsCount() {
    try {
      const offlineDataStr = await AsyncStorage.getItem(OFFLINE_GPS_KEY);
      if (!offlineDataStr) return 0;

      const offlineData = JSON.parse(offlineDataStr);
      return offlineData.length;
    } catch (error) {
      console.log('Error getting offline points count:', error);
      return 0;
    }
  }

  // Отправка на Traccar сервер через Nginx
  async sendToTraccarServer(positionData) {
    try {
      // Отправляем через Spring Boot бэкенд
      const success = await this.sendViaSpringBoot(positionData);
      return success;
    } catch (error) {
      console.log('Send to Traccar server error:', error);
      return false;
    }
  }

  // Отправка через Spring Boot бэкенд
  async sendViaSpringBoot(positionData) {
    try {
      const API_BASE_URL = 'http://85.113.27.42:80/api';
      const response = await fetch(`${API_BASE_URL}/traccar/positions`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(positionData),
        timeout: 10000, // 10 секунд таймаут
      });

      if (!response.ok) {
        throw new Error(`Spring Boot error! status: ${response.status}`);
      }

      console.log('Location sent via Spring Boot successfully');
      return true;
    } catch (error) {
      console.log('Spring Boot send error:', error);
      return false;
    }
  }

  async getAuthToken() {
    // Получение токена из AsyncStorage
    const token = await AsyncStorage.getItem('authToken');
    return token;
  }

  // Остановка отслеживания
  async stopTracking() {
    try {
      if (this.locationSubscription) {
        this.locationSubscription.remove();
        this.locationSubscription = null;
      }

      await Location.stopLocationUpdatesAsync(GPS_TASK_NAME);

      // Остановка периодической синхронизации
      if (this.syncInterval) {
        clearInterval(this.syncInterval);
        this.syncInterval = null;
      }

      this.isTracking = false;
      this.userId = null;

      console.log('GPS tracking stopped');
    } catch (error) {
      console.log('Stop tracking error:', error);
    }
  }

  // Запуск периодической синхронизации
  startPeriodicSync() {
    // Синхронизация каждые 5 минут
    this.syncInterval = setInterval(() => {
      if (this.isOnline) {
        this.syncOfflineData();
      }
    }, 5 * 60 * 1000); // 5 минут
  }
}

// Создаем экземпляр и определяем задачу
const gpsService = new GPSService();
gpsService.defineBackgroundTask();

export default gpsService;