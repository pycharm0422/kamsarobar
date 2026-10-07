import { useState } from 'react';
import { Linking, StyleSheet, Text, View } from 'react-native';
import { eventApi } from '../api';
import { colors, common } from '../theme';
import { errorMessage } from '../utils/errors';
import { formatEventWhen } from '../utils/format';
import { googleCalendarLink } from '../utils/links';
import { Button } from './ui';

/** Date badge, time, venue, online link and the "Add to my events" toggle. */
export default function EventBox({ post, onChange }) {
  const { event } = post;
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const start = new Date(event.startsAt);

  const toggle = async () => {
    setBusy(true);
    setError('');
    try {
      onChange?.(await (event.attending ? eventApi.leave(post.id) : eventApi.attend(post.id)));
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <View style={styles.box}>
      <View style={styles.badge}>
        <Text style={styles.month}>{start.toLocaleDateString('en-IN', { month: 'short' }).toUpperCase()}</Text>
        <Text style={styles.day}>{start.getDate()}</Text>
      </View>
      <View style={{ flex: 1, gap: 4 }}>
        <Text style={[common.text, { fontWeight: '700' }]}>🗓 {formatEventWhen(event.startsAt, event.endsAt)}</Text>
        {event.location ? <Text style={common.text}>📍 {event.location}</Text> : null}
        {event.link ? (
          <Text style={styles.link} onPress={() => Linking.openURL(event.link)}>🔗 Join online</Text>
        ) : null}
        {event.ended ? (
          <Text style={common.small}>This event has ended · {event.attendeeCount} attended</Text>
        ) : (
          <View style={[common.row, { gap: 10, marginTop: 6, flexWrap: 'wrap' }]}>
            <Button small busy={busy} variant={event.attending ? 'going' : 'primary'}
              title={event.attending ? '✓ In my events' : '＋ Add to my events'} onPress={toggle} />
            <Text style={common.small}>{event.attendeeCount} going</Text>
          </View>
        )}
        {event.attending && !event.ended ? (
          <Text style={styles.link} onPress={() => Linking.openURL(googleCalendarLink(post))}>Add to Google Calendar</Text>
        ) : null}
        {error ? <Text style={{ color: colors.danger }}>{error}</Text> : null}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  box: { flexDirection: 'row', gap: 12, backgroundColor: colors.eventBg, borderColor: colors.eventBorder, borderWidth: 1, borderRadius: 12, padding: 12, marginVertical: 8 },
  badge: { width: 54, borderRadius: 10, backgroundColor: '#fff', borderWidth: 1, borderColor: '#ddd6fe', overflow: 'hidden', alignItems: 'center', alignSelf: 'flex-start' },
  month: { backgroundColor: colors.violet, color: '#fff', width: '100%', textAlign: 'center', fontSize: 11, fontWeight: '800', paddingVertical: 2 },
  day: { fontSize: 22, fontWeight: '800', paddingVertical: 2, color: colors.text },
  link: { color: colors.primary, fontWeight: '600' },
});
