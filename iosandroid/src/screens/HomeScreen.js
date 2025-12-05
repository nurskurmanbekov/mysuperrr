
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
import gpsService from '../services/gpsService';

const HomeScreen = ({ navigation }) => {
  const { user, logout } = useAuth();
  const [gpsTracking, setGpsTracking] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    // Проверяем статус GPS при загрузке
    checkGpsStatus();
  }, []);

  const checkGpsStatus = () => {
    setGpsTracking(gpsService.isTracking);
  };

  const handleStartGPS = async () => {
    try {
      setLoading(true);
      await gpsService.startTracking(user?.inn || user?.name);
      setGpsTracking(true);
      Alert.alert('Успех', 'Отслеживание GPS запущено');
    } catch (error) {
      console.log('GPS start error:', error);
      Alert.alert('Ошибка', 'Не удалось запустить GPS отслеживание');
    } finally {
      setLoading(false);
    }
  };

  const handleStopGPS = async () => {
    try {
      setLoading(true);
      await gpsService.stopTracking();
      setGpsTracking(false);
      Alert.alert('Успех', 'Отслеживание GPS остановлено');
    } catch (error) {
      console.log('GPS stop error:', error);
      Alert.alert('Ошибка', 'Не удалось остановить GPS отслеживание');
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <ScrollView>
        <View style={styles.header}>
          <Text style={styles.welcome}>Добро пожаловать</Text>
          <Text style={styles.userInfo}>{user?.attributes?.fio || user?.name || user?.inn}</Text>
        </View>

        <View style={styles.menu}>
          <TouchableOpacity
            style={styles.menuItem}
            onPress={() => navigation.navigate('FaceCheck')}
          >
            <Text style={styles.menuItemText}>📸 Проверка Face-ID</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.menuItem}
            onPress={() => navigation.navigate('Profile')}
          >
            <Text style={styles.menuItemText}>👤 Мой профиль</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.menuItem}
            onPress={() => navigation.navigate('Notifications')}
          >
            <Text style={styles.menuItemText}>🔔 Уведомления</Text>
          </TouchableOpacity>

          {/* GPS контроль */}
          {loading ? (
            <View style={[styles.menuItem, styles.gpsItem]}>
              <ActivityIndicator size="small" color="#fff" />
              <Text style={styles.menuItemText}>Загрузка...</Text>
            </View>
          ) : gpsTracking ? (
            <TouchableOpacity
              style={[styles.menuItem, styles.gpsActiveButton]}
              onPress={handleStopGPS}
            >
              <Text style={styles.menuItemText}>📍 GPS активен - Остановить</Text>
            </TouchableOpacity>
          ) : (
            <TouchableOpacity
              style={[styles.menuItem, styles.gpsInactiveButton]}
              onPress={handleStartGPS}
            >
              <Text style={styles.menuItemText}>📍 Запустить GPS</Text>
            </TouchableOpacity>
          )}

          <TouchableOpacity
            style={[styles.menuItem, styles.logoutButton]}
            onPress={logout}
          >
            <Text style={styles.logoutButtonText}>🚪 Выйти</Text>
          </TouchableOpacity>
        </View>
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: 'white',
  },
  header: {
    padding: 20,
    backgroundColor: '#f8f9fa',
    borderBottomWidth: 1,
    borderBottomColor: '#dee2e6',
  },
  welcome: {
    fontSize: 18,
    color: '#6C757D',
    marginBottom: 5,
  },
  userInfo: {
    fontSize: 22,
    fontWeight: 'bold',
    color: '#333',
  },
  userFio: {
    fontSize: 16,
    color: '#666',
    marginTop: 5,
  },
  menu: {
    padding: 20,
  },
  menuItem: {
    backgroundColor: '#007AFF',
    padding: 15,
    borderRadius: 8,
    marginVertical: 8,
    alignItems: 'center',
  },
  menuItemText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '600',
  },
  gpsItem: {
    flexDirection: 'row',
    gap: 10,
  },
  gpsActiveButton: {
    backgroundColor: '#28A745',
  },
  gpsInactiveButton: {
    backgroundColor: '#FFC107',
  },
  logoutButton: {
    backgroundColor: '#DC3545',
    marginTop: 20,
  },
  logoutButtonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '600',
  },
});

export default HomeScreen;