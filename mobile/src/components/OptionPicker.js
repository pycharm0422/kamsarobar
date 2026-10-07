import { useState } from 'react';
import { FlatList, Modal, Pressable, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors, common } from '../theme';

/** A tap-to-choose field that opens a full list (React Native has no built-in dropdown). */
export default function OptionPicker({ label, value, options, onChange, placeholder = 'Choose…', compact }) {
  const [open, setOpen] = useState(false);
  const selected = options.find((o) => String(o.value) === String(value ?? ''));
  return (
    <View style={!compact && { marginBottom: 12, gap: 6 }}>
      {label ? <Text style={styles.label}>{label}</Text> : null}
      <Pressable style={[styles.field, compact && styles.compact]} onPress={() => setOpen(true)}
        accessibilityRole="button" accessibilityLabel={label || placeholder}>
        <Text style={[common.text, !selected && { color: '#9ca3af' }]} numberOfLines={1}>
          {selected ? selected.label : placeholder}
        </Text>
        <Text style={common.muted}>▾</Text>
      </Pressable>
      <Modal visible={open} animationType="slide" onRequestClose={() => setOpen(false)}>
        <SafeAreaView style={common.screen}>
          <View style={styles.header}>
            <Text style={common.h2}>{label || placeholder}</Text>
            <Pressable onPress={() => setOpen(false)} accessibilityRole="button"><Text style={styles.close}>Close</Text></Pressable>
          </View>
          <FlatList
            data={options}
            keyExtractor={(o) => String(o.value)}
            renderItem={({ item }) => {
              const active = String(item.value) === String(value ?? '');
              return (
                <Pressable style={[styles.option, active && styles.optionActive]} onPress={() => { onChange(item.value); setOpen(false); }}>
                  <Text style={[common.text, active && { fontWeight: '700', color: colors.primaryDark }]}>{item.label}</Text>
                  {active ? <Text style={{ color: colors.primary }}>✓</Text> : null}
                </Pressable>
              );
            }}
          />
        </SafeAreaView>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  label: { fontSize: 14, fontWeight: '600', color: colors.text },
  field: {
    flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', gap: 8,
    borderWidth: 1, borderColor: '#d1d5db', borderRadius: 12, paddingHorizontal: 12, paddingVertical: 12, backgroundColor: '#fff',
  },
  compact: { paddingVertical: 8 },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: 16 },
  close: { color: colors.primary, fontWeight: '700', fontSize: 16 },
  option: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 15, paddingHorizontal: 18, borderBottomWidth: 1, borderBottomColor: colors.border, backgroundColor: '#fff' },
  optionActive: { backgroundColor: colors.primarySoft },
});
