import { useEffect, useState } from 'react';
import { ScrollView, StyleSheet, Switch, Text, View } from 'react-native';
import { notificationApi } from '../api';
import { useAuth } from '../auth/AuthContext';
import { Card, ErrorBanner, Loading } from '../components/ui';
import { colors, common } from '../theme';
import { errorMessage } from '../utils/errors';

export default function NotificationSettingsScreen() {
  const { user } = useAuth();
  const [settings, setSettings] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    notificationApi.settings().then(setSettings).catch((e) => setError(errorMessage(e)));
  }, []);

  const toggle = async (key, value) => {
    const next = { ...settings, [key]: value };
    setSettings(next);
    try {
      setSettings(await notificationApi.updateSettings(next));
    } catch (e) {
      setError(errorMessage(e));
      setSettings(settings);
    }
  };

  if (!settings) return error ? <ErrorBanner message={error} /> : <Loading />;

  const rows = [
    ['cityPosts', `New posts in ${user.city.name}`, 'When members of your city post something.'],
    ['cityEvents', `New events & seminars in ${user.city.name}`, 'So you never miss a local seminar or meetup.'],
    ['allEvents', 'Events from all cities', 'Seminars and events other cities share with everyone.'],
    ['eventReminders', 'Event reminders', 'About an hour before events in “My upcoming events”.'],
  ];
  return (
    <ScrollView style={common.screen} contentContainerStyle={common.content}>
      <ErrorBanner message={error} onClose={() => setError('')} />
      <Card>
        {rows.map(([key, title, hint]) => (
          <View key={key} style={styles.row}>
            <View style={{ flex: 1 }}>
              <Text style={[common.text, { fontWeight: '600' }]}>{title}</Text>
              <Text style={common.small}>{hint}</Text>
            </View>
            <Switch value={settings[key]} onValueChange={(v) => toggle(key, v)} trackColor={{ true: colors.primary }} accessibilityLabel={title} />
          </View>
        ))}
      </Card>
      <Text style={common.small}>You can also turn notifications off completely in your phone's settings.</Text>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 12, borderBottomWidth: 1, borderBottomColor: colors.border },
});
