import { router } from 'expo-router';
import { Alert, Platform, Pressable, StyleSheet, Text, View } from 'react-native';
import { colors, common } from '../theme';
import { CATEGORY_LABELS, formatDateTime, wasEdited } from '../utils/format';
import EventBox from './EventBox';
import PostImages from './PostImages';
import { Badge } from './ui';

export function confirm(title, message, onYes) {
  if (Platform.OS === 'web') {
    if (globalThis.confirm?.(`${title}\n${message}`)) onYes();
    return;
  }
  Alert.alert(title, message, [{ text: 'Cancel', style: 'cancel' }, { text: 'Yes', style: 'destructive', onPress: onYes }]);
}

/** A post in the feed (or in full on its own screen). */
export default function PostCard({ post, full, onChange, onDelete }) {
  const open = () => !full && router.push(`/post/${post.id}`);
  return (
    <View style={common.card}>
      <View style={[common.row, { gap: 10, marginBottom: 6 }]}>
        <View style={styles.avatar}><Text style={styles.avatarText}>{post.author?.name?.[0]}</Text></View>
        <Pressable style={{ flex: 1 }} onPress={open}>
          <Text style={[common.text, { fontWeight: '700' }]}>{post.author?.name}</Text>
          <Text style={common.small}>
            {post.city?.name} · {formatDateTime(post.createdAt)}
            {wasEdited(post) ? ' · edited' : ''} · {post.visibility === 'EVERYONE' ? '🌐 Everyone' : `🏙 ${post.city?.name} only`}
          </Text>
        </Pressable>
        {post.category !== 'GENERAL' ? <Badge label={CATEGORY_LABELS[post.category]} tone={post.category.toLowerCase()} /> : null}
      </View>
      <Pressable onPress={open} disabled={full}>
        {post.title ? <Text style={common.h3}>{post.title}</Text> : null}
        {post.content ? <Text style={common.text} numberOfLines={full ? undefined : 5}>{post.content}</Text> : null}
      </Pressable>
      <PostImages images={post.images} />
      {post.event ? <EventBox post={post} onChange={onChange} /> : null}
      <View style={[common.rowBetween, { marginTop: 6 }]}>
        {!full ? (
          <Pressable onPress={open}><Text style={common.small}>💬 {post.commentCount} comment{post.commentCount === 1 ? '' : 's'}</Text></Pressable>
        ) : <View />}
        {post.canEdit ? (
          <View style={[common.row, { gap: 16 }]}>
            <Text style={styles.action} onPress={() => router.push({ pathname: '/post/new', params: { id: post.id } })}>Edit</Text>
            <Text style={[styles.action, { color: colors.danger }]}
              onPress={() => confirm('Delete this post?', 'This cannot be undone.', () => onDelete?.(post.id))}>Delete</Text>
          </View>
        ) : null}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  avatar: { width: 36, height: 36, borderRadius: 18, backgroundColor: colors.primarySoft, alignItems: 'center', justifyContent: 'center' },
  avatarText: { color: colors.primaryDark, fontWeight: '800' },
  action: { color: colors.primary, fontWeight: '700' },
});
