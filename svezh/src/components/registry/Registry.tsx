import React, { useState, useEffect } from 'react';
import { registryAPI } from '../../services/api';
import { Client } from '../../types';
import ClientForm from './ClientForm';

const Registry: React.FC = () => {
  const [clients, setClients] = useState<Client[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingClient, setEditingClient] = useState<Client | null>(null);

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

  const handleEdit = (client: Client) => {
    setEditingClient(client);
    setShowForm(true);
  };

  const handleDelete = async (clientId: number) => {
    if (!window.confirm('Вы уверены, что хотите удалить этого клиента?')) {
      return;
    }
    try {
      await registryAPI.deleteClient(clientId);
      loadClients();
    } catch (error) {
      console.error('Ошибка удаления клиента:', error);
      alert('Не удалось удалить клиента');
    }
  };

  if (loading) return <div>Загрузка реестра...</div>;

 return (
  <div className="registry-page">
    <div className="page-header">
      <h1>Реестр клиентов</h1>
      <button
        onClick={() => {
          setEditingClient(null);
          setShowForm(true);
        }}
        className="add-client-btn"
      >
        Добавить клиента
      </button>
    </div>

      {showForm && (
        <ClientForm
          client={editingClient}
          onClose={() => {
            setShowForm(false);
            setEditingClient(null);
          }}
          onSuccess={() => {
            setShowForm(false);
            setEditingClient(null);
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
            <div className="client-actions">
              <button
                onClick={() => handleEdit(client)}
                className="edit-btn"
                style={{
                  marginRight: '10px',
                  padding: '8px 16px',
                  backgroundColor: '#3b82f6',
                  color: 'white',
                  border: 'none',
                  borderRadius: '6px',
                  cursor: 'pointer'
                }}
              >
                Редактировать
              </button>
              <button
                onClick={() => handleDelete(client.id)}
                className="delete-btn"
                style={{
                  padding: '8px 16px',
                  backgroundColor: '#ef4444',
                  color: 'white',
                  border: 'none',
                  borderRadius: '6px',
                  cursor: 'pointer'
                }}
              >
                Удалить
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default Registry;