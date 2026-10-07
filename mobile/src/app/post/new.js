import { Image } from 'expo-image';
import * as ImagePicker from 'expo-image-picker';
import { router, useLocalSearchParams } from 'expo-router';
import { useEffect, useState } from 'react';
import { KeyboardAvoidingView, Platform, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { imageUrl, postApi, uploadImage } from '../../api';
import { useAuth } from '../../auth/AuthContext';
import DateTimeField from '../../components/DateTimeField';
import OptionPicker from '../../components/OptionPicker';
import { Button, ChoiceChip, ErrorBanner, Field, Loading } from '../../components/ui';
import { colors, common } from '../../theme';
import { errorMessage } from '../../utils/errors';
import { CATEGORY_LABELS, isEventCategory } from '../../utils/format';
import { prepareImage } from '../../utils/imageResize';

const MAX_PHOTOS = 6;
const CATEGORIES = Object.entries(CATEGORY_LABELS).map(([value, label]) => ({ value, label }));

/** Write or edit a post: text, photos or both; seminars and events also get a date and venue. */
export default function PostComposer() {
  const { id } = useLocalSearchParams();
  const { user } = useAuth();
  const [form, setForm] = useState({ category: 'GENERAL', visibility: 'CITY', title: '', content: '', eventLocation: '', eventLink: '' });
  const [startsAt, setStartsAt] = useState(null);
  const [endsAt, setEndsAt] = useState(null);
  const [images, setImages] = useState([]);
  const [uploading, setUploading] = useState(0);
  const [busy, setBusy] = useState(false);
  const [loaded, setLoaded] = useState(!id);
  const [error, setError] = useState('');
  const isEvent = isEventCategory(form.category);
  const set = (key) => (value) => setForm((f) => ({ ...f, [key]: value }));

  useEffect(() => {
    if (!id) return;
    postApi.get(id).then((p) => {
      setForm({ category: p.category, visibility: p.visibility, title: p.title || '', content: p.content || '',
        eventLocation: p.event?.location || '', eventLink: p.event?.link || '' });
      setStartsAt(p.event ? new Date(p.event.startsAt) : null);
      setEndsAt(p.event?.endsAt ? new Date(p.event.endsAt) : null);
      setImages(p.images);
      setLoaded(true);
    }).catch((e) => setError(errorMessage(e)));
  }, [id]);

  const addPhotos = async () => {
    setError('');
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ['images'],
      allowsMultipleSelection: true,
      selectionLimit: MAX_PHOTOS - images.length,
      quality: 1, // we resize ourselves, only when needed
    });
    if (result.canceled) return;
    for (const asset of result.assets.slice(0, MAX_PHOTOS - images.length)) {
      setUploading((n) => n + 1);
      try {
        const uploaded = await uploadImage(await prepareImage(asset));
        setImages((list) => [...list, uploaded]);
      } catch (e) {
        setError(errorMessage(e));
      } finally {
        setUploading((n) => n - 1);
      }
    }
  };

  const submit = async () => {
    if (!isEvent && !form.content.trim() && images.length === 0) {
      setError('Write something or add a photo.');
      return;
    }
    setBusy(true);
    setError('');
    const payload = {
      ...form,
      imageIds: images.map((i) => i.id),
      eventStartsAt: isEvent && startsAt ? startsAt.toISOString() : null,
      eventEndsAt: isEvent && endsAt ? endsAt.toISOString() : null,
    };
    try {
      if (id) await postApi.update(id, payload);
      else await postApi.create(payload);
      router.back();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  if (!loaded) return error ? <ErrorBanner message={error} /> : <Loading />;

  return (
    <KeyboardAvoidingView style={common.screen} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={common.content} keyboardShouldPersistTaps="handled">
        <ErrorBanner message={error} onClose={() => setError('')} />
        <OptionPicker label="What are you posting?" value={form.category} options={CATEGORIES} onChange={set('category')} />
        <Field label={isEvent ? 'Title' : 'Title (optional)'} value={form.title} onChangeText={set('title')}
          placeholder={isEvent ? 'e.g. Career guidance seminar' : 'Add a heading'} />
        {isEvent ? (
          <View style={styles.eventFields}>
            <DateTimeField label="Starts" value={startsAt} onChange={setStartsAt} />
            <DateTimeField label="Ends (optional)" value={endsAt} onChange={setEndsAt} optional />
            <Field label="Venue" value={form.eventLocation} onChangeText={set('eventLocation')} placeholder="Address or hall name" />
            <Field label="Online link (optional)" value={form.eventLink} onChangeText={set('eventLink')} placeholder="https://meet.google.com/…"
              autoCapitalize="none" keyboardType="url" />
          </View>
        ) : null}
        <Field label="Text" multiline value={form.content} onChangeText={set('content')}
          placeholder={isEvent ? 'What is it about? Who should come?' : "What's on your mind? Share anything…"} />

        <Text style={styles.label}>Photos</Text>
        <View style={styles.thumbs}>
          {images.map((img) => (
            <View key={img.id} style={styles.thumb}>
              <Image source={imageUrl(img)} style={StyleSheet.absoluteFill} contentFit="cover" />
              <Pressable style={styles.remove} onPress={() => setImages((l) => l.filter((i) => i.id !== img.id))} accessibilityLabel="Remove photo">
                <Text style={{ color: '#fff', fontWeight: '800' }}>×</Text>
              </Pressable>
            </View>
          ))}
          {Array.from({ length: uploading }).map((_, i) => (
            <View key={`up${i}`} style={[styles.thumb, styles.pending]}><Text style={common.small}>Uploading…</Text></View>
          ))}
          {images.length + uploading < MAX_PHOTOS ? (
            <Pressable style={[styles.thumb, styles.addPhoto]} onPress={addPhotos} accessibilityRole="button" accessibilityLabel="Add photos">
              <Text style={{ fontSize: 26 }}>📷</Text>
              <Text style={common.small}>Add</Text>
            </Pressable>
          ) : null}
        </View>
        <Text style={[common.small, { marginBottom: 14 }]}>Large photos are shrunk to under 3 MB on your phone, keeping their shape.</Text>

        <Text style={styles.label}>Who can see this?</Text>
        <View style={[common.row, { gap: 8, flexWrap: 'wrap', marginVertical: 8 }]}>
          <ChoiceChip label={`🏙 Only ${user.city.name} members`} selected={form.visibility === 'CITY'} onPress={() => set('visibility')('CITY')} />
          <ChoiceChip label="🌐 Everyone, all cities" selected={form.visibility === 'EVERYONE'} onPress={() => set('visibility')('EVERYONE')} />
        </View>

        <Button style={{ marginTop: 12 }} title={uploading ? 'Uploading photos…' : id ? 'Save' : 'Post'} onPress={submit} busy={busy} disabled={uploading > 0} />
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  label: { fontSize: 14, fontWeight: '600', color: colors.text },
  eventFields: { backgroundColor: colors.eventBg, borderColor: colors.eventBorder, borderWidth: 1, borderRadius: 12, padding: 12, marginBottom: 12 },
  thumbs: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginVertical: 8 },
  thumb: { width: 84, height: 84, borderRadius: 10, overflow: 'hidden', backgroundColor: '#eef0ec' },
  pending: { alignItems: 'center', justifyContent: 'center' },
  addPhoto: { borderWidth: 1, borderStyle: 'dashed', borderColor: '#9ca3af', alignItems: 'center', justifyContent: 'center' },
  remove: { position: 'absolute', top: 4, right: 4, width: 22, height: 22, borderRadius: 11, backgroundColor: 'rgba(0,0,0,0.65)', alignItems: 'center', justifyContent: 'center' },
});
