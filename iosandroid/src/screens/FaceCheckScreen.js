import React, { useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Alert,
  ActivityIndicator,
} from 'react-native';
import { Camera } from 'expo-camera';
import * as ImagePicker from 'expo-image-picker';
import { faceCheckAPI } from '../services/api';
import { useAuth } from '../store/authContext';

const FaceCheckScreen = () => {
  const [loading, setLoading] = useState(false);
  const [hasPermission, setHasPermission] = useState(null);
  const [cameraVisible, setCameraVisible] = useState(false);
  const { user } = useAuth();

  const requestCameraPermission = async () => {
    const { status } = await Camera.requestCameraPermissionsAsync();
    setHasPermission(status === 'granted');
    return status === 'granted';
  };

  const takePhoto = async () => {
    const hasPermission = await requestCameraPermission();
    if (!hasPermission) {
      Alert.alert('Ошибка', 'Необходим доступ к камере');
      return;
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

  const pickImage = async () => {
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsEditing: true,
      quality: 0.8,
    });

    if (!result.canceled) {
      handlePhotoTaken(result.assets[0]);
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
          
          <TouchableOpacity style={[styles.button, styles.secondaryButton]} onPress={pickImage}>
            <Text style={styles.buttonText}>Выбрать из галереи</Text>
          </TouchableOpacity>
        </View>
      )}
    </View>
  );
};

// Компонент камеры
const CameraScreen = ({ onPhotoTaken, onCancel }) => {
  const [cameraRef, setCameraRef] = useState(null);

  const takePicture = async () => {
    if (cameraRef) {
      const photo = await cameraRef.takePictureAsync();
      onPhotoTaken(photo);
    }
  };

  return (
    <View style={styles.cameraContainer}>
      <Camera
        style={styles.camera}
        type={Camera.Constants.Type.front}
        ref={ref => setCameraRef(ref)}
      >
        <View style={styles.cameraControls}>
          <TouchableOpacity style={styles.cancelButton} onPress={onCancel}>
            <Text style={styles.cancelButtonText}>×</Text>
          </TouchableOpacity>
          
          <TouchableOpacity style={styles.captureButton} onPress={takePicture}>
            <View style={styles.captureButtonInner} />
          </TouchableOpacity>
        </View>
      </Camera>
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
  cameraContainer: {
    flex: 1,
  },
  camera: {
    flex: 1,
  },
  cameraControls: {
    flex: 1,
    backgroundColor: 'transparent',
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-end',
    padding: 20,
  },
  cancelButton: {
    backgroundColor: 'rgba(0,0,0,0.5)',
    width: 50,
    height: 50,
    borderRadius: 25,
    justifyContent: 'center',
    alignItems: 'center',
  },
  cancelButtonText: {
    color: 'white',
    fontSize: 30,
    lineHeight: 30,
  },
  captureButton: {
    borderWidth: 4,
    borderColor: 'white',
    width: 80,
    height: 80,
    borderRadius: 40,
    justifyContent: 'center',
    alignItems: 'center',
  },
  captureButtonInner: {
    backgroundColor: 'white',
    width: 60,
    height: 60,
    borderRadius: 30,
  },
});

export default FaceCheckScreen;