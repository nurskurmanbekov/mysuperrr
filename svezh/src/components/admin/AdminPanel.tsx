import React, { useState, useEffect } from 'react';
import axios from 'axios';

const API_URL = 'http://localhost:8083/api';

interface District {
  id: number;
  name: string;
  code: string;
}

interface Mru {
  id: number;
  name: string;
  code: string;
}

interface Employee {
  id: number;
  inn: string;
  uniqueId: string;
  userType: string;
  mruId?: string;
  attributes?: any;
}

interface Client {
  id: number;
  fio: string;
  inn?: string;
  district?: District;
}

export const AdminPanel: React.FC = () => {
  const [tab, setTab] = useState<'employees' | 'clients'>('employees');
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [clients, setClients] = useState<Client[]>([]);
  const [districts, setDistricts] = useState<District[]>([]);
  const [mrus, setMrus] = useState<Mru[]>([]);

  const [newEmployee, setNewEmployee] = useState({
    inn: '',
    password: '',
    uniqueId: '',
    role: 'inspector',
    districtId: ''
  });

  useEffect(() => {
    loadDistricts();
    loadMrus();
    loadEmployees();
    loadClients();
  }, []);

  const loadDistricts = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await axios.get(`${API_URL}/admin/districts`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setDistricts(response.data);
    } catch (error) {
      console.error('Error loading districts:', error);
    }
  };

  const loadMrus = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await axios.get(`${API_URL}/admin/mrus`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setMrus(response.data);
    } catch (error) {
      console.error('Error loading MRUs:', error);
    }
  };

  const loadEmployees = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await axios.get(`${API_URL}/admin/employees`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setEmployees(response.data);
    } catch (error) {
      console.error('Error loading employees:', error);
    }
  };

  const loadClients = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await axios.get(`${API_URL}/admin/clients`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setClients(response.data);
    } catch (error) {
      console.error('Error loading clients:', error);
    }
  };

  const createEmployee = async () => {
    try {
      const token = localStorage.getItem('token');
      await axios.post(
        `${API_URL}/admin/employees`,
        newEmployee,
        { headers: { Authorization: `Bearer ${token}` } }
      );

      alert('Сотрудник создан!');
      setNewEmployee({
        inn: '',
        password: '',
        uniqueId: '',
        role: 'inspector',
        districtId: ''
      });
      loadEmployees();
    } catch (error: any) {
      console.error('Error creating employee:', error);
      alert(`Ошибка: ${error.response?.data?.error || error.message}`);
    }
  };

  const deleteEmployee = async (employeeId: number) => {
    if (window.confirm('Удалить сотрудника?')) {
      try {
        const token = localStorage.getItem('token');
        await axios.delete(`${API_URL}/admin/employees/${employeeId}`, {
          headers: { Authorization: `Bearer ${token}` }
        });

        alert('Сотрудник удален!');
        loadEmployees();
      } catch (error) {
        console.error('Error deleting employee:', error);
        alert('Ошибка при удалении');
      }
    }
  };

  const transferClient = async (clientId: number, newDistrictId: string) => {
    if (window.confirm('Перевести осужденного?')) {
      try {
        const token = localStorage.getItem('token');
        await axios.put(
          `${API_URL}/admin/clients/${clientId}/transfer`,
          { districtId: parseInt(newDistrictId) },
          { headers: { Authorization: `Bearer ${token}` } }
        );

        alert('Осужденный переведен!');
        loadClients();
      } catch (error) {
        console.error('Error transferring client:', error);
        alert('Ошибка при переводе');
      }
    }
  };

  return (
    <div style={{ padding: '20px' }}>
      <h1>Админ панель</h1>

      <div style={{ marginBottom: '20px' }}>
        <button onClick={() => setTab('employees')} style={{ marginRight: '10px' }}>
          Сотрудники
        </button>
        <button onClick={() => setTab('clients')}>
          Осужденные
        </button>
      </div>

      {tab === 'employees' && (
        <div>
          <h2>Управление сотрудниками</h2>

          <div style={{ marginBottom: '20px', padding: '15px', border: '1px solid #ccc' }}>
            <h3>Добавить нового сотрудника</h3>
            <div style={{ display: 'grid', gap: '10px' }}>
              <input
                type="text"
                placeholder="ИНН"
                value={newEmployee.inn}
                onChange={(e) => setNewEmployee({ ...newEmployee, inn: e.target.value })}
              />
              <input
                type="password"
                placeholder="Пароль"
                value={newEmployee.password}
                onChange={(e) => setNewEmployee({ ...newEmployee, password: e.target.value })}
              />
              <input
                type="text"
                placeholder="Unique ID"
                value={newEmployee.uniqueId}
                onChange={(e) => setNewEmployee({ ...newEmployee, uniqueId: e.target.value })}
              />
              <select
                value={newEmployee.role}
                onChange={(e) => setNewEmployee({ ...newEmployee, role: e.target.value })}
              >
                <option value="inspector">Инспектор</option>
                <option value="mruAdmin">МРУ Админ</option>
                <option value="deptAdmin">Департамент Админ</option>
              </select>
              <select
                value={newEmployee.districtId}
                onChange={(e) => setNewEmployee({ ...newEmployee, districtId: e.target.value })}
              >
                <option value="">Выберите район</option>
                {districts.map(d => (
                  <option key={d.id} value={d.id}>{d.name}</option>
                ))}
              </select>
              <button onClick={createEmployee}>Создать</button>
            </div>
          </div>

          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid #ccc' }}>
                <th style={{ padding: '10px', textAlign: 'left' }}>ИНН</th>
                <th style={{ padding: '10px', textAlign: 'left' }}>Unique ID</th>
                <th style={{ padding: '10px', textAlign: 'left' }}>Роль</th>
                <th style={{ padding: '10px', textAlign: 'left' }}>Действия</th>
              </tr>
            </thead>
            <tbody>
              {employees.map(emp => (
                <tr key={emp.id} style={{ borderBottom: '1px solid #eee' }}>
                  <td style={{ padding: '10px' }}>{emp.inn}</td>
                  <td style={{ padding: '10px' }}>{emp.uniqueId}</td>
                  <td style={{ padding: '10px' }}>{emp.attributes?.role || 'N/A'}</td>
                  <td style={{ padding: '10px' }}>
                    <button onClick={() => deleteEmployee(emp.id)}>Удалить</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {tab === 'clients' && (
        <div>
          <h2>Управление осужденными</h2>

          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid #ccc' }}>
                <th style={{ padding: '10px', textAlign: 'left' }}>ФИО</th>
                <th style={{ padding: '10px', textAlign: 'left' }}>ИНН</th>
                <th style={{ padding: '10px', textAlign: 'left' }}>Район</th>
                <th style={{ padding: '10px', textAlign: 'left' }}>Действия</th>
              </tr>
            </thead>
            <tbody>
              {clients.map(client => (
                <tr key={client.id} style={{ borderBottom: '1px solid #eee' }}>
                  <td style={{ padding: '10px' }}>{client.fio}</td>
                  <td style={{ padding: '10px' }}>{client.inn || 'N/A'}</td>
                  <td style={{ padding: '10px' }}>{client.district?.name || 'Не назначен'}</td>
                  <td style={{ padding: '10px' }}>
                    <select
                      onChange={(e) => transferClient(client.id, e.target.value)}
                      defaultValue=""
                    >
                      <option value="">Перевести в...</option>
                      {districts.map(d => (
                        <option key={d.id} value={d.id}>{d.name}</option>
                      ))}
                    </select>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};
