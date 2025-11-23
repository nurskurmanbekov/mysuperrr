import React, { useState, useRef } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Alert,
  ActivityIndicator,
} from 'react-native';
import { CameraView } from 'expo-camera';

const CameraScreen = ({ onPhotoTaken, onCancel, mode = 'front' }) => {
  const cameraRef = useRef(null);
  const [loading, setLoading] = useState(false);
  const [facing, setFacing] = useState(mode);

  const takePicture = async () => {
    if (!cameraRef.current) {
      console.log('❌ Camera ref is null');
      Alert.alert('Ошибка', 'Камера не готова. Попробуйте еще раз.');
      return;
    }

    setLoading(true);
    try {
      console.log('📸 Taking picture...');

      const photo = await cameraRef.current.takePictureAsync({
        quality: 0.8,
        base64: false,
        exif: false,
        skipProcessing: false,
      });

      console.log('✅ Photo taken:', photo.uri);

      // Конвертируем фото в нужный формат
      const processedPhoto = {
        uri: photo.uri,
        type: 'image/jpeg',
        name: `face_check_${Date.now()}.jpg`,
      };

      onPhotoTaken(processedPhoto);
    } catch (error) {
      console.log('❌ Error taking picture:', error);
      console.log('Error details:', JSON.stringify(error));
      Alert.alert('Ошибка', `Не удалось сделать фото: ${error.message || 'Неизвестная ошибка'}`);
    } finally {
      setLoading(false);
    }
  };

  const switchCamera = () => {
    setFacing(facing === 'back' ? 'front' : 'back');
  };

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#007AFF" />
        <Text style={styles.loadingText}>Обрабатываем фото...</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <CameraView
        style={styles.camera}
        facing={facing}
        ref={cameraRef}
      >
        <View style={styles.overlay}>
          {/* Рамка для лица */}
          <View style={styles.faceFrame}>
            <View style={styles.frameCornerTopLeft} />
            <View style={styles.frameCornerTopRight} />
            <View style={styles.frameCornerBottomLeft} />
            <View style={styles.frameCornerBottomRight} />
          </View>

          <Text style={styles.instruction}>
            Расположите лицо в рамке
          </Text>
        </View>

        <View style={styles.controls}>
          <TouchableOpacity style={styles.controlButton} onPress={onCancel}>
            <Text style={styles.controlButtonText}>✕</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.captureButton}
            onPress={takePicture}
            disabled={loading}
          >
            <View style={styles.captureButtonInner} />
          </TouchableOpacity>

          <TouchableOpacity style={styles.controlButton} onPress={switchCamera}>
            <Text style={styles.controlButtonText}>🔄</Text>
          </TouchableOpacity>
        </View>
      </CameraView>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: 'black',
  },
  camera: {
    flex: 1,
  },
  overlay: {
    flex: 1,
    backgroundColor: 'transparent',
    justifyContent: 'center',
    alignItems: 'center',
  },
  faceFrame: {
    width: 250,
    height: 250,
    borderWidth: 2,
    borderColor: 'rgba(255, 255, 255, 0.5)',
    borderRadius: 10,
    position: 'relative',
  },
  frameCornerTopLeft: {
    position: 'absolute',
    top: -2,
    left: -2,
    width: 20,
    height: 20,
    borderTopWidth: 3,
    borderLeftWidth: 3,
    borderColor: '#007AFF',
  },
  frameCornerTopRight: {
    position: 'absolute',
    top: -2,
    right: -2,
    width: 20,
    height: 20,
    borderTopWidth: 3,
    borderRightWidth: 3,
    borderColor: '#007AFF',
  },
  frameCornerBottomLeft: {
    position: 'absolute',
    bottom: -2,
    left: -2,
    width: 20,
    height: 20,
    borderBottomWidth: 3,
    borderLeftWidth: 3,
    borderColor: '#007AFF',
  },
  frameCornerBottomRight: {
    position: 'absolute',
    bottom: -2,
    right: -2,
    width: 20,
    height: 20,
    borderBottomWidth: 3,
    borderRightWidth: 3,
    borderColor: '#007AFF',
  },
  instruction: {
    color: 'white',
    fontSize: 16,
    marginTop: 20,
    textAlign: 'center',
    backgroundColor: 'rgba(0,0,0,0.5)',
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 5,
  },
  controls: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingBottom: 40,
    backgroundColor: 'rgba(0,0,0,0.5)',
  },
  controlButton: {
    width: 50,
    height: 50,
    borderRadius: 25,
    backgroundColor: 'rgba(255,255,255,0.2)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  controlButtonText: {
    color: 'white',
    fontSize: 20,
  },
  captureButton: {
    width: 70,
    height: 70,
    borderRadius: 35,
    backgroundColor: 'rgba(255,255,255,0.3)',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 3,
    borderColor: 'white',
  },
  captureButtonInner: {
    width: 50,
    height: 50,
    borderRadius: 25,
    backgroundColor: 'white',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'black',
  },
  loadingText: {
    color: 'white',
    fontSize: 16,
    marginTop: 10,
  },
});

export default CameraScreen;