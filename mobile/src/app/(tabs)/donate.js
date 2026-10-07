import { useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { Linking, Platform, ScrollView, StyleSheet, Switch, Text, View } from 'react-native';
import { cityApi, donationApi } from '../../api';
import { useAuth } from '../../auth/AuthContext';
import CityPicker from '../../components/CityPicker';
import OptionPicker from '../../components/OptionPicker';
import { Badge, Button, Card, ChoiceChip, ErrorBanner, Field, SuccessBanner } from '../../components/ui';
import { colors, common } from '../../theme';
import { errorMessage } from '../../utils/errors';
import { formatDate, formatMoney } from '../../utils/format';
import { upiLink } from '../../utils/links';

const QUICK = [51, 101, 251, 501, 1001];
const EMPTY = { amount: '', campaignId: '', transactionRef: '', note: '', anonymous: false };

/** Members see their own city's fund by default and can pick any other city. */
export default function DonateScreen() {
  const { user } = useAuth();
  const myCity = String(user.city.id);
  const [cityId, setCityId] = useState(myCity);
  const [fund, setFund] = useState(null);
  const [city, setCity] = useState(null);
  const [campaigns, setCampaigns] = useState([]);
  const [mine, setMine] = useState([]);
  const [form, setForm] = useState(EMPTY);
  const [message, setMessage] = useState({});

  const load = useCallback(() => {
    donationApi.citySummary(cityId).then(setFund).catch(() => {});
    cityApi.get(cityId).then(setCity).catch(() => {});
    donationApi.campaigns(cityId).then(setCampaigns).catch(() => setCampaigns([]));
    donationApi.mine({ size: 10 }).then((p) => setMine(p.content)).catch(() => {});
  }, [cityId]);

  useFocusEffect(useCallback(() => { load(); }, [load]));

  const submit = async () => {
    setMessage({});
    try {
      await donationApi.record({ ...form, cityId: Number(cityId), campaignId: form.campaignId ? Number(form.campaignId) : null, amount: Number(form.amount) });
      setForm(EMPTY);
      setMessage({ ok: 'Thank you! Your contribution is recorded and will count once the city admin verifies it.' });
      load();
    } catch (e) {
      setMessage({ error: errorMessage(e) });
    }
  };

  const bank = city?.bank;
  return (
    <ScrollView style={common.screen} contentContainerStyle={common.content} keyboardShouldPersistTaps="handled">
      <Card>
        <Text style={common.small}>{cityId === myCity ? 'Your city' : 'Showing'}</Text>
        <Text style={common.h1}>{fund?.cityName || city?.name || '…'}</Text>
        <Text style={common.bigNumber}>{formatMoney(fund?.collected)}</Text>
        <Text style={common.small}>
          collected from {fund?.donations ?? 0} verified contribution(s)
          {Number(fund?.pending) > 0 ? ` · ${formatMoney(fund.pending)} waiting for verification` : ''}
        </Text>
        <View style={{ marginTop: 12 }}><CityPicker label="See another city" value={cityId} onChange={setCityId} /></View>
      </Card>

      <Card>
        <Text style={common.h2}>1. Send money to the {city?.name} account</Text>
        {bank && (bank.accountNumber || bank.upiId) ? (
          <>
            {[['Account name', bank.accountName], ['Account number', bank.accountNumber], ['IFSC', bank.ifsc], ['Bank', bank.bankName], ['UPI ID', bank.upiId]]
              .filter(([, v]) => v)
              .map(([k, v]) => (
                <View key={k} style={styles.bankRow}>
                  <Text style={common.small}>{k}</Text>
                  <Text selectable style={[common.text, { fontWeight: '700', flexShrink: 1, textAlign: 'right' }]}>{v}</Text>
                </View>
              ))}
            <Text style={[common.small, { marginTop: 6 }]}>Tip: long-press a value to copy it.</Text>
            {bank.upiId && Platform.OS !== 'web' ? (
              <Button style={{ marginTop: 10 }} variant="soft" title="📱 Pay with a UPI app"
                onPress={() => Linking.openURL(upiLink({ upiId: bank.upiId, name: bank.accountName, amount: form.amount, note: 'Kamsar o Bar' })).catch(() => {})} />
            ) : null}
          </>
        ) : (
          <Text style={common.muted}>The admin of {city?.name} has not added bank details yet.</Text>
        )}
      </Card>

      <Card>
        <Text style={common.h2}>2. Tell us about your contribution</Text>
        <ErrorBanner message={message.error} />
        <SuccessBanner message={message.ok} />
        <View style={styles.chips}>
          {QUICK.map((a) => <ChoiceChip key={a} label={`₹${a}`} selected={Number(form.amount) === a} onPress={() => setForm({ ...form, amount: String(a) })} />)}
        </View>
        <Field label="Amount (₹)" keyboardType="number-pad" value={form.amount} onChangeText={(amount) => setForm({ ...form, amount })} />
        {campaigns.length ? (
          <OptionPicker label="For a cause (optional)" value={form.campaignId} onChange={(campaignId) => setForm({ ...form, campaignId })}
            options={[{ value: '', label: 'General city fund' }, ...campaigns.map((c) => ({ value: String(c.id), label: c.title }))]} />
        ) : null}
        <Field label="UPI / bank transaction reference" placeholder="Helps the admin match your payment" value={form.transactionRef}
          onChangeText={(transactionRef) => setForm({ ...form, transactionRef })} />
        <View style={[common.rowBetween, { marginBottom: 12 }]}>
          <Text style={common.text}>Show me as “Anonymous”</Text>
          <Switch value={form.anonymous} onValueChange={(anonymous) => setForm({ ...form, anonymous })} trackColor={{ true: colors.primary }} />
        </View>
        <Button title="Record contribution" onPress={submit} disabled={!form.amount} />
      </Card>

      <Card>
        <Text style={common.h2}>My contributions</Text>
        {mine.length === 0 ? <Text style={common.muted}>None yet.</Text> : mine.map((d) => (
          <View key={d.id} style={styles.bankRow}>
            <View>
              <Text style={common.text}>{formatMoney(d.amount)} · {d.city.name}</Text>
              <Text style={common.small}>{formatDate(d.createdAt)}</Text>
            </View>
            <Badge label={d.status} tone={d.status.toLowerCase()} />
          </View>
        ))}
      </Card>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  bankRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', gap: 12, paddingVertical: 8, borderBottomWidth: 1, borderBottomColor: colors.border },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginBottom: 12 },
});
