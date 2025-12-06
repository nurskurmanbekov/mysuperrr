import React, { useState, useEffect } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { useNavigate } from 'react-router-dom';

const Login: React.FC = () => {
  const [login, setLogin] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login: authLogin, user } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (user) {
      navigate('/');
    }
  }, [user, navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await authLogin(login, password);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Ошибка входа');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      {/* Шапка */}
     <header className="login-header">
  <div className="login-header-content">
    <div className="login-emblem">
      <img src="/images/Emblem.svg" alt="Герб Кыргызской Республики" className="emblem-image" />
    </div>
    <h1 className="login-title">Система Мониторинга Департамента Пробации</h1>
  </div>
</header>

      {/* Форма логина */}
      <div className="login-container">
        <form onSubmit={handleSubmit} className="login-form">
          <h2>Вход в систему</h2>
          
          {error && <div className="error-message">{error}</div>}
          
          <div className="form-group">
            <label>Логин:</label>
            <input
              type="text"
              value={login}
              onChange={(e) => setLogin(e.target.value)}
              placeholder="admin_user"
              required
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label>Пароль:</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="password123"
              required
              disabled={loading}
            />
          </div>
          
          <button 
            type="submit" 
            className="login-button"
            disabled={loading}
          >
            {loading ? '⏳ Вход...' : '🔐 Войти'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default Login;