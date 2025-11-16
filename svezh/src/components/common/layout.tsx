import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

interface LayoutProps {
  children: React.ReactNode;
}

const Layout: React.FC<LayoutProps> = ({ children }) => {
  const { user, logout } = useAuth();
  const location = useLocation();

  const isAdmin = user?.attributes?.administrator || user?.attributes?.role === 'deptAdmin' || user?.attributes?.role === 'mruAdmin';

  const navItems = [
    { path: '/', label: 'Дашборд' },
    { path: '/map', label: 'Карта' },
    { path: '/playback', label: 'История' },
    { path: '/registry', label: 'Реестр' },
    { path: '/devices', label: 'Устройства' },
    { path: '/events', label: 'События' },
    { path: '/geozones', label: 'Геозоны' },
  ];

  // Добавляем админ панель только для администраторов
  if (isAdmin) {
    navItems.push({ path: '/admin', label: 'Администрирование' });
  }

  return (
    <div className="app-layout">
      <nav className="sidebar">
        <div className="user-info">
          <h3>{user?.name}</h3>
          <span>{user?.attributes.role}</span>
        </div>
        
        <ul className="nav-menu">
          {navItems.map(item => (
            <li key={item.path}>
              <Link 
                to={item.path} 
                className={location.pathname === item.path ? 'active' : ''}
              >
                {item.label}
              </Link>
            </li>
          ))}
        </ul>
        
        <button onClick={logout} className="logout-button">
          Выйти
        </button>
      </nav>

      
      
      <main className="main-content">
        {children}
      </main>
    </div>
  );
};

export default Layout;