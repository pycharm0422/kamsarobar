import { Linking, StyleSheet, Text, View } from 'react-native';
import { colors, common } from '../theme';
import { displayMobile } from '../utils/format';
import { Button } from './ui';

/** A member in search results, with the matching company / expertise highlighted. */
export default function MemberCard({ member, actionLabel, onAction }) {
  const matched = new Set((member.matched || []).map((m) => m.toLowerCase()));
  const tags = [...member.referralCompanies.map((t) => ['🏢', t]), ...member.expertise.map((t) => ['💡', t])];
  return (
    <View style={common.card}>
      <Text style={common.h3}>{member.name}</Text>
      <Text style={common.muted}>
        {[member.position, member.currentCompany].filter(Boolean).join(' at ') || 'Community member'}
        {member.yearsOfExperience != null ? ` · ${member.yearsOfExperience} yrs` : ''}
      </Text>
      <Text style={common.small}>📍 {member.city?.name} · {displayMobile(member.mobile)}</Text>
      {member.bio ? <Text style={[common.text, { marginTop: 6 }]}>{member.bio}</Text> : null}
      <View style={styles.tags}>
        {tags.map(([icon, t]) => (
          <Text key={icon + t} style={[styles.tag, matched.has(t.toLowerCase()) && styles.match]}>{icon} {t}</Text>
        ))}
      </View>
      <View style={[common.row, { gap: 8 }]}>
        <Button variant="whatsapp" title={actionLabel} onPress={() => onAction(member)} style={{ flex: 1 }} />
        {member.linkedinUrl ? <Button variant="ghost" title="LinkedIn" onPress={() => Linking.openURL(member.linkedinUrl)} /> : null}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  tags: { flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginVertical: 10 },
  tag: { fontSize: 13, paddingHorizontal: 9, paddingVertical: 3, borderRadius: 999, backgroundColor: colors.bg, borderWidth: 1, borderColor: colors.border, overflow: 'hidden', color: colors.text },
  match: { backgroundColor: '#fef3c7', borderColor: '#fcd34d', fontWeight: '700' },
});
