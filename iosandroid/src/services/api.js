import axios from 'axios';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { API_CONFIG, TRACCAR_CONFIG } from '../utils/constants';

const api = axios.create({
  baseURL: API_CONFIG.BASE_URL,
  timeout: API_CONFIG.TIMEOUT,
});

// Добавляем токен к каждому запросу
api.interceptors.request.use(
  async (config) => {
    const token = await AsyncStorage.getItem('authToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export const authAPI = {
  login: (login, password) =>
    api.post('/auth/login', { login, password }),
  
  getMe: () => 
    api.get('/auth/me'),
  
  logout: () => 
    api.post('/auth/logout'),
};

export const faceCheckAPI = {
  verifyFace: (userId, file) => {
    const formData = new FormData();
    formData.append('userId', userId);
    formData.append('file', {
      uri: file.uri,
      type: 'image/jpeg',
      name: 'selfie.jpg',
    });
    
    return api.post('/facecheck/verify', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },
  
  sendResult: (data) => 
    api.post('/facecheck/result', data),
  
  registerFCMToken: (deviceUnique, fcmToken) => 
    api.post('/facecheck/register-token', {
      device_unique: deviceUnique,
      fcm_token: fcmToken,
    }),
};

export const deviceAPI = {
  getDeviceByUniqueId: (uniqueId) =>
    api.get(`/devices/by-unique-id/${uniqueId}`),

  getAllDevices: () =>
    api.get('/devices'),
};

export const traccarAPI = {
  // Отправка GPS данных через Nginx в Traccar
  sendPosition: (positionData) => {
    return fetch(TRACCAR_CONFIG.GPS_ENDPOINT, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(positionData),
    });
  },

  // API запросы к Traccar через /traccar/ путь
  getDevices: () => 
    api.get('/devices'), // Это пойдет в Spring Boot

  getTraccarDevices: () =>
    fetch(`${TRACCAR_CONFIG.API_ENDPOINT}/devices`, {
      headers: {
        'Authorization': 'Basic ' + btoa('admin:admin'), // Traccar auth
      },
    }),
};

export default api;