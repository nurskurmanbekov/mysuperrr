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
import { deviceAPI, faceCheckAPI } from '../services/api';
import AsyncStorage from '@react-native-async-storage/async-storage';

const ProfileScreen = () => {
  const { user, logout } = useAuth();
  const [devices, setDevices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [faceCheckHistory, setFaceCheckHistory] = useState([]);

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

      // Загружаем устройства пользователя
      try {
        const devicesResponse = await deviceAPI.getDeviceByUniqueId(user.name);

        // 🔍 ДИАГНОСТИКА: Проверяем devices данные
        console.log('═══════════════════════════════════════════');
        console.log('🔍 ProfileScreen - DEVICES RESPONSE:');
        console.log('devicesResponse.data:', JSON.stringify(devicesResponse.data, null, 2));
        if (devicesResponse.data && devicesResponse.data[0]) {
          console.log('─────────────────────────────────────────');
          console.log('First device.disabled type:', typeof devicesResponse.data[0].disabled);
          console.log('First device.disabled value:', devicesResponse.data[0].disabled);
        }
        console.log('═══════════════════════════════════════════');

        if (devicesResponse.data) {
          setDevices(devicesResponse.data);
        }
      } catch (deviceError) {
        // Устройство не найдено в Traccar - это нормально для новых пользователей
        console.log('No device found for user, showing empty device list');
        setDevices([]);
      }

      // Здесь можно добавить загрузку истории проверок
      // const historyResponse = await faceCheckAPI.getHistory(user.name);
      // setFaceCheckHistory(historyResponse.data);

    } catch (error) {
      console.log('Error loading profile data:', error);
      // Не показываем Alert при отсутствии устройств
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
          <InfoRow label="ИНН" value={user.name} />
          <InfoRow label="Email" value={user.email} />
          <InfoRow 
            label="Статус" 
            value={user.administrator ? 'Администратор' : 'Пользователь'} 
          />
        </View>
      </View>

      {/* Устройства */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Мои устройства</Text>
        {devices.length > 0 ? (
          devices.map((device, index) => (
            <View key={index} style={styles.deviceCard}>
              <Text style={styles.deviceName}>{device.name || 'Устройство'}</Text>
              <Text style={styles.deviceId}>ID: {device.uniqueId}</Text>
              <Text style={styles.deviceStatus}>
                Статус: {device.status || 'неизвестно'}
              </Text>
              
              {device.attributes && (
                <View style={styles.attributes}>
                  {device.attributes.lastFaceAt && (
                    <Text style={styles.attribute}>
                      Последняя проверка: {new Date(device.attributes.lastFaceAt).toLocaleString()}
                    </Text>
                  )}
                  {device.attributes.lastFaceOkAt && (
                    <Text style={styles.attribute}>
                      Успешная проверка: {new Date(device.attributes.lastFaceOkAt).toLocaleString()}
                    </Text>
                  )}
                  {device.attributes.lastFaceDist && (
                    <Text style={styles.attribute}>
                      Точность: {(1 - device.attributes.lastFaceDist).toFixed(4)}
                    </Text>
                  )}
                </View>
              )}
            </View>
          ))
        ) : (
          <Text style={styles.noData}>Устройства не найдены</Text>
        )}
      </View>

      {/* История проверок (заглушка) */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>История проверок</Text>
        {faceCheckHistory.length > 0 ? (
          faceCheckHistory.map((check, index) => (
            <View key={index} style={styles.historyItem}>
              <Text style={styles.historyDate}>
                {new Date(check.taken_at).toLocaleString()}
              </Text>
              <Text style={[
                styles.historyOutcome,
                check.outcome === 'ok' ? styles.success : styles.error
              ]}>
                {getOutcomeText(check.outcome)}
              </Text>
            </View>
          ))
        ) : (
          <Text style={styles.noData}>История проверок пуста</Text>
        )}
      </View>

      {/* Настройки */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Настройки</Text>
        
        <TouchableOpacity style={styles.settingButton} onPress={clearCache}>
          <Text style={styles.settingButtonText}>Очистить кэш</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.settingButton}>
          <Text style={styles.settingButtonText}>Уведомления</Text>
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

const getOutcomeText = (outcome) => {
  const outcomes = {
    'ok': 'Успешно',
    'failed': 'Не пройдено',
    'declined': 'Отклонено',
    'failed_network': 'Ошибка сети',
    'late_ok': 'Успешно (поздно)',
    'late_failed': 'Не пройдено (поздно)',
  };
  return outcomes[outcome] || outcome;
};

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