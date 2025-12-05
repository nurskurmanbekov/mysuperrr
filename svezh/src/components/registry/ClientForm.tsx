import React, { useState, useEffect } from 'react';
import { registryAPI } from '../../services/api';
import { Client } from '../../types';

interface ClientFormProps {
  client?: Client | null;
  onClose: () => void;
  onSuccess: () => void;
}

const ClientForm: React.FC<ClientFormProps> = ({ client, onClose, onSuccess }) => {
  const isEditMode = !!client;

  const [formData, setFormData] = useState({
    // Основная информация
    lastName: '',
    firstName: '',
    middleName: '',
    sex: '',
    birthDate: '',
    age: '',
    inn: '',
    noInn: false,
    passport: '',
    // Адреса
    regAddress: '',
    factAddress: '',
    // Контакты
    contact1: '',
    contact2: '',
    // Надзор
    obsType: 'Электронный надзор',
    obsStart: '',
    obsEnd: '',
    unit: '',
    // Дополнительные поля надзора
    erpNumber: '',
    degree: '',
    udNumber: '',
    extraInfo: '',
    measures: '',
    // Доступ
    appPassword: ''
  });

  const [articles, setArticles] = useState<Array<{
    article: string;
    part: string;
    point: string;
  }>>([{ article: '', part: '', point: '' }]);
  const [photo, setPhoto] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>('');

  // Вычисляем возраст из даты рождения
  const calculateAge = (birthDate: string): number => {
    if (!birthDate) return 0;
    const today = new Date();
    const birth = new Date(birthDate);
    let age = today.getFullYear() - birth.getFullYear();
    const monthDiff = today.getMonth() - birth.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
      age--;
    }
    return age;
  };

  // Загружаем данные клиента при редактировании
  useEffect(() => {
    if (client) {
      // Разбиваем ФИО на части
      const nameParts = client.fio.split(' ');
      const lastName = nameParts[0] || '';
      const firstName = nameParts[1] || '';
      const middleName = nameParts[2] || '';

      setFormData({
        lastName,
        firstName,
        middleName,
        sex: client.sex || '',
        birthDate: client.birthDate || '',
        age: client.age?.toString() || '',
        inn: client.inn || '',
        noInn: !client.inn,
        passport: client.passport || '',
        regAddress: client.regAddress || '',
        factAddress: client.factAddress || '',
        contact1: client.contact1 || '',
        contact2: client.contact2 || '',
        obsType: client.obsType || 'Электронный надзор',
        obsStart: client.obsStart || '',
        obsEnd: client.obsEnd || '',
        unit: client.unit || '',
        erpNumber: client.erpNumber || '',
        degree: client.degree || '',
        udNumber: client.udNumber || '',
        extraInfo: client.extraInfo || '',
        measures: client.measures || '',
        appPassword: '' // Не показываем старый пароль
      });

      // Загружаем статьи осуждения
      if (client.articles && client.articles.length > 0) {
        setArticles(client.articles.map(a => ({
          article: a.article || '',
          part: a.part || '',
          point: a.point || ''
        })));
      } else {
        setArticles([{ article: '', part: '', point: '' }]);
      }
    }
  }, [client]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const formDataToSend = new FormData();

      // Вычисляем возраст если указана дата рождения
      const ageValue = formData.birthDate ? calculateAge(formData.birthDate) : null;

      // Добавляем данные формы, исключая пустые даты
      const requestData: any = {
        ...formData,
        age: ageValue, // Автоматически вычисленный возраст
        // Если noInn отмечен, отправляем пустую строку для inn
        inn: formData.noInn ? '' : formData.inn,
        noInn: formData.noInn || undefined,
        // Удаляем пустые даты (иначе backend не сможет их распарсить)
        birthDate: formData.birthDate || null,
        obsStart: formData.obsStart || null,
        obsEnd: formData.obsEnd || null,
        // Добавляем статьи осуждения (только непустые)
        articles: articles.filter(a => a.article || a.part || a.point)
      };

      // При редактировании не требуем пароль если он пустой
      if (isEditMode && !formData.appPassword) {
        delete requestData.appPassword;
      }

      // Удаляем null значения
      Object.keys(requestData).forEach(key => {
        if (requestData[key] === null || requestData[key] === '') {
          if (!['inn', 'noInn', 'birthDate', 'obsStart', 'obsEnd', 'age'].includes(key)) {
            delete requestData[key];
          }
        }
      });

      formDataToSend.append('request', new Blob([JSON.stringify(requestData)], {
        type: 'application/json'
      }));

      // Добавляем фото если есть
      if (photo) {
        formDataToSend.append('photo', photo);
      }

      if (isEditMode && client) {
        await registryAPI.updateClient(client.id, formDataToSend);
      } else {
        await registryAPI.createClient(formDataToSend);
      }

      onSuccess();
    } catch (error: any) {
      console.error(isEditMode ? 'Ошибка обновления клиента:' : 'Ошибка создания клиента:', error);
      const errorMessage = error.response?.data?.error || error.response?.data?.message ||
        (isEditMode ? 'Ошибка при обновлении клиента. Проверьте данные.' : 'Ошибка при создании клиента. Проверьте данные.');
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value, type } = e.target;
    const newValue = type === 'checkbox' ? (e.target as HTMLInputElement).checked : value;

    setFormData(prev => {
      const updated = {
        ...prev,
        [name]: newValue
      };

      // Автоматически обновляем возраст при изменении даты рождения
      if (name === 'birthDate' && value) {
        updated.age = calculateAge(value).toString();
      }

      return updated;
    });
  };

  return (
    <div className="modal-overlay">
      <div className="modal-content">
        <div className="modal-header">
          <h2>{isEditMode ? 'Редактировать клиента' : 'Добавить клиента'}</h2>
          <button onClick={onClose} className="close-button">×</button>
        </div>

        <form onSubmit={handleSubmit} className="client-form">
          {error && (
            <div className="error-message" style={{
              background: '#dc2626',
              color: 'white',
              padding: '15px',
              borderRadius: '8px',
              marginBottom: '20px',
              fontWeight: '600',
              border: '2px solid #ef4444'
            }}>
              ⚠️ {error}
            </div>
          )}

          {/* 1. ОСНОВНАЯ ИНФОРМАЦИЯ */}
          <div className="form-section">
            <h3>Основная информация</h3>

            <div className="form-row">
              <div className="form-group">
                <label>Фамилия *</label>
                <input
                  type="text"
                  name="lastName"
                  value={formData.lastName}
                  onChange={handleChange}
                  required
                />
              </div>

              <div className="form-group">
                <label>Имя *</label>
                <input
                  type="text"
                  name="firstName"
                  value={formData.firstName}
                  onChange={handleChange}
                  required
                />
              </div>

              <div className="form-group">
                <label>Отчество</label>
                <input
                  type="text"
                  name="middleName"
                  value={formData.middleName}
                  onChange={handleChange}
                />
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label>Пол</label>
                <select
                  name="sex"
                  value={formData.sex}
                  onChange={handleChange}
                >
                  <option value="">Выберите</option>
                  <option value="М">Мужской</option>
                  <option value="Ж">Женский</option>
                </select>
              </div>

              <div className="form-group">
                <label>Дата рождения</label>
                <input
                  type="date"
                  name="birthDate"
                  value={formData.birthDate}
                  onChange={handleChange}
                />
              </div>

              <div className="form-group">
                <label>Возраст</label>
                <input
                  type="number"
                  name="age"
                  value={formData.age}
                  readOnly
                  disabled
                  style={{ backgroundColor: '#f3f4f6', cursor: 'not-allowed' }}
                  title="Вычисляется автоматически из даты рождения"
                />
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label>ИНН</label>
                <input
                  type="text"
                  name="inn"
                  value={formData.inn}
                  onChange={handleChange}
                  disabled={formData.noInn}
                />
              </div>

              <div className="form-group checkbox-group">
                <label>
                  <input
                    type="checkbox"
                    name="noInn"
                    checked={formData.noInn}
                    onChange={handleChange}
                  />
                  Без ИНН
                </label>
              </div>

              <div className="form-group">
                <label>Паспорт</label>
                <input
                  type="text"
                  name="passport"
                  value={formData.passport}
                  onChange={handleChange}
                />
              </div>
            </div>
          </div>

          {/* 2. АДРЕСА */}
          <div className="form-section">
            <h3>Адреса</h3>

            <div className="form-group">
              <label>Адрес регистрации</label>
              <input
                type="text"
                name="regAddress"
                value={formData.regAddress}
                onChange={handleChange}
              />
            </div>

            <div className="form-group">
              <label>Фактический адрес</label>
              <input
                type="text"
                name="factAddress"
                value={formData.factAddress}
                onChange={handleChange}
              />
            </div>
          </div>

          {/* 3. КОНТАКТЫ */}
          <div className="form-section">
            <h3>Контакты</h3>

            <div className="form-row">
              <div className="form-group">
                <label>Телефон</label>
                <input
                  type="text"
                  name="contact1"
                  value={formData.contact1}
                  onChange={handleChange}
                  placeholder="+996 XXX XXX XXX"
                />
              </div>

              <div className="form-group">
                <label>Экстренный контакт</label>
                <input
                  type="text"
                  name="contact2"
                  value={formData.contact2}
                  onChange={handleChange}
                  placeholder="+996 XXX XXX XXX"
                />
              </div>
            </div>
          </div>

          {/* 4. НАДЗОР */}
          <div className="form-section">
            <h3>Надзор</h3>

            <div className="form-row">
              <div className="form-group">
                <label>Тип надзора *</label>
                <select
                  name="obsType"
                  value={formData.obsType}
                  onChange={handleChange}
                  required
                >
                  <option value="Электронный надзор">Электронный надзор</option>
                  <option value="Мобильное приложение">Мобильное приложение</option>
                  <option value="Иное">Иное</option>
                </select>
              </div>

              <div className="form-group">
                <label>Дата начала надзора</label>
                <input
                  type="date"
                  name="obsStart"
                  value={formData.obsStart}
                  onChange={handleChange}
                />
              </div>

              <div className="form-group">
                <label>Дата окончания надзора</label>
                <input
                  type="date"
                  name="obsEnd"
                  value={formData.obsEnd}
                  onChange={handleChange}
                />
              </div>
            </div>
          </div>

          {/* 5. СТАТЬЯ ОСУЖДЕНИЯ */}
          <div className="form-section">
            <h3>⚖️ Статья осуждения</h3>

            {articles.map((article, index) => (
              <div key={index} style={{
                marginBottom: '20px',
                padding: '15px',
                border: '1px solid #e5e7eb',
                borderRadius: '8px',
                backgroundColor: '#f9fafb'
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
                  <strong style={{ color: '#374151' }}>Статья {index + 1}</strong>
                  {articles.length > 1 && (
                    <button
                      type="button"
                      onClick={() => {
                        const newArticles = articles.filter((_, i) => i !== index);
                        setArticles(newArticles);
                      }}
                      style={{
                        background: '#ef4444',
                        color: 'white',
                        border: 'none',
                        padding: '5px 10px',
                        borderRadius: '4px',
                        cursor: 'pointer',
                        fontSize: '14px'
                      }}
                    >
                      Удалить
                    </button>
                  )}
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label>Статья</label>
                    <input
                      type="text"
                      value={article.article}
                      onChange={(e) => {
                        const newArticles = [...articles];
                        newArticles[index].article = e.target.value;
                        setArticles(newArticles);
                      }}
                      placeholder="Например: 158"
                    />
                  </div>

                  <div className="form-group">
                    <label>Часть</label>
                    <input
                      type="text"
                      value={article.part}
                      onChange={(e) => {
                        const newArticles = [...articles];
                        newArticles[index].part = e.target.value;
                        setArticles(newArticles);
                      }}
                      placeholder="Например: 3"
                    />
                  </div>

                  <div className="form-group">
                    <label>Пункт</label>
                    <input
                      type="text"
                      value={article.point}
                      onChange={(e) => {
                        const newArticles = [...articles];
                        newArticles[index].point = e.target.value;
                        setArticles(newArticles);
                      }}
                      placeholder="Например: а, б"
                    />
                  </div>
                </div>
              </div>
            ))}

            <button
              type="button"
              onClick={() => {
                setArticles([...articles, { article: '', part: '', point: '' }]);
              }}
              style={{
                background: '#3b82f6',
                color: 'white',
                border: 'none',
                padding: '10px 20px',
                borderRadius: '6px',
                cursor: 'pointer',
                fontSize: '14px',
                fontWeight: '500'
              }}
            >
              + Добавить статью
            </button>
          </div>

          {/* 6. ПОДРАЗДЕЛЕНИЕ */}
          <div className="form-section">
            <h3>Подразделение</h3>

            <div className="form-group">
              <label>Район (Подразделение)</label>
              <input
                type="text"
                name="unit"
                value={formData.unit}
                onChange={handleChange}
                placeholder="Например: Центральный район"
              />
            </div>
          </div>

          {/* 7. ДОСТУП */}
          <div className="form-section">
            <h3>Доступ</h3>

            <div className="form-group">
              <label>Пароль для приложения {isEditMode && '(оставьте пустым, чтобы не менять)'}{!isEditMode && '*'}</label>
              <input
                type="password"
                name="appPassword"
                value={formData.appPassword}
                onChange={handleChange}
                required={!isEditMode}
              />
            </div>
          </div>

          {/* 8. ЛИЦО */}
          <div className="form-section">
            <h3>Эталонное фото для Face ID</h3>

            <div className="form-group">
              <label>Эталонное фото {isEditMode && '(загрузите новое, чтобы заменить)'}</label>
              <input
                type="file"
                accept="image/*"
                onChange={(e) => setPhoto(e.target.files?.[0] || null)}
              />
              {!isEditMode && (
                <small style={{ display: 'block', marginTop: '5px', color: '#6b7280' }}>
                  Рекомендуется загрузить фото для корректной работы Face ID
                </small>
              )}
            </div>
          </div>

          <div className="form-actions">
            <button type="button" onClick={onClose} className="btn-secondary">
              Отмена
            </button>
            <button type="submit" disabled={loading} className="btn-primary">
              {loading
                ? (isEditMode ? 'Сохранение...' : 'Создание...')
                : (isEditMode ? 'Сохранить изменения' : 'Создать клиента')
              }
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ClientForm;
