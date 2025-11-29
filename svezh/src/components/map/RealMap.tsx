// components/map/RealMap.tsx
import React, { useState, useEffect, useCallback } from 'react';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import api from '../../services/api';
import 'leaflet/dist/leaflet.css';
import './RealMap.css';
import L from 'leaflet';

// Кастомная иконка с фото осужденного
const createPhotoIcon = (client: ClientWithPosition) => {
  const photoUrl = client.photoKey
    ? `http://localhost:8083/api/faces/photos/${client.photoKey}`
    : `https://ui-avatars.com/api/?name=${encodeURIComponent(client.fio)}&background=3b82f6&color=fff&size=80`;

  const statusColor = client.status === 'online' ? '#10b981' : '#ef4444';

  return new L.DivIcon({
    html: `
      <div class="client-marker">
        <div class="client-avatar" style="border-color: ${statusColor}">
          <img src="${photoUrl}" alt="${client.fio}" onerror="this.src='https://ui-avatars.com/api/?name=${encodeURIComponent(client.fio)}&background=3b82f6&color=fff&size=80'" />
        </div>
        <div class="status-indicator" style="background-color: ${statusColor}"></div>
      </div>
    `,
    className: 'custom-client-marker',
    iconSize: [50, 50],
    iconAnchor: [25, 50],
  });
};

interface ClientWithPosition {
  id: number;
  fio: string;
  birthDate?: string;
  sex?: string;
  inn?: string;
  passportNumber?: string;
  registrationAddress?: string;
  actualAddress?: string;
  phoneNumber?: string;
  emergencyContact?: string;
  supervisionType?: string;
  supervisionStartDate?: string;
  supervisionEndDate?: string;
  districtName?: string;
  photoKey?: string;
  status: string;
  position?: {
    latitude: number;
    longitude: number;
    timestamp: string;
  };
}

// Координаты центра Кыргызстана для показа всей страны
const KYRGYZSTAN_CENTER = [41.20, 74.77] as [number, number];

// Границы Кыргызстана (юго-запад, северо-восток)
const KYRGYZSTAN_BOUNDS: [[number, number], [number, number]] = [
  [39.17, 69.25], // Юго-западный угол
  [43.24, 80.28]  // Северо-восточный угол
];

