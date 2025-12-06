import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  Alert,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { useAuth } from '../store/authContext';
import {
  isBiometricAvailable,
  hasBiometricCredentials,
  authenticateWithBiometrics,
  getBiometricCredentials,
  getBiometricTypeName,
  saveBiometricCredentials,
} from '../services/biometricAuth';

const LoginScreen = ({ navigation }) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [biometricAvailable, setBiometricAvailable] = useState(false);
  const [biometricHasCredentials, setBiometricHasCredentials] = useState(false);
  const [biometricType, setBiometricType] = useState('Биометрия');
  const [showBiometricOption, setShowBiometricOption] = useState(false);
  const { login } = useAuth();

  useEffect(() => {
    checkBiometricAvailability();
  }, []);

  const checkBiometricAvailability = async () => {
    const available = await isBiometricAvailable();
    const hasCredentials = await hasBiometricCredentials();
    const typeName = await getBiometricTypeName();

    setBiometricAvailable(available);
    setBiometricHasCredentials(hasCredentials);
    setBiometricType(typeName);
    setShowBiometricOption(available && hasCredentials);

    // Автоматически предлагаем биометрию при запуске, если доступна
    if (available && hasCredentials) {
      setTimeout(() => {
        handleBiometricLogin();
      }, 500);
    }
  };

  const handleLogin = async () => {
    if (!email || !password) {
      Alert.alert('Ошибка', 'Пожалуйста, заполните все поля');
      return;
    }

    setLoading(true);
    const result = await login(email, password);
    setLoading(false);

    if (!result.success) {
      Alert.alert('Ошибка входа', result.message);
    } else {
      // Предлагаем сохранить учетные данные для биометрии
      if (biometricAvailable && !biometricHasCredentials) {
        Alert.alert(
          `Включить ${biometricType}?`,
          `Хотите использовать ${biometricType} для быстрого входа в приложение?`,
          [
            {
              text: 'Нет',
              style: 'cancel',
              onPress: () => navigation.replace('Home'),
            },
            {
              text: 'Да',
              onPress: async () => {
                await saveBiometricCredentials(email, password);
                setBiometricHasCredentials(true);
                setShowBiometricOption(true);
                navigation.replace('Home');
              },
            },
          ]
        );
      } else {
        navigation.replace('Home');
      }
    }
  };

  const handleBiometricLogin = async () => {
    const credentials = await getBiometricCredentials();
    if (!credentials) {
      Alert.alert('Ошибка', 'Учетные данные не найдены');
      return;
    }

    const authResult = await authenticateWithBiometrics();
    if (!authResult.success) {
      console.log('Биометрическая аутентификация отменена');
      return;
    }

    setLoading(true);
    const result = await login(credentials.login, credentials.password);
    setLoading(false);

    if (!result.success) {
      Alert.alert('Ошибка входа', result.message);
    } else {
      navigation.replace('Home');
    }
  };

  return (
    <KeyboardAvoidingView 
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <View style={styles.form}>
        <Text style={styles.title}>Электронный надзор</Text>
        
        <TextInput
          style={styles.input}
          placeholder="ИНН (логин)"
          value={email}
          onChangeText={setEmail}
          autoCapitalize="none"
        />
        
        <TextInput
          style={styles.input}
          placeholder="Пароль"
          value={password}
          onChangeText={setPassword}
          secureTextEntry={true}
        />
        
        <TouchableOpacity
          style={[styles.button, loading && styles.buttonDisabled]}
          onPress={handleLogin}
          disabled={loading}
        >
          <Text style={styles.buttonText}>
            {loading ? 'Вход...' : 'Войти'}
          </Text>
        </TouchableOpacity>

        {showBiometricOption && (
          <TouchableOpacity
            style={[styles.biometricButton, loading && styles.buttonDisabled]}
            onPress={handleBiometricLogin}
            disabled={loading}
          >
            <Text style={styles.biometricButtonText}>
              {`Войти с ${biometricType}`}
            </Text>
          </TouchableOpacity>
        )}

        {biometricAvailable && !biometricHasCredentials && (
          <Text style={styles.biometricHint}>
            {`После первого входа вы сможете использовать ${biometricType}`}
          </Text>
        )}
      </View>
    </KeyboardAvoidingView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    padding: 20,
    backgroundColor: '#f5f5f5',
  },
  form: {
    backgroundColor: 'white',
    padding: 20,
    borderRadius: 10,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: 30,
    color: '#333',
  },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    padding: 15,
    marginBottom: 15,
    borderRadius: 8,
    fontSize: 16,
  },
  button: {
    backgroundColor: '#007AFF',
    padding: 15,
    borderRadius: 8,
    alignItems: 'center',
  },
  buttonDisabled: {
    backgroundColor: '#ccc',
  },
  buttonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '600',
  },
  biometricButton: {
    backgroundColor: '#34C759',
    padding: 15,
    borderRadius: 8,
    alignItems: 'center',
    marginTop: 10,
  },
  biometricButtonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '600',
  },
  biometricHint: {
    fontSize: 12,
    color: '#666',
    textAlign: 'center',
    marginTop: 15,
    lineHeight: 16,
  },
});

export default LoginScreen;