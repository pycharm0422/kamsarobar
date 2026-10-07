import { Link } from 'expo-router';
import { useState } from 'react';
import { Image, KeyboardAvoidingView, Platform, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useAuth } from '../auth/AuthContext';
import { Button, Card, ErrorBanner, Field } from '../components/ui';
import { colors, common } from '../theme';
import { errorMessage } from '../utils/errors';

export default function LoginScreen() {
  const { login } = useAuth();
  const [mobile, setMobile] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async () => {
    setBusy(true);
    setError('');
    try {
      await login({ mobile, password });
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <SafeAreaView style={[common.screen, { backgroundColor: colors.primary }]}>
      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView contentContainerStyle={{ padding: 20, flexGrow: 1, justifyContent: 'center' }} keyboardShouldPersistTaps="handled">
          <View style={{ alignItems: 'center', marginBottom: 24 }}>
            <Image source={require('../../assets/icon.png')} style={{ width: 72, height: 72, borderRadius: 18 }} />
            <Text style={{ color: '#fff', fontSize: 30, fontWeight: '800', marginTop: 12 }}>Kamsar o Bar</Text>
            <Text style={{ color: '#ccfbf1', fontSize: 15 }}>connect · help · grow together</Text>
          </View>
          <Card>
            <Text style={common.h2}>Welcome back</Text>
            <ErrorBanner message={error} onClose={() => setError('')} />
            <Field label="Mobile number" value={mobile} onChangeText={setMobile} keyboardType="phone-pad" autoComplete="tel" placeholder="98765 43210" />
            <Field label="Password" value={password} onChangeText={setPassword} secureTextEntry autoComplete="password" onSubmitEditing={submit} />
            <Button title="Log in" onPress={submit} busy={busy} />
            <Text style={[common.muted, { textAlign: 'center', marginTop: 14 }]}>
              New here? <Link href="/register" style={{ color: colors.primary, fontWeight: '700' }}>Create an account</Link>
            </Text>
          </Card>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}
