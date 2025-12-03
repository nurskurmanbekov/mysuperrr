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
    inn: '',
    noInn: false,
    lastName: '',
    firstName: '',
    middleName: '',
    birthDate: '',
    sex: '',
    passport: '',
    regAddress: '',
    factAddress: '',
    contact1: '',
    contact2: '',
    erpNumber: '',
    obsStart: '',
    obsEnd: '',
    obsType: 'Электронный надзор',
    degree: '',
    udNumber: '',
    extraInfo: '',
    measures: '',
    appPassword: '',
    unit: '',
    districtId: ''
  });

  const [articles, setArticles] = useState<Array<{ article: string; part: string; point: string }>>([
    { article: '', part: '', point: '' }
  ]);
  const [photo, setPhoto] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>('');

  // Загружаем данные клиента при редактировании
  useEffect(() => {
    if (client) {
      // Разбиваем ФИО на части
      const nameParts = client.fio.split(' ');
      const lastName = nameParts[0] || '';
      const firstName = nameParts[1] || '';
      const middleName = nameParts[2] || '';

      setFormData({
        inn: client.inn || '',
        noInn: !client.inn,
        lastName,
        firstName,
        middleName,
        birthDate: client.birthDate || '',
        sex: client.sex || '',
        passport: client.passport || '',
        regAddress: client.regAddress || '',
        factAddress: client.factAddress || '',
        contact1: client.contact1 || '',
        contact2: client.contact2 || '',
        erpNumber: client.erpNumber || '',
        obsStart: client.obsStart || '',
        obsEnd: client.obsEnd || '',
        obsType: client.obsType || 'Электронный надзор',
        degree: client.degree || '',
        udNumber: client.udNumber || '',
        extraInfo: client.extraInfo || '',
        measures: client.measures || '',
        appPassword: '', // Не показываем старый пароль
        unit: client.unit || '',
        districtId: client.district?.id?.toString() || ''
      });

      // Загружаем статьи
      if (client.articles && client.articles.length > 0) {
        setArticles(client.articles.map(a => ({
          article: a.article || '',
          part: a.part || '',
          point: a.point || ''
        })));
      }
    }
  }, [client]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const formDataToSend = new FormData();

      // Добавляем данные формы, исключая пустые даты
      const requestData: any = {
        ...formData,
        // Если noInn отмечен, отправляем пустую строку для inn
        inn: formData.noInn ? '' : formData.inn,
        noInn: formData.noInn || undefined,
        // Удаляем пустые даты (иначе backend не сможет их распарсить)
        birthDate: formData.birthDate || null,
        obsStart: formData.obsStart || null,
        obsEnd: formData.obsEnd || null,
        // Конвертируем districtId в число
        districtId: formData.districtId ? parseInt(formData.districtId) : null,
        // Добавляем статьи осуждения
        articles: articles.filter(a => a.article || a.part || a.point) // Убираем пустые статьи
      };

      // При редактировании не требуем пароль если он пустой
      if (isEditMode && !formData.appPassword) {
        delete requestData.appPassword;
      }

      // Удаляем null значения
      Object.keys(requestData).forEach(key => {
        if (requestData[key] === null || requestData[key] === '') {
          if (!['inn', 'noInn', 'birthDate', 'obsStart', 'obsEnd'].includes(key)) {
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
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? (e.target as HTMLInputElement).checked : value
    }));
  };

  const calculateAge = (birthDate: string): string => {
    if (!birthDate) return '';
    const today = new Date();
    const birth = new Date(birthDate);
    let age = today.getFullYear() - birth.getFullYear();
    const monthDiff = today.getMonth() - birth.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
      age--;
    }
    return age.toString();
  };

  const handleArticleChange = (index: number, field: keyof typeof articles[0], value: string) => {
    const newArticles = [...articles];
    newArticles[index][field] = value;
    setArticles(newArticles);
  };

  const addArticle = () => {
    setArticles([...articles, { article: '', part: '', point: '' }]);
  };

  const removeArticle = (index: number) => {
    if (articles.length > 1) {
      setArticles(articles.filter((_, i) => i !== index));
    }
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
              <label>Дата рождения</label>
              <input
                type="date"
                name="birthDate"
                value={formData.birthDate}
                onChange={handleChange}
              />
            </div>
          </div>

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
              <label>Подразделение</label>
              <input
                type="text"
                name="unit"
                value={formData.unit}
                onChange={handleChange}
              />
            </div>
          </div>

          <h3 style={{ marginTop: '20px', marginBottom: '15px', color: '#374151' }}>Статья осуждения</h3>
          {articles.map((articleItem, index) => (
            <div key={index} style={{ marginBottom: '15px', padding: '15px', backgroundColor: '#f9fafb', borderRadius: '8px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
                <strong>Статья {index + 1}</strong>
                {articles.length > 1 && (
                  <button
                    type="button"
                    onClick={() => removeArticle(index)}
                    style={{
                      padding: '5px 10px',
                      backgroundColor: '#ef4444',
                      color: 'white',
                      border: 'none',
                      borderRadius: '4px',
                      cursor: 'pointer'
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
                    value={articleItem.article}
                    onChange={(e) => handleArticleChange(index, 'article', e.target.value)}
                    placeholder="Например: 228"
                  />
                </div>
                <div className="form-group">
                  <label>Часть</label>
                  <input
                    type="text"
                    value={articleItem.part}
                    onChange={(e) => handleArticleChange(index, 'part', e.target.value)}
                    placeholder="Например: 1"
                  />
                </div>
                <div className="form-group">
                  <label>Пункт</label>
                  <input
                    type="text"
                    value={articleItem.point}
                    onChange={(e) => handleArticleChange(index, 'point', e.target.value)}
                    placeholder="Например: а"
                  />
                </div>
              </div>
            </div>
          ))}
          <button
            type="button"
            onClick={addArticle}
            style={{
              padding: '10px 20px',
              backgroundColor: '#3b82f6',
              color: 'white',
              border: 'none',
              borderRadius: '6px',
              cursor: 'pointer',
              marginBottom: '20px',
              fontWeight: '600'
            }}
          >
            + Добавить статью
          </button>

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

          <div className="form-group">
            <label>Эталонное фото {isEditMode && '(загрузите новое, чтобы заменить)'}</label>
            <input
              type="file"
              accept="image/*"
              onChange={(e) => setPhoto(e.target.files?.[0] || null)}
            />
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