const RealMap: React.FC = () => {
  const [clients, setClients] = useState<ClientWithPosition[]>([]);
  const [loading, setLoading] = useState(true);
  const [isClient, setIsClient] = useState(false);

  const loadClients = useCallback(async () => {
    try {
      // Получаем клиентов и их последние GPS позиции из нашего backend
      const [clientsResponse, positionsResponse] = await Promise.all([
        api.get('/admin/clients'),
        api.get('/positions/latest')
      ]);

      const clientsData = clientsResponse.data || [];
      const positionsData = positionsResponse.data?.positions || [];

      // Создаем Map для быстрого поиска позиций по uniqueId
      const positionsMap = new Map();
      positionsData.forEach((pos: any) => {
        positionsMap.set(pos.uniqueId, pos);
      });

      // Объединяем клиентов с их позициями
      const clientsWithPositions = clientsData.map((client: any) => {
        const position = positionsMap.get(client.uniqueId || client.inn);

        // Определяем статус: online если позиция обновлялась < 5 минут назад
        let status = 'offline';
        if (position && position.serverTime) {
          const lastUpdate = new Date(position.serverTime);
          const now = new Date();
          const diffMinutes = (now.getTime() - lastUpdate.getTime()) / (1000 * 60);
          status = diffMinutes < 5 ? 'online' : 'offline';
        }

        return {
          ...client,
          status,
          position: position ? {
            latitude: position.latitude,
            longitude: position.longitude,
            timestamp: position.timestamp
          } : null
        };
      });

      setClients(clientsWithPositions);
    } catch (error) {
      console.error('Ошибка загрузки осужденных:', error);
      setClients([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    setIsClient(true);
    loadClients();

    // Обновляем данные каждые 10 секунд для отображения актуального статуса
    const interval = setInterval(() => {
      loadClients();
    }, 10000);

    return () => clearInterval(interval);
  }, [loadClients]);

  const getStatusColor = (status: string) => {
    return status === 'online' ? '#10b981' : '#ef4444';
  };

  const getStatusText = (status: string) => {
    return status === 'online' ? '🟢 Онлайн' : '🔴 Оффлайн';
  };

  const calculateAge = (birthDate?: string): number | null => {
    if (!birthDate) return null;
    const birth = new Date(birthDate);
    const today = new Date();
    let age = today.getFullYear() - birth.getFullYear();
    const monthDiff = today.getMonth() - birth.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
      age--;
    }
    return age;
  };

  const formatDate = (dateString?: string): string => {
    if (!dateString) return 'Не указано';
    const date = new Date(dateString);
    return date.toLocaleDateString('ru-RU');
  };

  if (!isClient) {
    return (
      <div className="map-loading">
        <div className="spinner"></div>
        <div>Инициализация карты...</div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="map-loading">
        <div className="spinner"></div>
        <div>Загрузка данных...</div>
      </div>
    );
  }

  const clientsWithValidPositions = clients.filter(client =>
    client.position &&
    !isNaN(client.position.latitude) &&
    !isNaN(client.position.longitude)
  );

  return (
    <div className="real-map-page">
      <div className="map-container-wrapper" style={{ position: 'relative', height: '100%', width: '100%' }}>
        <MapContainer
          bounds={KYRGYZSTAN_BOUNDS}
          boundsOptions={{ padding: [50, 50] }}
          maxBounds={KYRGYZSTAN_BOUNDS}
          minZoom={7}
          maxZoom={18}
          scrollWheelZoom={true}
          style={{
            height: '100%',
            width: '100%',
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0
          }}
          className="real-map"
        >
          <TileLayer
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          />

          {clientsWithValidPositions.map(client => {
            const photoUrl = client.photoKey
              ? `http://localhost:8083/api/faces/photos/${client.photoKey}`
              : `https://ui-avatars.com/api/?name=${encodeURIComponent(client.fio)}&background=3b82f6&color=fff&size=80`;

            const age = calculateAge(client.birthDate);

            return (
              <Marker
                key={client.id}
                position={[client.position!.latitude, client.position!.longitude]}
                icon={createPhotoIcon(client)}
              >
                <Popup maxWidth={400} className="client-popup">
                  <div className="popup-header">
                    <div className="popup-photo">
                      <img
                        src={photoUrl}
                        alt={client.fio}
                        onError={(e) => {
                          (e.target as HTMLImageElement).src = `https://ui-avatars.com/api/?name=${encodeURIComponent(client.fio)}&background=3b82f6&color=fff&size=120`;
                        }}
                      />
                    </div>
                    <div className="popup-title">
                      <h3>{client.fio}</h3>
                      <span
                        className="status-badge"
                        style={{ backgroundColor: getStatusColor(client.status) }}
                      >
                        {getStatusText(client.status)}
                      </span>
                    </div>
                  </div>

                  <div className="popup-content">
                    <div className="info-section">
                      <h4>📋 Основная информация</h4>
                      <div className="info-row">
                        <span className="label">Возраст:</span>
                        <span className="value">{age !== null ? `${age} лет` : 'Не указано'}</span>
                      </div>
                      <div className="info-row">
                        <span className="label">Пол:</span>
                        <span className="value">{client.sex || 'Не указано'}</span>
                      </div>
                      <div className="info-row">
                        <span className="label">ИНН:</span>
                        <span className="value">{client.inn || 'Не указано'}</span>
                      </div>
                      <div className="info-row">
                        <span className="label">Паспорт:</span>
                        <span className="value">{client.passportNumber || 'Не указано'}</span>
                      </div>
                    </div>

                    <div className="info-section">
                      <h4>📍 Адреса</h4>
                      <div className="info-row">
                        <span className="label">Регистрация:</span>
                        <span className="value">{client.registrationAddress || 'Не указано'}</span>
                      </div>
                      <div className="info-row">
                        <span className="label">Фактический:</span>
                        <span className="value">{client.actualAddress || 'Не указано'}</span>
                      </div>
                    </div>

                    <div className="info-section">
                      <h4>📞 Контакты</h4>
                      <div className="info-row">
                        <span className="label">Телефон:</span>
                        <span className="value">{client.phoneNumber || 'Не указано'}</span>
                      </div>
                      <div className="info-row">
                        <span className="label">Экстренный контакт:</span>
                        <span className="value">{client.emergencyContact || 'Не указано'}</span>
                      </div>
                    </div>

                    <div className="info-section">
                      <h4>⚖️ Надзор</h4>
                      <div className="info-row">
                        <span className="label">Тип:</span>
                        <span className="value">{client.supervisionType || 'Не указано'}</span>
                      </div>
                      <div className="info-row">
                        <span className="label">Начало:</span>
                        <span className="value">{formatDate(client.supervisionStartDate)}</span>
                      </div>
                      <div className="info-row">
                        <span className="label">Окончание:</span>
                        <span className="value">{formatDate(client.supervisionEndDate)}</span>
                      </div>
                      <div className="info-row">
                        <span className="label">Район:</span>
                        <span className="value">{client.districtName || 'Не указано'}</span>
                      </div>
                    </div>

                    {client.position && (
                      <div className="info-section">
                        <h4>🗺️ Текущая позиция</h4>
                        <div className="info-row">
                          <span className="label">Координаты:</span>
                          <span className="value">
                            {client.position.latitude.toFixed(6)}, {client.position.longitude.toFixed(6)}
                          </span>
                        </div>
                        <div className="info-row">
                          <span className="label">Обновлено:</span>
                          <span className="value">
                            {new Date(client.position.timestamp).toLocaleString('ru-RU')}
                          </span>
                        </div>
                        <div className="info-row">
                          <span className="label">Локация:</span>
                          <span className="value">Бишкек</span>
                        </div>
                      </div>
                    )}
                  </div>
                </Popup>
              </Marker>
            );
          })}
        </MapContainer>
      </div>
    </div>
  );
};

export default RealMap;
