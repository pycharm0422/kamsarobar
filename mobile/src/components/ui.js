import { ActivityIndicator, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { colors, common, space } from '../theme';

const VARIANTS = {
  primary: { bg: colors.primary, fg: '#fff', border: colors.primary },
  ghost: { bg: '#fff', fg: colors.text, border: colors.border },
  soft: { bg: colors.primarySoft, fg: colors.primaryDark, border: colors.primarySoft },
  whatsapp: { bg: colors.whatsapp, fg: '#fff', border: colors.whatsapp },
  danger: { bg: colors.danger, fg: '#fff', border: colors.danger },
  going: { bg: '#dcfce7', fg: '#166534', border: '#86efac' },
};

export function Button({ title, onPress, variant = 'primary', disabled, busy, icon, style, small }) {
  const v = VARIANTS[variant];
  return (
    <Pressable
      accessibilityRole="button"
      onPress={onPress}
      disabled={disabled || busy}
      style={({ pressed }) => [
        styles.button,
        small && styles.buttonSmall,
        { backgroundColor: v.bg, borderColor: v.border, opacity: disabled ? 0.55 : pressed ? 0.85 : 1 },
        style,
      ]}
    >
      {busy ? <ActivityIndicator color={v.fg} /> : icon}
      <Text style={[styles.buttonText, small && { fontSize: 14 }, { color: v.fg }]}>{title}</Text>
    </Pressable>
  );
}

export function Field({ label, hint, error, style, multiline, ...input }) {
  return (
    <View style={[styles.field, style]}>
      {label ? <Text style={styles.label}>{label}</Text> : null}
      <TextInput
        placeholderTextColor="#9ca3af"
        style={[styles.input, multiline && styles.multiline]}
        multiline={multiline}
        textAlignVertical={multiline ? 'top' : 'center'}
        accessibilityLabel={label || input.placeholder}
        {...input}
      />
      {hint ? <Text style={common.small}>{hint}</Text> : null}
      {error ? <Text style={styles.error}>{error}</Text> : null}
    </View>
  );
}

export function Card({ children, style }) {
  return <View style={[common.card, style]}>{children}</View>;
}

export function Badge({ label, tone = 'default' }) {
  const tones = {
    default: ['#f3f4f6', colors.text],
    job_opening: ['#dbeafe', '#1e40af'],
    help_needed: ['#fee2e2', '#991b1b'],
    event: ['#ede9fe', '#5b21b6'],
    seminar: ['#e0e7ff', '#3730a3'],
    announcement: ['#fef3c7', '#92400e'],
    pending: ['#fef3c7', '#92400e'],
    verified: ['#dcfce7', '#166534'],
    rejected: ['#fee2e2', '#991b1b'],
  };
  const [bg, fg] = tones[tone] || tones.default;
  return (
    <View style={[styles.badge, { backgroundColor: bg }]}>
      <Text style={[styles.badgeText, { color: fg }]}>{label}</Text>
    </View>
  );
}

export function ErrorBanner({ message, onClose }) {
  if (!message) return null;
  return (
    <Pressable onPress={onClose} style={styles.errorBanner} accessibilityRole="alert">
      <Text style={styles.errorBannerText}>{message}</Text>
    </Pressable>
  );
}

export function SuccessBanner({ message }) {
  if (!message) return null;
  return (
    <View style={styles.successBanner}>
      <Text style={styles.successBannerText}>{message}</Text>
    </View>
  );
}

export function Loading({ label = 'Loading…' }) {
  return (
    <View style={styles.loading}>
      <ActivityIndicator color={colors.primary} size="large" />
      <Text style={common.muted}>{label}</Text>
    </View>
  );
}

export function EmptyState({ icon = '🔍', title, children }) {
  return (
    <View style={styles.empty}>
      <Text style={{ fontSize: 38 }}>{icon}</Text>
      <Text style={common.h3}>{title}</Text>
      {children ? <Text style={[common.muted, { textAlign: 'center' }]}>{children}</Text> : null}
    </View>
  );
}

/** A row of mutually exclusive options, e.g. "My events | Discover". */
export function Segmented({ options, value, onChange }) {
  return (
    <View style={styles.segmented}>
      {options.map((o) => {
        const active = o.value === value;
        return (
          <Pressable key={o.value} onPress={() => onChange(o.value)} style={[styles.segment, active && styles.segmentActive]}
            accessibilityRole="tab" accessibilityState={{ selected: active }}>
            <Text style={[styles.segmentText, active && styles.segmentTextActive]}>{o.label}</Text>
          </Pressable>
        );
      })}
    </View>
  );
}

/** Pill-shaped choice, e.g. visibility or quick amounts. */
export function ChoiceChip({ label, selected, onPress }) {
  return (
    <Pressable onPress={onPress} style={[styles.choice, selected && styles.choiceSelected]}
      accessibilityRole="radio" accessibilityState={{ checked: selected }}>
      <Text style={[styles.choiceText, selected && styles.choiceTextSelected]}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    paddingVertical: 12,
    paddingHorizontal: 18,
    borderRadius: 12,
    borderWidth: 1,
  },
  buttonSmall: { paddingVertical: 8, paddingHorizontal: 12 },
  buttonText: { fontSize: 16, fontWeight: '700' },
  field: { marginBottom: space.md, gap: 6 },
  label: { fontSize: 14, fontWeight: '600', color: colors.text },
  input: {
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 12,
    paddingHorizontal: 12,
    paddingVertical: 11,
    fontSize: 16,
    backgroundColor: '#fff',
    color: colors.text,
  },
  multiline: { minHeight: 96 },
  error: { color: colors.danger, fontSize: 13 },
  badge: { borderRadius: 999, paddingHorizontal: 9, paddingVertical: 3, alignSelf: 'flex-start' },
  badgeText: { fontSize: 12, fontWeight: '700' },
  errorBanner: { backgroundColor: '#fef2f2', borderColor: '#fecaca', borderWidth: 1, borderRadius: 10, padding: 12, marginBottom: space.md },
  errorBannerText: { color: '#991b1b', fontSize: 14 },
  successBanner: { backgroundColor: '#ecfdf5', borderColor: '#a7f3d0', borderWidth: 1, borderRadius: 10, padding: 12, marginBottom: space.md },
  successBannerText: { color: '#065f46', fontSize: 14 },
  loading: { padding: 40, alignItems: 'center', gap: 10 },
  empty: { padding: 40, alignItems: 'center', gap: 6 },
  segmented: { flexDirection: 'row', backgroundColor: '#eef0ec', borderRadius: 12, padding: 4, marginBottom: space.md },
  segment: { flex: 1, paddingVertical: 9, borderRadius: 9, alignItems: 'center' },
  segmentActive: { backgroundColor: '#fff', shadowColor: '#000', shadowOpacity: 0.06, shadowRadius: 4, elevation: 1 },
  segmentText: { fontWeight: '600', color: colors.muted },
  segmentTextActive: { color: colors.primary },
  choice: { borderWidth: 1, borderColor: colors.border, borderRadius: 999, paddingVertical: 8, paddingHorizontal: 14, backgroundColor: '#fff' },
  choiceSelected: { borderColor: colors.primary, backgroundColor: colors.primarySoft },
  choiceText: { color: colors.text, fontSize: 14 },
  choiceTextSelected: { color: colors.primaryDark, fontWeight: '700' },
});
