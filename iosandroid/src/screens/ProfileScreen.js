import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Alert,
  ActivityIndicator,
} from 'react-native';
import { useAuth } from '../store/authContext';
import { mobileAPI } from '../services/api';
import AsyncStorage from '@react-native-async-storage/async-storage';

const ProfileScreen = () => {
  const { user, logout } = useAuth();
  const [loading, setLoading] = useState(true);
  const [clientData, setClientData] = useState(null);

  // 🔍 ДИАГНОСТИКА: Проверяем user объект
  useEffect(() => {
    if (user) {
      console.log('═══════════════════════════════════════════');
      console.log('🔍 ProfileScreen - USER OBJECT:');
      console.log('user:', JSON.stringify(user, null, 2));
      console.log('─────────────────────────────────────────');
      console.log('user.administrator type:', typeof user.administrator);
      console.log('user.administrator value:', user.administrator);
      console.log('user.administrator === true:', user.administrator === true);
      console.log('user.administrator === "true":', user.administrator === "true");
      console.log('═══════════════════════════════════════════');
    }
  }, [user]);

  useEffect(() => {
    loadProfileData();
  }, []);

  const loadProfileData = async () => {
    try {
      setLoading(true);
      // Загружаем данные клиента из реестра
      const response = await mobileAPI.getProfile();
      setClientData(response.data);
      console.log('Client data loaded:', response.data);
    } catch (error) {
      console.log('Error loading profile data:', error);
      Alert.alert('Ошибка', 'Не удалось загрузить данные профиля');
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    Alert.alert(
      'Выход',
      'Вы уверены, что хотите выйти?',
      [
        { text: 'Отмена', style: 'cancel' },
        { text: 'Выйти', onPress: logout, style: 'destructive' },
      ]
    );
  };

  const clearCache = async () => {
    try {
      await AsyncStorage.clear();
      Alert.alert('Успех', 'Кэш очищен');
    } catch (error) {
      Alert.alert('Ошибка', 'Не удалось очистить кэш');
    }
  };

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#007AFF" />
        <Text style={styles.loadingText}>Загрузка данных...</Text>
      </View>
    );
  }

  return (
    <ScrollView style={styles.container}>
      {/* Информация о пользователе */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Личная информация</Text>
        <View style={styles.infoCard}>
          <InfoRow label="ФИО" value={clientData?.fio || 'Не указано'} />
          <InfoRow label="Возраст" value={clientData?.age ? `${clientData.age} лет` : 'Не указано'} />
          <InfoRow label="ИНН" value={clientData?.inn || user.name} />
        </View>
      </View>

      {/* Настройки */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Настройки</Text>

        <TouchableOpacity style={styles.settingButton} onPress={clearCache}>
          <Text style={styles.settingButtonText}>Очистить кэш</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.settingButton}>
          <Text style={styles.settingButtonText}>О приложении</Text>
        </TouchableOpacity>
      </View>

      {/* Выход */}
      <TouchableOpacity style={styles.logoutButton} onPress={handleLogout}>
        <Text style={styles.logoutButtonText}>Выйти из аккаунта</Text>
      </TouchableOpacity>

      <Text style={styles.version}>Версия 1.0.0</Text>
    </ScrollView>
  );
};

const InfoRow = ({ label, value }) => (
  <View style={styles.infoRow}>
    <Text style={styles.infoLabel}>{label}:</Text>
    <Text style={styles.infoValue}>{value}</Text>
  </View>
);

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    marginTop: 10,
    fontSize: 16,
    color: '#666',
  },
  section: {
    marginBottom: 20,
    paddingHorizontal: 15,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 10,
    color: '#333',
  },
  infoCard: {
    backgroundColor: 'white',
    padding: 15,
    borderRadius: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 3,
    elevation: 2,
  },
  infoRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 8,
  },
  infoLabel: {
    fontWeight: '600',
    color: '#666',
  },
  infoValue: {
    color: '#333',
  },
  deviceCard: {
    backgroundColor: 'white',
    padding: 15,
    borderRadius: 8,
    marginBottom: 10,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 3,
    elevation: 2,
  },
  deviceName: {
    fontSize: 16,
    fontWeight: 'bold',
    marginBottom: 5,
  },
  deviceId: {
    color: '#666',
    marginBottom: 3,
  },
  deviceStatus: {
    color: '#666',
    marginBottom: 8,
  },
  attributes: {
    borderTopWidth: 1,
    borderTopColor: '#eee',
    paddingTop: 8,
  },
  attribute: {
    fontSize: 12,
    color: '#888',
    marginBottom: 2,
  },
  historyItem: {
    backgroundColor: 'white',
    padding: 12,
    borderRadius: 6,
    marginBottom: 8,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  historyDate: {
    color: '#666',
  },
  historyOutcome: {
    fontWeight: '600',
  },
  success: {
    color: '#4CAF50',
  },
  error: {
    color: '#F44336',
  },
  noData: {
    textAlign: 'center',
    color: '#999',
    fontStyle: 'italic',
    padding: 20,
  },
  settingButton: {
    backgroundColor: 'white',
    padding: 15,
    borderRadius: 8,
    marginBottom: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 3,
    elevation: 2,
  },
  settingButtonText: {
    fontSize: 16,
    color: '#333',
  },
  logoutButton: {
    backgroundColor: '#DC3545',
    margin: 15,
    padding: 15,
    borderRadius: 8,
    alignItems: 'center',
  },
  logoutButtonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '600',
  },
  version: {
    textAlign: 'center',
    color: '#999',
    marginBottom: 20,
  },
});

export default ProfileScreen;