import React, { useState, useEffect, useRef } from 'react';
import { MapContainer, TileLayer, Polyline, Marker, Popup, useMap } from 'react-leaflet';
import L from 'leaflet';
import api from '../../services/api';
import './TrackPlayback.css';

interface Position {
  id: number;
  latitude: number;
  longitude: number;
  altitude?: number;
  speed?: number;
  accuracy?: number;
  timestamp: string;
}

interface Client {
  id: number;
  fio: string;
}

// Компонент для автоматического центрирования карты
const MapController: React.FC<{ positions: Position[], currentIndex: number }> = ({ positions, currentIndex }) => {
  const map = useMap();

  useEffect(() => {
    if (positions.length > 0 && currentIndex >= 0 && currentIndex < positions.length) {
      const pos = positions[currentIndex];
      map.setView([pos.latitude, pos.longitude], map.getZoom());
    }
  }, [currentIndex, positions, map]);

  return null;
};

const TrackPlayback: React.FC = () => {
  const [clients, setClients] = useState<Client[]>([]);
  const [selectedClientId, setSelectedClientId] = useState<number | null>(null);
  const [positions, setPositions] = useState<Position[]>([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);
  const [playbackSpeed, setPlaybackSpeed] = useState(1);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const intervalRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    loadClients();
    // Устанавливаем дефолтные даты (последние 24 часа)
    const now = new Date();
    const yesterday = new Date(now.getTime() - 24 * 60 * 60 * 1000);
    setEndDate(formatDateTimeLocal(now));
    setStartDate(formatDateTimeLocal(yesterday));
  }, []);

  useEffect(() => {
    if (isPlaying && positions.length > 0) {
      intervalRef.current = setInterval(() => {
        setCurrentIndex(prev => {
          if (prev >= positions.length - 1) {
            setIsPlaying(false);
            return prev;
          }
          return prev + 1;
        });
      }, 1000 / playbackSpeed);
    } else {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
    }

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    };
  }, [isPlaying, playbackSpeed, positions.length]);

  const formatDateTimeLocal = (date: Date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
  };

  const loadClients = async () => {
    try {
      const response = await api.get('/admin/clients');
      setClients(response.data);
    } catch (error) {
      console.error('Error loading clients:', error);
    }
  };

  const loadTrack = async () => {
    if (!selectedClientId || !startDate || !endDate) {
      alert('Пожалуйста, выберите осужденного и временной диапазон');
      return;
    }

    try {
      const response = await api.get(`/positions/track/${selectedClientId}`, {
        params: {
          startTime: startDate,
          endTime: endDate
        }
      });

      setPositions(response.data);
      setCurrentIndex(0);
      setIsPlaying(false);

      if (response.data.length === 0) {
        alert('Нет данных за выбранный период');
      }
    } catch (error: any) {
      console.error('Error loading track:', error);
      alert(`Ошибка при загрузке трека: ${error.response?.data?.message || error.message}`);
    }
  };

  const handlePlayPause = () => {
    setIsPlaying(!isPlaying);
  };

  const handleReset = () => {
    setCurrentIndex(0);
    setIsPlaying(false);
  };

  const handleSliderChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newIndex = parseInt(e.target.value);
    setCurrentIndex(newIndex);
    setIsPlaying(false);
  };

  const formatTimestamp = (timestamp: string) => {
    const date = new Date(timestamp);
    return date.toLocaleString('ru-RU');
  };

  const currentPosition = positions[currentIndex];
  const trackLine = positions.slice(0, currentIndex + 1).map(p => [p.latitude, p.longitude] as [number, number]);
  const mapCenter: [number, number] = currentPosition
    ? [currentPosition.latitude, currentPosition.longitude]
    : [43.2566, 76.9286];

  // Создаем кастомную иконку для текущей позиции
  const currentIcon = new L.Icon({
    iconUrl: 'data:image/svg+xml;base64,' + btoa(`
      <svg width="32" height="32" viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
        <circle cx="16" cy="16" r="12" fill="#3b82f6" stroke="white" stroke-width="3"/>
        <circle cx="16" cy="16" r="5" fill="white"/>
      </svg>
    `),
    iconSize: [32, 32],
    iconAnchor: [16, 16],
  });

  return (
    <div className="track-playback">
      <div className="playback-header">
        <h1>История передвижений</h1>
        <p className="subtitle">Воспроизведение трека перемещения осужденного</p>
      </div>

      <div className="playback-content">
        <div className="controls-panel">
          <div className="panel-card">
            <h2>Параметры</h2>

            <div className="form-group">
              <label>Осужденный</label>
              <select
                value={selectedClientId || ''}
                onChange={(e) => setSelectedClientId(Number(e.target.value) || null)}
                className="styled-select"
              >
                <option value="">Выберите осужденного</option>
                {clients.map(client => (
                  <option key={client.id} value={client.id}>
                    {client.fio}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label>Начало периода</label>
              <input
                type="datetime-local"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="styled-input"
              />
            </div>

            <div className="form-group">
              <label>Конец периода</label>
              <input
                type="datetime-local"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="styled-input"
              />
            </div>

            <button onClick={loadTrack} className="btn-load">
              Загрузить трек
            </button>
          </div>

          {positions.length > 0 && (
            <>
              <div className="panel-card">
                <h2>Управление воспроизведением</h2>

                <div className="playback-controls">
                  <button onClick={handleReset} className="btn-control">
                    <svg width="20" height="20" fill="currentColor" viewBox="0 0 16 16">
                      <path d="M8 3.5a.5.5 0 0 0-1 0V9a.5.5 0 0 0 .252.434l3.5 2a.5.5 0 0 0 .496-.868L8 8.71V3.5z"/>
                      <path d="M8 16A8 8 0 1 0 8 0a8 8 0 0 0 0 16zm7-8A7 7 0 1 1 1 8a7 7 0 0 1 14 0z"/>
                    </svg>
                    Сброс
                  </button>

                  <button onClick={handlePlayPause} className="btn-play">
                    {isPlaying ? (
                      <svg width="20" height="20" fill="currentColor" viewBox="0 0 16 16">
                        <path d="M5.5 3.5A1.5 1.5 0 0 1 7 5v6a1.5 1.5 0 0 1-3 0V5a1.5 1.5 0 0 1 1.5-1.5zm5 0A1.5 1.5 0 0 1 12 5v6a1.5 1.5 0 0 1-3 0V5a1.5 1.5 0 0 1 1.5-1.5z"/>
                      </svg>
                    ) : (
                      <svg width="20" height="20" fill="currentColor" viewBox="0 0 16 16">
                        <path d="M8 15A7 7 0 1 1 8 1a7 7 0 0 1 0 14zm0 1A8 8 0 1 0 8 0a8 8 0 0 0 0 16z"/>
                        <path d="M6.271 5.055a.5.5 0 0 1 .52.038l3.5 2.5a.5.5 0 0 1 0 .814l-3.5 2.5A.5.5 0 0 1 6 10.5v-5a.5.5 0 0 1 .271-.445z"/>
                      </svg>
                    )}
                    {isPlaying ? 'Пауза' : 'Воспроизвести'}
                  </button>
                </div>

                <div className="speed-control">
                  <label>Скорость: {playbackSpeed}x</label>
                  <div className="speed-buttons">
                    {[0.5, 1, 2, 5].map(speed => (
                      <button
                        key={speed}
                        onClick={() => setPlaybackSpeed(speed)}
                        className={`btn-speed ${playbackSpeed === speed ? 'active' : ''}`}
                      >
                        {speed}x
                      </button>
                    ))}
                  </div>
                </div>

                <div className="timeline">
                  <input
                    type="range"
                    min="0"
                    max={positions.length - 1}
                    value={currentIndex}
                    onChange={handleSliderChange}
                    className="timeline-slider"
                  />
                  <div className="timeline-info">
                    <span>{currentIndex + 1} / {positions.length}</span>
                  </div>
                </div>
              </div>

              {currentPosition && (
                <div className="panel-card">
                  <h2>Текущая позиция</h2>
                  <div className="position-details">
                    <div className="detail-item">
                      <span className="label">Время:</span>
                      <span className="value">{formatTimestamp(currentPosition.timestamp)}</span>
                    </div>
                    <div className="detail-item">
                      <span className="label">Координаты:</span>
                      <span className="value">
                        {currentPosition.latitude.toFixed(6)}, {currentPosition.longitude.toFixed(6)}
                      </span>
                    </div>
                    {currentPosition.speed !== null && currentPosition.speed !== undefined && (
                      <div className="detail-item">
                        <span className="label">Скорость:</span>
                        <span className="value">{currentPosition.speed.toFixed(1)} км/ч</span>
                      </div>
                    )}
                    {currentPosition.accuracy && (
                      <div className="detail-item">
                        <span className="label">Точность:</span>
                        <span className="value">{currentPosition.accuracy.toFixed(0)} м</span>
                      </div>
                    )}
                  </div>
                </div>
              )}
            </>
          )}
        </div>

        <div className="map-container">
          <MapContainer
            center={mapCenter}
            zoom={15}
            style={{ height: '100%', width: '100%' }}
          >
            <TileLayer
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            />

            {positions.length > 0 && (
              <>
                <MapController positions={positions} currentIndex={currentIndex} />

                {/* Полная линия трека (серая) */}
                <Polyline
                  positions={positions.map(p => [p.latitude, p.longitude])}
                  pathOptions={{ color: '#d1d5db', weight: 3, opacity: 0.5 }}
                />

                {/* Пройденная часть (синяя) */}
                <Polyline
                  positions={trackLine}
                  pathOptions={{ color: '#3b82f6', weight: 4 }}
                />

                {/* Текущая позиция */}
                {currentPosition && (
                  <Marker
                    position={[currentPosition.latitude, currentPosition.longitude]}
                    icon={currentIcon}
                  >
                    <Popup>
                      <div className="popup-content">
                        <strong>{formatTimestamp(currentPosition.timestamp)}</strong>
                        <p>Координаты: {currentPosition.latitude.toFixed(6)}, {currentPosition.longitude.toFixed(6)}</p>
                        {currentPosition.speed && <p>Скорость: {currentPosition.speed.toFixed(1)} км/ч</p>}
                      </div>
                    </Popup>
                  </Marker>
                )}

                {/* Начальная точка */}
                <Marker
                  position={[positions[0].latitude, positions[0].longitude]}
                >
                  <Popup>
                    <strong>Начало маршрута</strong>
                    <p>{formatTimestamp(positions[0].timestamp)}</p>
                  </Popup>
                </Marker>

                {/* Конечная точка */}
                <Marker
                  position={[positions[positions.length - 1].latitude, positions[positions.length - 1].longitude]}
                >
                  <Popup>
                    <strong>Конец маршрута</strong>
                    <p>{formatTimestamp(positions[positions.length - 1].timestamp)}</p>
                  </Popup>
                </Marker>
              </>
            )}
          </MapContainer>
        </div>
      </div>
    </div>
  );
};

export default TrackPlayback;
