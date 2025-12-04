import axios from 'axios';

const API_BASE_URL = 'http://localhost:8083/api';

const api = axios.create({
  baseURL: API_BASE_URL,
});

// Интерцептор для добавления токена
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('authToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Интерцептор для обработки ошибок
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('authToken');
      window.location.href = '/login';
    } else if (error.response?.status === 403) {
      // 403 Forbidden - пользователь не имеет доступа (например, клиент пытается войти в веб-интерфейс)
      if (error.response?.data?.error === 'PROBATIONER_WEB_ACCESS_DENIED') {
        localStorage.removeItem('authToken');
        window.location.href = '/login';
        alert('Доступ запрещён. Клиенты могут использовать только мобильное приложение.');
      }
    }
    return Promise.reject(error);
  }
);

export const authAPI = {
  login: (login: string, password: string) =>
    api.post('/auth/login', { login, password }),

  getMe: () => api.get('/auth/me'),

  logout: () => api.post('/auth/logout')
};

export const registryAPI = {
  getClients: () => api.get('/registry'),

  createClient: (clientData: FormData) =>
    api.post('/registry', clientData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }),

  updateClient: (id: number, clientData: FormData) =>
    api.put(`/registry/${id}`, clientData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }),

  deleteClient: (id: number) => api.delete(`/registry/${id}`)
};

export const devicesAPI = {
  getDevices: () => api.get('/devices'),
  
  getDevice: (id: number) => api.get(`/devices/${id}`),
  
  updateDevice: (id: number, data: any) => 
    api.put(`/devices/${id}`, data)
};

export const eventsAPI = {
  getEvents: (params?: { from?: string; to?: string; deviceId?: number }) => 
    api.get('/events', { params })
};

export const faceCheckAPI = {
  verifyFace: (userId: string, file: File) => {
    const formData = new FormData();
    formData.append('userId', userId);
    formData.append('file', file);
    return api.post('/facecheck/verify', formData);
  },

  sendResult: (data: any) => api.post('/facecheck/result', data)
};

export const adminAPI = {
  // Employee management
  deleteEmployee: (id: number) => api.delete(`/admin/employees/${id}`),

  updateEmployee: (id: number, data: any) => api.put(`/admin/employees/${id}`, data),

  changeEmployeePassword: (id: number, newPassword: string) =>
    api.put(`/admin/employees/${id}/password`, { newPassword })
};

// services/api.ts
export const positionsAPI = {
  getPositions: () => api.get('/positions'),
  
  getDeviceTrack: (deviceId: number, from: string, to: string) => 
    api.get(`/positions/${deviceId}/track`, { params: { from, to } }),
  
  sendPosition: (positionData: any) => 
    api.post('/positions', positionData)
};

export default api;