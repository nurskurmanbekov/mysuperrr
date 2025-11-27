import { useState, useEffect, createContext, useContext, ReactNode } from 'react';
import { authAPI } from '../services/api';
import { User } from '../types';
import React from 'react';

interface AuthContextType {
  user: User | null;
  login: (login: string, password: string) => Promise<void>;
  logout: () => void;
  loading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider = ({ children }: AuthProviderProps) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    checkAuth();
  }, []);

  const checkAuth = async () => {
    const token = localStorage.getItem('authToken');
    if (token) {
      try {
        const response = await authAPI.getMe();
        setUser(response.data.user);
      } catch (error) {
        localStorage.removeItem('authToken');
      }
    }
    setLoading(false);
  };

  const login = async (login: string, password: string) => {
    const response = await authAPI.login(login, password);
    const { token, user } = response.data;

    localStorage.setItem('authToken', token);
    setUser(user);
    // Редирект будет происходить через изменение состояния user
  };

  const logout = () => {
    localStorage.removeItem('authToken');
    setUser(null);
    authAPI.logout();
    // Редирект будет происходить через изменение состояния user
  };

  const value = {
    user,
    login,
    logout,
    loading
  };

  return React.createElement(
    AuthContext.Provider,
    { value: value },
    children
  );
};