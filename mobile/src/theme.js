import { StyleSheet } from 'react-native';

/** Same palette as the website so both feel like one product. */
export const colors = {
  primary: '#0f766e',
  primaryDark: '#115e59',
  primarySoft: '#ccfbf1',
  accent: '#f59e0b',
  whatsapp: '#25d366',
  danger: '#dc2626',
  text: '#1f2937',
  muted: '#6b7280',
  border: '#e5e7eb',
  bg: '#f6f7f4',
  card: '#ffffff',
  eventBg: '#f5f3ff',
  eventBorder: '#ede9fe',
  violet: '#6d28d9',
};

export const space = { xs: 4, sm: 8, md: 12, lg: 16, xl: 24 };

export const common = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.bg },
  content: { padding: space.lg, paddingBottom: 40 },
  card: {
    backgroundColor: colors.card,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: colors.border,
    padding: space.lg,
    marginBottom: space.md,
  },
  h1: { fontSize: 24, fontWeight: '800', color: colors.text, marginBottom: 4 },
  h2: { fontSize: 18, fontWeight: '700', color: colors.text, marginBottom: 8 },
  h3: { fontSize: 16, fontWeight: '700', color: colors.text, marginBottom: 4 },
  text: { fontSize: 15, color: colors.text, lineHeight: 21 },
  muted: { fontSize: 14, color: colors.muted },
  small: { fontSize: 13, color: colors.muted },
  row: { flexDirection: 'row', alignItems: 'center' },
  rowBetween: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  bigNumber: { fontSize: 30, fontWeight: '800', color: colors.primary },
});
