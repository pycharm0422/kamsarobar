import { useEffect, useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, Switch, Text, View } from 'react-native';
import { profileApi, userApi } from '../../api';
import { useAuth } from '../../auth/AuthContext';
import CityPicker from '../../components/CityPicker';
import TagInput from '../../components/TagInput';
import { Button, Card, ErrorBanner, Field, Loading, Segmented, SuccessBanner } from '../../components/ui';
import { colors, common } from '../../theme';
import { errorMessage } from '../../utils/errors';

export default function EditProfileScreen() {
  const [tab, setTab] = useState('work');
  return (
    <KeyboardAvoidingView style={common.screen} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={common.content} keyboardShouldPersistTaps="handled">
        <Segmented value={tab} onChange={setTab}
          options={[{ value: 'work', label: 'Work & referrals' }, { value: 'basic', label: 'Basic' }, { value: 'password', label: 'Password' }]} />
        {tab === 'work' ? <WorkForm /> : tab === 'basic' ? <BasicForm /> : <PasswordForm />}
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

/** Form 2 - the professional profile. */
function WorkForm() {
  const [form, setForm] = useState(null);
  const [msg, setMsg] = useState({});
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    profileApi.mine().then((p) => setForm({
      linkedinUrl: p.linkedinUrl || '', currentCompany: p.currentCompany || '', position: p.position || '',
      yearsOfExperience: p.yearsOfExperience == null ? '' : String(p.yearsOfExperience), bio: p.bio || '',
      openToHelp: p.openToHelp, referralCompanies: p.referralCompanies, expertise: p.expertise,
    })).catch((e) => setMsg({ error: errorMessage(e) }));
  }, []);

  if (!form) return msg.error ? <ErrorBanner message={msg.error} /> : <Loading />;
  const set = (key) => (value) => setForm((f) => ({ ...f, [key]: value }));

  const save = async () => {
    setBusy(true);
    setMsg({});
    try {
      await profileApi.save({ ...form, yearsOfExperience: form.yearsOfExperience === '' ? null : Number(form.yearsOfExperience) });
      setMsg({ ok: 'Saved. Members can now find you in search.' });
    } catch (e) {
      setMsg({ error: errorMessage(e) });
    } finally {
      setBusy(false);
    }
  };

  return (
    <Card>
      <ErrorBanner message={msg.error} />
      <SuccessBanner message={msg.ok} />
      <Field label="LinkedIn profile URL" value={form.linkedinUrl} onChangeText={set('linkedinUrl')} autoCapitalize="none" keyboardType="url"
        placeholder="https://www.linkedin.com/in/your-name" />
      <Field label="Current company" value={form.currentCompany} onChangeText={set('currentCompany')} />
      <Field label="Position" value={form.position} onChangeText={set('position')} />
      <Field label="Experience (years)" value={form.yearsOfExperience} onChangeText={set('yearsOfExperience')} keyboardType="number-pad" />
      <TagInput label="Companies where you can give a referral" value={form.referralCompanies} onChange={set('referralCompanies')}
        suggest={profileApi.suggestCompanies} placeholder="Type a company" />
      <TagInput label="Your expertise" value={form.expertise} onChange={set('expertise')} max={30}
        suggest={profileApi.suggestExpertise} placeholder="e.g. Java, UPSC, Real estate" />
      <Field label="Short bio" value={form.bio} onChangeText={set('bio')} multiline />
      <View style={[common.rowBetween, { marginBottom: 14 }]}>
        <Text style={[common.text, { flex: 1 }]}>Show me in referral & expert search</Text>
        <Switch value={form.openToHelp} onValueChange={set('openToHelp')} trackColor={{ true: colors.primary }} />
      </View>
      <Button title="Save profile" onPress={save} busy={busy} />
    </Card>
  );
}

function BasicForm() {
  const { user, setUser } = useAuth();
  const [form, setForm] = useState({ name: user.name, mobile: `+${user.mobile}`, cityId: String(user.city.id) });
  const [msg, setMsg] = useState({});
  const save = async () => {
    setMsg({});
    try {
      setUser(await userApi.update({ ...form, cityId: Number(form.cityId) }));
      setMsg({ ok: 'Saved.' });
    } catch (e) {
      setMsg({ error: errorMessage(e) });
    }
  };
  return (
    <Card>
      <ErrorBanner message={msg.error} />
      <SuccessBanner message={msg.ok} />
      <Field label="Full name" value={form.name} onChangeText={(name) => setForm({ ...form, name })} />
      <Field label="Mobile number (WhatsApp)" value={form.mobile} keyboardType="phone-pad" onChangeText={(mobile) => setForm({ ...form, mobile })} />
      <CityPicker label="City" value={form.cityId} onChange={(cityId) => setForm({ ...form, cityId })} />
      <Button title="Save" onPress={save} />
    </Card>
  );
}

function PasswordForm() {
  const [form, setForm] = useState({ currentPassword: '', newPassword: '' });
  const [msg, setMsg] = useState({});
  const save = async () => {
    setMsg({});
    try {
      await userApi.changePassword(form);
      setForm({ currentPassword: '', newPassword: '' });
      setMsg({ ok: 'Password changed.' });
    } catch (e) {
      setMsg({ error: errorMessage(e) });
    }
  };
  return (
    <Card>
      <ErrorBanner message={msg.error} />
      <SuccessBanner message={msg.ok} />
      <Field label="Current password" secureTextEntry value={form.currentPassword} onChangeText={(currentPassword) => setForm({ ...form, currentPassword })} />
      <Field label="New password" secureTextEntry value={form.newPassword} onChangeText={(newPassword) => setForm({ ...form, newPassword })} />
      <Button title="Change password" onPress={save} />
    </Card>
  );
}
