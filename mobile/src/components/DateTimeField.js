import DateTimePicker, { DateTimePickerAndroid } from '@react-native-community/datetimepicker';
import { Platform, Pressable, Text, View } from 'react-native';
import { colors, common } from '../theme';
import { Field } from './ui';

const pad = (n) => String(n).padStart(2, '0');
const toText = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;

/** Date + time picker: native dialogs on Android, inline picker on iOS, text entry in the web preview. */
export default function DateTimeField({ label, value, onChange, optional }) {
  if (Platform.OS === 'web') {
    return (
      <Field label={label} placeholder="YYYY-MM-DD HH:MM" defaultValue={value ? toText(value) : ''}
        onChangeText={(t) => {
          const d = new Date(t.replace(' ', 'T'));
          onChange(Number.isNaN(d.getTime()) ? null : d);
        }} />
    );
  }
  if (Platform.OS === 'ios') {
    return (
      <View style={{ marginBottom: 12, gap: 6 }}>
        <Text style={{ fontWeight: '600', color: colors.text }}>{label}</Text>
        <View style={common.row}>
          <DateTimePicker value={value || new Date()} mode="datetime" onChange={(_, d) => d && onChange(d)} />
          {optional && value ? <Text style={{ color: colors.primary, marginLeft: 12 }} onPress={() => onChange(null)}>Clear</Text> : null}
        </View>
      </View>
    );
  }
  const pick = () => {
    const base = value || new Date();
    DateTimePickerAndroid.open({
      value: base,
      mode: 'date',
      onChange: (event, date) => {
        if (event.type !== 'set' || !date) return;
        DateTimePickerAndroid.open({
          value: date,
          mode: 'time',
          onChange: (e2, time) => {
            if (e2.type === 'set' && time) onChange(time);
          },
        });
      },
    });
  };
  return (
    <View style={{ marginBottom: 12, gap: 6 }}>
      <Text style={{ fontWeight: '600', color: colors.text }}>{label}</Text>
      <Pressable onPress={pick} style={{ borderWidth: 1, borderColor: '#d1d5db', borderRadius: 12, padding: 12, backgroundColor: '#fff' }}>
        <Text style={[common.text, !value && { color: '#9ca3af' }]}>{value ? toText(value) : 'Tap to choose date and time'}</Text>
      </Pressable>
      {optional && value ? <Text style={{ color: colors.primary }} onPress={() => onChange(null)}>Clear</Text> : null}
    </View>
  );
}
