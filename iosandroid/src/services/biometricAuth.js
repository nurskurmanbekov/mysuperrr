import * as LocalAuthentication from 'expo-local-authentication';
import AsyncStorage from '@react-native-async-storage/async-storage';

const BIOMETRIC_CREDENTIALS_KEY = 'biometric_credentials';

/**
 * Проверяет, поддерживает ли устройство биометрическую аутентификацию
 */
export const isBiometricSupported = async () => {
  try {
    const compatible = await LocalAuthentication.hasHardwareAsync();
    return compatible;
  } catch (error) {
    console.error('Error checking biometric support:', error);
    return false;
  }
};

/**
 * Проверяет, есть ли сохраненные биометрические данные на устройстве
 */
export const isBiometricEnrolled = async () => {
  try {
    const enrolled = await LocalAuthentication.isEnrolledAsync();
    return enrolled;
  } catch (error) {
    console.error('Error checking biometric enrollment:', error);
    return false;
  }
};

/**
 * Получает доступные типы биометрической аутентификации
 */
export const getSupportedBiometricTypes = async () => {
  try {
    const types = await LocalAuthentication.supportedAuthenticationTypesAsync();
    return types;
  } catch (error) {
    console.error('Error getting biometric types:', error);
    return [];
  }
};

/**
 * Возвращает название биометрического метода для отображения пользователю
 */
export const getBiometricTypeName = async () => {
  const types = await getSupportedBiometricTypes();

  if (types.includes(LocalAuthentication.AuthenticationType.FACIAL_RECOGNITION)) {
    return 'Face ID';
  } else if (types.includes(LocalAuthentication.AuthenticationType.FINGERPRINT)) {
    return 'Touch ID';
  } else if (types.includes(LocalAuthentication.AuthenticationType.IRIS)) {
    return 'Iris';
  }

  return 'Биометрия';
};

/**
 * Выполняет биометрическую аутентификацию
 */
export const authenticateWithBiometrics = async () => {
  try {
    const biometricType = await getBiometricTypeName();

    const result = await LocalAuthentication.authenticateAsync({
      promptMessage: `Войдите с помощью ${biometricType}`,
      fallbackLabel: 'Использовать пароль',
      cancelLabel: 'Отмена',
      disableDeviceFallback: false,
    });

    return {
      success: result.success,
      error: result.error,
    };
  } catch (error) {
    console.error('Biometric authentication error:', error);
    return {
      success: false,
      error: error.message || 'Ошибка биометрической аутентификации',
    };
  }
};

/**
 * Сохраняет учетные данные для биометрического входа
 */
export const saveBiometricCredentials = async (login, password) => {
  try {
    const credentials = { login, password };
    await AsyncStorage.setItem(BIOMETRIC_CREDENTIALS_KEY, JSON.stringify(credentials));
    return true;
  } catch (error) {
    console.error('Error saving biometric credentials:', error);
    return false;
  }
};

/**
 * Получает сохраненные учетные данные
 */
export const getBiometricCredentials = async () => {
  try {
    const credentialsJson = await AsyncStorage.getItem(BIOMETRIC_CREDENTIALS_KEY);
    if (credentialsJson) {
      return JSON.parse(credentialsJson);
    }
    return null;
  } catch (error) {
    console.error('Error getting biometric credentials:', error);
    return null;
  }
};

/**
 * Удаляет сохраненные биометрические учетные данные
 */
export const removeBiometricCredentials = async () => {
  try {
    await AsyncStorage.removeItem(BIOMETRIC_CREDENTIALS_KEY);
    return true;
  } catch (error) {
    console.error('Error removing biometric credentials:', error);
    return false;
  }
};

/**
 * Проверяет, доступна ли биометрическая аутентификация
 * (устройство поддерживает и пользователь настроил)
 */
export const isBiometricAvailable = async () => {
  const supported = await isBiometricSupported();
  const enrolled = await isBiometricEnrolled();
  return supported && enrolled;
};

/**
 * Проверяет, сохранены ли учетные данные для биометрического входа
 */
export const hasBiometricCredentials = async () => {
  const credentials = await getBiometricCredentials();
  return credentials !== null;
};
