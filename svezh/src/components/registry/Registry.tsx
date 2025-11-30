import React, { useState, useEffect } from 'react';
import { registryAPI } from '../../services/api';
import { Client } from '../../types';
import ClientForm from './ClientForm';
import { useAuth } from '../../hooks/useAuth';

const Registry: React.FC = () => {
  const [clients, setClients] = useState<Client[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const { user } = useAuth();

  // Проверка прав доступа: только deptAdmin может редактировать
  const canEdit = user?.attributes?.role === 'deptAdmin';

  useEffect(() => {
    loadClients();
  }, []);

  const loadClients = async () => {
    try {
      const response = await registryAPI.getClients();
      setClients(response.data);
    } catch (error) {
      console.error('Ошибка загрузки реестра:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div>Загрузка реестра...</div>;

 return (
  <div className="registry-page">
    <div className="page-header">
      <h1>Реестр клиентов</h1>
      {canEdit ? (
        <button
          onClick={() => setShowForm(true)}
          className="add-client-btn"
        >
          Добавить клиента
        </button>
      ) : (
        <div style={{
          color: '#94a3b8',
          fontSize: '14px',
          fontStyle: 'italic'
        }}>
          Только Администратор департамента может добавлять клиентов
        </div>
      )}
    </div>

      {showForm && canEdit && (
        <ClientForm
          onClose={() => setShowForm(false)}
          onSuccess={() => {
            setShowForm(false);
            loadClients();
          }}
        />
      )}

      <div className="clients-grid">
        {clients.map(client => (
          <div key={client.id} className="client-card">
            <h3>{client.fio}</h3>
            <p>ИНН: {client.inn || 'Не указан'}</p>
            <p>Тип надзора: {client.obsType}</p>
            <p>Подразделение: {client.unit}</p>
            {client.photoKey && (
              <img 
                src={`/api/faces/${client.photoKey}`} 
                alt={client.fio}
                className="client-photo"
              />
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

export default Registry;