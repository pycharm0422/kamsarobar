import { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, Text } from 'react-native';
import { useAuth } from '../auth/AuthContext';
import CityPicker from '../components/CityPicker';
import { Button, Card, ErrorBanner, Field } from '../components/ui';
import { common } from '../theme';
import { errorMessage } from '../utils/errors';

/** Form 1 - name, mobile and city (plus a password). The work profile (form 2) comes next. */
export default function RegisterScreen() {
  const { register } = useAuth();
  const [form, setForm] = useState({ name: '', mobile: '', cityId: '', password: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (key) => (value) => setForm((f) => ({ ...f, [key]: value }));

  const submit = async () => {
    if (!form.cityId) {
      setError('Please choose your city.');
      return;
    }
    setBusy(true);
    setError('');
    try {
      await register({ ...form, cityId: Number(form.cityId) });
      // Signed in: the app switches to the main tabs, where "Me" leads to the work profile.
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <KeyboardAvoidingView style={common.screen} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={common.content} keyboardShouldPersistTaps="handled">
        <Card>
          <Text style={common.h2}>Step 1 of 2 · the basics</Text>
          <Text style={[common.muted, { marginBottom: 12 }]}>You can add your work details right after.</Text>
          <ErrorBanner message={error} onClose={() => setError('')} />
          <Field label="Full name" value={form.name} onChangeText={set('name')} autoComplete="name" />
          <Field label="Mobile number (WhatsApp)" value={form.mobile} onChangeText={set('mobile')} keyboardType="phone-pad"
            hint="10-digit Indian numbers get +91 automatically. Add the country code for other countries." />
          <CityPicker label="City you live in" value={form.cityId} onChange={set('cityId')} />
          <Field label="Choose a password" value={form.password} onChangeText={set('password')} secureTextEntry autoComplete="new-password"
            hint="At least 6 characters" />
          <Button title="Create account" onPress={submit} busy={busy} />
        </Card>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}
