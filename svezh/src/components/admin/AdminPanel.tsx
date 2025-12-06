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
      console.log('Обновление сотрудника:', employeeId, updates);
      const response = await adminAPI.updateEmployee(employeeId, updates);
      console.log('Ответ сервера:', response.data);
      await loadEmployees();
      setEditingEmployee(null);
      alert('Сотрудник успешно обновлен!');
    } catch (error: any) {
      console.error('Ошибка при обновлении сотрудника:', error);
      const errorMessage = error.response?.data?.error || error.message || 'Неизвестная ошибка';
      alert(`Ошибка при обновлении сотрудника: ${errorMessage}`);
    }
  };

  const changePassword = async (employeeId: number, newPassword: string) => {
    if (!newPassword) {
      alert('Введите новый пароль');
      return;
    }
    if (newPassword.length < 4) {
      alert('Пароль должен быть не менее 4 символов');
      return;
    }
    try {
      console.log('Изменение пароля для сотрудника:', employeeId);
      const response = await adminAPI.changeEmployeePassword(employeeId, newPassword);
      console.log('Ответ сервера:', response.data);
      setChangingPassword(null);
      alert('Пароль успешно изменен!');
    } catch (error: any) {
      console.error('Ошибка при изменении пароля:', error);
      const errorMessage = error.response?.data?.error || error.message || 'Неизвестная ошибка';
      alert(`Ошибка при изменении пароля: ${errorMessage}`);
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

      {/* Модальное окно редактирования сотрудника */}
      {editingEmployee && (
        <div className="modal-overlay" onClick={() => setEditingEmployee(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>Редактировать сотрудника</h2>
            <div className="form-group">
              <label>ИНН (логин)</label>
              <input type="text" value={editingEmployee.inn} disabled style={{backgroundColor: '#f0f0f0'}} />
            </div>
            <div className="form-group">
              <label>Unique ID</label>
              <input type="text" value={editingEmployee.uniqueId} disabled style={{backgroundColor: '#f0f0f0'}} />
            </div>
            <div className="form-group">
              <label>Роль</label>
              <select
                value={editingEmployee.attributes?.role || 'inspector'}
                onChange={(e) => setEditingEmployee({
                  ...editingEmployee,
                  attributes: { ...editingEmployee.attributes, role: e.target.value }
                })}
              >
                <option value="deptAdmin">Администратор департамента</option>
                <option value="mruAdmin">Администратор МРУ</option>
                <option value="inspector">Инспектор</option>
              </select>
            </div>
            <div className="modal-actions">
              <button
                onClick={() => updateEmployee(editingEmployee.id, {
                  role: editingEmployee.attributes?.role
                })}
                style={{
                  padding: '10px 20px',
                  backgroundColor: '#3b82f6',
                  color: 'white',
                  border: 'none',
                  borderRadius: '4px',
                  cursor: 'pointer',
                  marginRight: '10px'
                }}
              >
                Сохранить
              </button>
              <button
                onClick={() => setEditingEmployee(null)}
                style={{
                  padding: '10px 20px',
                  backgroundColor: '#6b7280',
                  color: 'white',
                  border: 'none',
                  borderRadius: '4px',
                  cursor: 'pointer'
                }}
              >
                Отмена
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Модальное окно изменения пароля */}
      {changingPassword && (
        <div className="modal-overlay" onClick={() => setChangingPassword(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>Изменить пароль</h2>
            <div className="form-group">
              <label>Новый пароль</label>
              <input
                type="password"
                value={changingPassword.newPassword}
                onChange={(e) => setChangingPassword({
                  ...changingPassword,
                  newPassword: e.target.value
                })}
                onKeyPress={(e) => {
                  if (e.key === 'Enter') {
                    changePassword(changingPassword.employeeId, changingPassword.newPassword);
                  }
                }}
                placeholder="Введите новый пароль (минимум 4 символа)"
                autoFocus
              />
            </div>
            <div className="modal-actions">
              <button
                onClick={() => {
                  changePassword(changingPassword.employeeId, changingPassword.newPassword);
                }}
                style={{
                  padding: '10px 20px',
                  backgroundColor: '#f59e0b',
                  color: 'white',
                  border: 'none',
                  borderRadius: '4px',
                  cursor: 'pointer',
                  marginRight: '10px'
                }}
              >
                Изменить пароль
              </button>
              <button
                onClick={() => setChangingPassword(null)}
                style={{
                  padding: '10px 20px',
                  backgroundColor: '#6b7280',
                  color: 'white',
                  border: 'none',
                  borderRadius: '4px',
                  cursor: 'pointer'
                }}
              >
                Отмена
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminPanel;
