import { router, useLocalSearchParams } from 'expo-router';
import { useCallback, useEffect, useState } from 'react';
import { FlatList, KeyboardAvoidingView, Platform, StyleSheet, Text, TextInput, View } from 'react-native';
import { postApi } from '../../api';
import PostCard, { confirm } from '../../components/PostCard';
import { Button, ErrorBanner, Loading } from '../../components/ui';
import { colors, common } from '../../theme';
import { errorMessage } from '../../utils/errors';
import { formatDateTime } from '../../utils/format';

export default function PostScreen() {
  const { id } = useLocalSearchParams();
  const [post, setPost] = useState(null);
  const [comments, setComments] = useState([]);
  const [text, setText] = useState('');
  const [editing, setEditing] = useState(null);
  const [error, setError] = useState('');

  const loadComments = useCallback(() => postApi.comments(id, { size: 50 }).then((p) => setComments(p.content)), [id]);

  useEffect(() => {
    postApi.get(id).then(setPost).catch((e) => setError(errorMessage(e)));
    loadComments().catch(() => {});
  }, [id, loadComments]);

  const run = (fn) => fn().catch((e) => setError(errorMessage(e)));

  if (!post) return error ? <View style={common.content}><ErrorBanner message={error} /></View> : <Loading />;

  return (
    <KeyboardAvoidingView style={common.screen} behavior={Platform.OS === 'ios' ? 'padding' : undefined} keyboardVerticalOffset={90}>
      <FlatList
        data={comments}
        keyExtractor={(c) => String(c.id)}
        contentContainerStyle={common.content}
        ListHeaderComponent={
          <>
            <ErrorBanner message={error} onClose={() => setError('')} />
            <PostCard post={post} full onChange={setPost}
              onDelete={(pid) => run(async () => { await postApi.remove(pid); router.back(); })} />
            <Text style={common.h2}>Comments ({comments.length})</Text>
          </>
        }
        renderItem={({ item: c }) => (
          <View style={styles.comment}>
            <Text style={common.small}><Text style={{ fontWeight: '700', color: colors.text }}>{c.author?.name}</Text> · {formatDateTime(c.createdAt)}</Text>
            {editing?.id === c.id ? (
              <>
                <TextInput style={styles.input} value={editing.content} multiline onChangeText={(content) => setEditing({ ...editing, content })} />
                <View style={[common.row, { gap: 8 }]}>
                  <Button small variant="ghost" title="Cancel" onPress={() => setEditing(null)} />
                  <Button small title="Save" onPress={() => run(async () => { await postApi.updateComment(c.id, { content: editing.content }); setEditing(null); await loadComments(); })} />
                </View>
              </>
            ) : (
              <Text style={common.text}>{c.content}</Text>
            )}
            {c.canEdit && editing?.id !== c.id ? (
              <View style={[common.row, { gap: 16, marginTop: 4 }]}>
                <Text style={styles.action} onPress={() => setEditing({ id: c.id, content: c.content })}>Edit</Text>
                <Text style={[styles.action, { color: colors.danger }]}
                  onPress={() => confirm('Delete this comment?', '', () => run(async () => { await postApi.removeComment(c.id); await loadComments(); }))}>Delete</Text>
              </View>
            ) : null}
          </View>
        )}
      />
      <View style={styles.composer}>
        <TextInput style={[styles.input, { flex: 1, marginBottom: 0 }]} placeholder="Write a comment…" value={text} onChangeText={setText} multiline accessibilityLabel="Write a comment" />
        <Button title="Send" disabled={!text.trim()}
          onPress={() => run(async () => { await postApi.addComment(id, { content: text }); setText(''); await loadComments(); })} />
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  comment: { backgroundColor: '#fff', borderRadius: 12, borderWidth: 1, borderColor: colors.border, padding: 12, marginBottom: 8, gap: 4 },
  action: { color: colors.primary, fontWeight: '700' },
  input: { borderWidth: 1, borderColor: '#d1d5db', borderRadius: 12, padding: 10, fontSize: 15, backgroundColor: '#fff', marginBottom: 8, color: colors.text },
  composer: { flexDirection: 'row', gap: 8, padding: 12, borderTopWidth: 1, borderTopColor: colors.border, backgroundColor: '#fff', alignItems: 'flex-end' },
});
