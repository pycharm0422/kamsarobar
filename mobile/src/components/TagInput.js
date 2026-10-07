import { useEffect, useState } from 'react';
import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { colors } from '../theme';

/** Type a name and tap Add (or press enter); suggestions come from what other members already used. */
export default function TagInput({ label, value, onChange, suggest, placeholder, max = 20 }) {
  const [text, setText] = useState('');
  const [suggestions, setSuggestions] = useState([]);

  useEffect(() => {
    const q = text.trim();
    if (!suggest || q.length < 2) {
      setSuggestions([]);
      return;
    }
    let active = true;
    const id = setTimeout(() => {
      suggest(q)
        .then((list) => active && setSuggestions(list.filter((s) => !value.some((v) => v.toLowerCase() === s.toLowerCase()))))
        .catch(() => {});
    }, 250);
    return () => {
      active = false;
      clearTimeout(id);
    };
  }, [text, suggest, value]);

  const add = (raw) => {
    const tag = raw.trim().replace(/\s+/g, ' ');
    if (tag && !value.some((v) => v.toLowerCase() === tag.toLowerCase()) && value.length < max) onChange([...value, tag]);
    setText('');
    setSuggestions([]);
  };

  return (
    <View style={{ marginBottom: 12, gap: 6 }}>
      {label ? <Text style={styles.label}>{label}</Text> : null}
      <View style={styles.chips}>
        {value.map((tag) => (
          <Pressable key={tag} style={styles.chip} onPress={() => onChange(value.filter((t) => t !== tag))} accessibilityLabel={`Remove ${tag}`}>
            <Text style={styles.chipText}>{tag}  ×</Text>
          </Pressable>
        ))}
      </View>
      <View style={styles.row}>
        <TextInput
          style={styles.input}
          value={text}
          placeholder={placeholder}
          placeholderTextColor="#9ca3af"
          onChangeText={setText}
          onSubmitEditing={() => add(text)}
          returnKeyType="done"
          submitBehavior="submit"
          accessibilityLabel={label}
        />
        <Pressable style={styles.add} onPress={() => add(text)} accessibilityRole="button"><Text style={styles.addText}>Add</Text></Pressable>
      </View>
      {suggestions.map((s) => (
        <Pressable key={s} style={styles.suggestion} onPress={() => add(s)}>
          <Text style={{ color: colors.text }}>＋ {s}</Text>
        </Pressable>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  label: { fontSize: 14, fontWeight: '600', color: colors.text },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
  chip: { backgroundColor: colors.primarySoft, borderRadius: 999, paddingHorizontal: 12, paddingVertical: 6 },
  chipText: { color: colors.primaryDark, fontWeight: '600' },
  row: { flexDirection: 'row', gap: 8 },
  input: { flex: 1, borderWidth: 1, borderColor: '#d1d5db', borderRadius: 12, paddingHorizontal: 12, paddingVertical: 10, fontSize: 16, backgroundColor: '#fff', color: colors.text },
  add: { backgroundColor: colors.primary, borderRadius: 12, paddingHorizontal: 16, justifyContent: 'center' },
  addText: { color: '#fff', fontWeight: '700' },
  suggestion: { paddingVertical: 8, paddingHorizontal: 12, backgroundColor: '#fff', borderWidth: 1, borderColor: colors.border, borderRadius: 10 },
});
