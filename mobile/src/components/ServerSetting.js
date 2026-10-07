import { useState } from 'react';
import { Text, View } from 'react-native';
import { changeApiUrl, getApiUrl } from '../config';
import { resetCities } from '../hooks/useCities';
import { colors, common } from '../theme';
import { Button, Field } from './ui';

/** Shows which server the app talks to and lets the member change it (e.g. their PC's Wi-Fi address). */
export default function ServerSetting() {
  const [editing, setEditing] = useState(false);
  const [value, setValue] = useState(getApiUrl());
  const [current, setCurrent] = useState(getApiUrl());
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const save = async () => {
    setBusy(true);
    setError('');
    try {
      const url = await changeApiUrl(value);
      resetCities();
      setCurrent(url);
      setValue(url);
      setEditing(false);
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  };

  if (!editing) {
    return (
      <Text style={[common.small, { textAlign: 'center', marginTop: 16, color: '#ccfbf1' }]}>
        Server: {current}{'  '}
        <Text style={{ color: '#fff', fontWeight: '700', textDecorationLine: 'underline' }} onPress={() => setEditing(true)}>Change</Text>
      </Text>
    );
  }
  return (
    <View style={[common.card, { marginTop: 12 }]}>
      <Text style={common.h3}>Server address</Text>
      <Text style={[common.small, { marginBottom: 8 }]}>
        Your website's address (e.g. https://kamsarobar.in), or for testing, your computer's Wi-Fi address with port 8080
        (e.g. 192.168.1.5:8080).
      </Text>
      <Field value={value} onChangeText={setValue} autoCapitalize="none" autoCorrect={false} keyboardType="url" label="Address" />
      {error ? <Text style={{ color: colors.danger, marginBottom: 8 }}>{error}</Text> : null}
      <View style={[common.row, { gap: 8 }]}>
        <Button variant="ghost" title="Cancel" onPress={() => { setEditing(false); setError(''); }} />
        <Button title="Save & test" onPress={save} busy={busy} style={{ flex: 1 }} />
      </View>
    </View>
  );
}
