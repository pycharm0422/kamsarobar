import { useEffect, useState } from 'react';
import { FlatList, Text, View } from 'react-native';
import { directoryApi, profileApi } from '../../api';
import { useAuth } from '../../auth/AuthContext';
import CityPicker from '../../components/CityPicker';
import MemberCard from '../../components/MemberCard';
import { Button, EmptyState, ErrorBanner, Field, Loading, Segmented } from '../../components/ui';
import WhatsAppComposer from '../../components/WhatsAppComposer';
import { common } from '../../theme';
import { errorMessage } from '../../utils/errors';
import { adviceMessage, referralMessage } from '../../utils/links';

/**
 * Referral search and expert search are two configurations of one screen - a new kind of search
 * only needs a new entry here.
 */
const MODES = {
  referral: {
    label: 'Company name',
    placeholder: 'e.g. Google, TCS, Amazon',
    intro: 'Saw an opening? Find members who can refer you to that company.',
    search: (q, cityId, page) => directoryApi.referrers({ company: q, cityId, page }),
    suggest: profileApi.suggestCompanies,
    action: 'Ask for referral',
    empty: 'No member has listed this company yet. Try a shorter name or another city.',
    message: (args) => referralMessage({ ...args, company: args.term }),
  },
  expert: {
    label: 'Expertise',
    placeholder: 'e.g. Java, UPSC, Startups, Medicine',
    intro: 'Need guidance? Find members who know the topic well.',
    search: (q, cityId, page) => directoryApi.experts({ expertise: q, cityId, page }),
    suggest: profileApi.suggestExpertise,
    action: 'Ask for advice',
    empty: 'No member has listed this expertise yet. Try a related word.',
    message: (args) => adviceMessage({ ...args, topic: args.term }),
  },
};

export default function FindScreen() {
  const { user } = useAuth();
  const [mode, setMode] = useState('referral');
  const [term, setTerm] = useState('');
  const [role, setRole] = useState('');
  const [cityId, setCityId] = useState('');
  const [suggestions, setSuggestions] = useState([]);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [selected, setSelected] = useState(null);
  const [myProfile, setMyProfile] = useState(null);
  const config = MODES[mode];

  useEffect(() => {
    profileApi.mine().then(setMyProfile).catch(() => {});
  }, []);

  useEffect(() => {
    setResult(null);
    setSuggestions([]);
  }, [mode]);

  useEffect(() => {
    const q = term.trim();
    if (q.length < 2) {
      setSuggestions([]);
      return undefined;
    }
    const id = setTimeout(() => config.suggest(q).then((s) => setSuggestions(s.filter((x) => x.toLowerCase() !== q.toLowerCase()))).catch(() => {}), 250);
    return () => clearTimeout(id);
  }, [term, config]);

  const search = async (q = term) => {
    if (q.trim().length < 2) {
      setError('Type at least 2 characters.');
      return;
    }
    setSuggestions([]);
    setLoading(true);
    setError('');
    try {
      setResult(await config.search(q.trim(), cityId || undefined, 0));
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={common.screen}>
      <FlatList
        data={result?.content || []}
        keyExtractor={(m) => String(m.userId)}
        contentContainerStyle={common.content}
        keyboardShouldPersistTaps="handled"
        ListHeaderComponent={
          <>
            <Segmented value={mode} onChange={setMode} options={[{ value: 'referral', label: 'Referral' }, { value: 'expert', label: 'Expert advice' }]} />
            <Text style={[common.muted, { marginBottom: 10 }]}>{config.intro}</Text>
            <Field label={config.label} placeholder={config.placeholder} value={term} onChangeText={setTerm} onSubmitEditing={() => search()} returnKeyType="search" />
            {suggestions.slice(0, 5).map((s) => (
              <Text key={s} style={{ padding: 8, color: common.text.color }} onPress={() => { setTerm(s); search(s); }}>🔎 {s}</Text>
            ))}
            {mode === 'referral' ? <Field label="Job role (optional)" placeholder="e.g. Data Analyst" value={role} onChangeText={setRole} /> : null}
            <CityPicker label="City" value={cityId} onChange={setCityId} allLabel="All cities" />
            <Button title="Search" onPress={() => search()} busy={loading} />
            <ErrorBanner message={error} onClose={() => setError('')} />
            {loading ? <Loading label="Searching the community…" /> : null}
            {result && !loading ? <Text style={[common.muted, { marginVertical: 12 }]}>{result.totalElements} member(s) found</Text> : null}
          </>
        }
        renderItem={({ item }) => <MemberCard member={item} actionLabel={config.action} onAction={setSelected} />}
        ListEmptyComponent={result && !loading ? <EmptyState icon={mode === 'referral' ? '🏢' : '💡'} title="No one found yet">{config.empty}</EmptyState> : null}
      />
      {selected ? (
        <WhatsAppComposer
          member={selected}
          initialMessage={config.message({ member: selected, me: user, myProfile, term: term.trim(), role })}
          onClose={() => setSelected(null)}
        />
      ) : null}
    </View>
  );
}
