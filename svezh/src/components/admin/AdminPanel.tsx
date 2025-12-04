import React, { useState, useEffect } from 'react';
import api, { adminAPI } from '../../services/api';
import { useAuth } from '../../hooks/useAuth';
import './AdminPanel.css';

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
  const { user } = useAuth();
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
    mruId: '',
    districtId: ''
  });

  const [editingEmployee, setEditingEmployee] = useState<Employee | null>(null);
  const [changingPassword, setChangingPassword] = useState<{employeeId: number, newPassword: string} | null>(null);

  // Проверка роли: только deptAdmin может редактировать
  const isDeptAdmin = user?.attributes?.role === 'deptAdmin';

  useEffect(() => {
    loadDistricts();
    loadMrus();
    if (tab === 'employees') {
      loadEmployees();
    } else {
      loadClients();
    }
  }, [tab]);

  const loadDistricts = async () => {
    try {
      const response = await api.get('/admin/districts');
      setDistricts(response.data);
    } catch (error) {
      console.error('Error loading districts:', error);
    }
  };

  const loadMrus = async () => {
    try {
      const response = await api.get('/admin/mrus');
      setMrus(response.data);
    } catch (error) {
      console.error('Error loading MRUs:', error);
    }
  };

  const loadEmployees = async () => {
    try {
      const response = await api.get('/admin/employees');
      setEmployees(response.data);
    } catch (error) {
      console.error('Error loading employees:', error);
    }
  };

  const loadClients = async () => {
    try {
      const response = await api.get('/admin/clients');
      setClients(response.data);
    } catch (error) {
      console.error('Error loading clients:', error);
    }
  };

  const createEmployee = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const payload: any = {
        inn: newEmployee.inn,
        password: newEmployee.password,
        uniqueId: newEmployee.uniqueId,
        role: newEmployee.role,
        userType: 'employee'
      };

      // Логика в зависимости от роли
      if (newEmployee.role === 'mruAdmin' && newEmployee.mruId) {
        payload.mruId = newEmployee.mruId;
      } else if (newEmployee.role === 'inspector' && newEmployee.districtId) {
        payload.districtIds = [parseInt(newEmployee.districtId)];
      }

      await api.post('/admin/employees', payload);
      setNewEmployee({
        inn: '',
        password: '',
        uniqueId: '',
        role: 'inspector',
        mruId: '',
        districtId: ''
      });
      loadEmployees();
      alert('Сотрудник успешно создан!');
    } catch (error: any) {
      console.error('Error creating employee:', error);
      alert(`Ошибка: ${error.response?.data?.message || error.message}`);
    }
  };

  const transferClient = async (clientId: number, newDistrictId: string) => {
    if (!newDistrictId) return;
    try {
      await api.put(`/admin/clients/${clientId}/transfer`, {
        districtId: parseInt(newDistrictId)
      });
      loadClients();
      alert('Осужденный успешно переведен!');
    } catch (error) {
      console.error('Error transferring client:', error);
      alert('Ошибка при переводе осужденного');
    }
  };

  const deleteEmployee = async (employeeId: number) => {
    if (!window.confirm('Вы уверены, что хотите удалить этого сотрудника?')) {
      return;
    }
    try {
      await adminAPI.deleteEmployee(employeeId);
      loadEmployees();
      alert('Сотрудник успешно удален!');
    } catch (error) {
      console.error('Error deleting employee:', error);
      alert('Ошибка при удалении сотрудника');
    }
  };

  const updateEmployee = async (employeeId: number, updates: any) => {
    try {
      await adminAPI.updateEmployee(employeeId, updates);
      loadEmployees();
      setEditingEmployee(null);
      alert('Сотрудник успешно обновлен!');
    } catch (error) {
      console.error('Error updating employee:', error);
      alert('Ошибка при обновлении сотрудника');
    }
  };

  const changePassword = async (employeeId: number, newPassword: string) => {
    if (!newPassword) {
      alert('Введите новый пароль');
      return;
    }
    try {
      await adminAPI.changeEmployeePassword(employeeId, newPassword);
      setChangingPassword(null);
      alert('Пароль успешно изменен!');
    } catch (error) {
      console.error('Error changing password:', error);
      alert('Ошибка при изменении пароля');
    }
  };

  const getRoleName = (role: string) => {
    switch (role) {
      case 'deptAdmin': return 'Администратор департамента';
      case 'mruAdmin': return 'Администратор МРУ';
      case 'inspector': return 'Инспектор';
      default: return role;
    }
  };

  return (
    <div className="admin-panel">
      <div className="admin-header">
        <h1>Администрирование</h1>
        <div className="tab-buttons">
          <button
            className={tab === 'employees' ? 'active' : ''}
            onClick={() => setTab('employees')}
          >
            Сотрудники
          </button>
          <button
            className={tab === 'clients' ? 'active' : ''}
            onClick={() => setTab('clients')}
          >
            Осужденные
          </button>
        </div>
      </div>

      {tab === 'employees' ? (
        <div className="employees-section">
          {isDeptAdmin && (
            <div className="create-employee-card">
              <h2>Добавить сотрудника</h2>
              <form onSubmit={createEmployee} className="employee-form">
              <div className="form-row">
                <div className="form-group">
                  <label>ИНН (логин)</label>
                  <input
                    type="text"
                    value={newEmployee.inn}
                    onChange={(e) => setNewEmployee({ ...newEmployee, inn: e.target.value })}
                    placeholder="Введите ИНН"
                    required
                  />
                </div>
                <div className="form-group">
                  <label>Пароль</label>
                  <input
                    type="password"
                    value={newEmployee.password}
                    onChange={(e) => setNewEmployee({ ...newEmployee, password: e.target.value })}
                    placeholder="Введите пароль"
                    required
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Уникальный ID</label>
                  <input
                    type="text"
                    value={newEmployee.uniqueId}
                    onChange={(e) => setNewEmployee({ ...newEmployee, uniqueId: e.target.value })}
                    placeholder="Уникальный идентификатор"
                    required
                  />
                </div>
                <div className="form-group">
                  <label>Роль</label>
                  <select
                    value={newEmployee.role}
                    onChange={(e) => setNewEmployee({
                      ...newEmployee,
                      role: e.target.value,
                      mruId: '',
                      districtId: ''
                    })}
                  >
                    <option value="deptAdmin">Администратор департамента</option>
                    <option value="mruAdmin">Администратор МРУ</option>
                    <option value="inspector">Инспектор</option>
                  </select>
                </div>
              </div>

              {newEmployee.role === 'mruAdmin' && (
                <div className="form-group">
                  <label>МРУ</label>
                  <select
                    value={newEmployee.mruId}
                    onChange={(e) => setNewEmployee({ ...newEmployee, mruId: e.target.value })}
                    required
                  >
                    <option value="">Выберите МРУ</option>
                    {mrus.map(mru => (
                      <option key={mru.id} value={mru.id}>{mru.name}</option>
                    ))}
                  </select>
                </div>
              )}

              {newEmployee.role === 'inspector' && (
                <div className="form-group">
                  <label>Район</label>
                  <select
                    value={newEmployee.districtId}
                    onChange={(e) => setNewEmployee({ ...newEmployee, districtId: e.target.value })}
                    required
                  >
                    <option value="">Выберите район</option>
                    {districts.map(district => (
                      <option key={district.id} value={district.id}>{district.name}</option>
                    ))}
                  </select>
                </div>
              )}

              <button type="submit" className="btn-primary">
                Создать сотрудника
              </button>
            </form>
          </div>
          )}

          <div className="employees-list-card">
            <h2>Список сотрудников</h2>
            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>ИНН</th>
                    <th>Unique ID</th>
                    <th>Роль</th>
                    <th>МРУ</th>
                    {isDeptAdmin && <th>Действия</th>}
                  </tr>
                </thead>
                <tbody>
                  {employees.map(emp => (
                    <tr key={emp.id}>
                      <td>{emp.inn}</td>
                      <td>{emp.uniqueId}</td>
                      <td>{getRoleName(emp.attributes?.role || 'inspector')}</td>
                      <td>{emp.mruId || '-'}</td>
                      {isDeptAdmin && (
                        <td>
                          <button
                            onClick={() => setEditingEmployee(emp)}
                            style={{
                              marginRight: '5px',
                              padding: '6px 12px',
                              backgroundColor: '#3b82f6',
                              color: 'white',
                              border: 'none',
                              borderRadius: '4px',
                              cursor: 'pointer',
                              fontSize: '12px'
                            }}
                          >
                            Редактировать
                          </button>
                          <button
                            onClick={() => setChangingPassword({employeeId: emp.id, newPassword: ''})}
                            style={{
                              marginRight: '5px',
                              padding: '6px 12px',
                              backgroundColor: '#f59e0b',
                              color: 'white',
                              border: 'none',
                              borderRadius: '4px',
                              cursor: 'pointer',
                              fontSize: '12px'
                            }}
                          >
                            Изменить пароль
                          </button>
                          <button
                            onClick={() => deleteEmployee(emp.id)}
                            style={{
                              padding: '6px 12px',
                              backgroundColor: '#ef4444',
                              color: 'white',
                              border: 'none',
                              borderRadius: '4px',
                              cursor: 'pointer',
                              fontSize: '12px'
                            }}
                          >
                            Удалить
                          </button>
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      ) : (
        <div className="clients-section">
          <div className="clients-list-card">
            <h2>Список осужденных</h2>
            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>ФИО</th>
                    <th>ИНН</th>
                    <th>Район</th>
                    <th>Действия</th>
                  </tr>
                </thead>
                <tbody>
                  {clients.map(client => (
                    <tr key={client.id}>
                      <td>{client.fio}</td>
                      <td>{client.inn || '-'}</td>
                      <td>{client.district?.name || '-'}</td>
                      <td>
                        <select
                          className="transfer-select"
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
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminPanel;
