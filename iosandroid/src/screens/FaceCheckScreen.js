import React, { useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Alert,
  ActivityIndicator,
} from 'react-native';
import { CameraView, useCameraPermissions } from 'expo-camera';
import { faceCheckAPI } from '../services/api';
import { useAuth } from '../store/authContext';
import CameraScreen from './CameraScreen';

const FaceCheckScreen = () => {
  const [loading, setLoading] = useState(false);
  const [cameraVisible, setCameraVisible] = useState(false);
  const [permission, requestPermission] = useCameraPermissions();
  const { user } = useAuth();

  const takePhoto = async () => {
    if (!permission?.granted) {
      const result = await requestPermission();
      if (!result.granted) {
        Alert.alert('Ошибка', 'Необходим доступ к камере');
        return;
      }
    }

    setCameraVisible(true);
  };

  const handlePhotoTaken = async (photo) => {
    setCameraVisible(false);
    setLoading(true);

    try {
      const response = await faceCheckAPI.verifyFace(user.name, photo);
      const { ok, message, distance } = response.data;

      Alert.alert(
        ok ? 'Успешно' : 'Ошибка',
        `${message}${distance ? `\nРасстояние: ${distance.toFixed(4)}` : ''}`
      );

      // Отправляем результат на бэкенд
      await faceCheckAPI.sendResult({
        user_id: user.name,
        device_id: user.name, // или реальный device_id
        check_id: null,
        outcome: ok ? 'ok' : 'failed',
        taken_at: Date.now(),
        distance: distance,
        deadline_iso: null,
        app_version: '1.0.0',
      });

    } catch (error) {
      console.log('Face check error:', error);
      Alert.alert('Ошибка', 'Не удалось выполнить проверку');
    } finally {
      setLoading(false);
    }
  };

  if (cameraVisible) {
    return (
      <CameraScreen
        onPhotoTaken={handlePhotoTaken}
        onCancel={() => setCameraVisible(false)}
      />
    );
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Проверка Face-ID</Text>

      {loading ? (
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color="#007AFF" />
          <Text style={styles.loadingText}>Проверяем фото...</Text>
        </View>
      ) : (
        <View style={styles.buttonsContainer}>
          <TouchableOpacity style={styles.button} onPress={takePhoto}>
            <Text style={styles.buttonText}>Сделать фото</Text>
          </TouchableOpacity>
        </View>
      )}
    </View>
  );
};


const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
    backgroundColor: 'white',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    textAlign: 'center',
    marginVertical: 20,
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
  buttonsContainer: {
    flex: 1,
    justifyContent: 'center',
  },
  button: {
    backgroundColor: '#007AFF',
    padding: 15,
    borderRadius: 8,
    marginVertical: 10,
    alignItems: 'center',
  },
  secondaryButton: {
    backgroundColor: '#6C757D',
  },
  buttonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '600',
  },
});

export default FaceCheckScreen;
