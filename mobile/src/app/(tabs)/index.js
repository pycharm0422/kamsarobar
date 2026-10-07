import { router, useFocusEffect } from 'expo-router';
import { useCallback, useRef, useState } from 'react';
import { FlatList, Pressable, RefreshControl, StyleSheet, Text, View } from 'react-native';
import { postApi } from '../../api';
import { useAuth } from '../../auth/AuthContext';
import CityPicker from '../../components/CityPicker';
import OptionPicker from '../../components/OptionPicker';
import PostCard from '../../components/PostCard';
import { EmptyState, ErrorBanner, Loading } from '../../components/ui';
import { colors, common } from '../../theme';
import { errorMessage } from '../../utils/errors';
import { CATEGORY_LABELS } from '../../utils/format';

const CATEGORIES = [{ value: '', label: 'All posts' }, ...Object.entries(CATEGORY_LABELS).map(([value, label]) => ({ value, label }))];

export default function PostsScreen() {
  const { user } = useAuth();
  const [cityId, setCityId] = useState(String(user.city.id));
  const [category, setCategory] = useState('');
  const [posts, setPosts] = useState(null);
  const [page, setPage] = useState(null);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');
  const loadingMore = useRef(false);

  const load = useCallback(
    async (pageNo = 0) => {
      try {
        const result = await postApi.feed({ cityId: cityId || undefined, category: category || undefined, page: pageNo });
        setPage(result);
        setPosts((prev) => (pageNo === 0 ? result.content : [...(prev || []), ...result.content]));
        setError('');
      } catch (e) {
        setError(errorMessage(e));
        if (pageNo === 0) setPosts([]);
      }
    },
    [cityId, category],
  );

  // Reload whenever the tab comes back into view (e.g. after writing a post).
  useFocusEffect(useCallback(() => { load(0); }, [load]));

  const replace = (updated) => setPosts((list) => list.map((p) => (p.id === updated.id ? updated : p)));
  const remove = async (id) => {
    try {
      await postApi.remove(id);
      setPosts((list) => list.filter((p) => p.id !== id));
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  const otherCity = cityId && cityId !== String(user.city.id);

  return (
    <View style={common.screen}>
      <View style={styles.filters}>
        <View style={{ flex: 1.3 }}><CityPicker value={cityId} onChange={setCityId} allLabel="All cities" compact /></View>
        <View style={{ flex: 1 }}><OptionPicker value={category} options={CATEGORIES} onChange={setCategory} compact label="" placeholder="All posts" /></View>
      </View>
      {posts === null ? (
        <Loading />
      ) : (
        <FlatList
          data={posts}
          keyExtractor={(p) => String(p.id)}
          contentContainerStyle={common.content}
          renderItem={({ item }) => <PostCard post={item} onChange={replace} onDelete={remove} />}
          refreshControl={<RefreshControl refreshing={refreshing} colors={[colors.primary]}
            onRefresh={async () => { setRefreshing(true); await load(0); setRefreshing(false); }} />}
          onEndReachedThreshold={0.4}
          onEndReached={async () => {
            if (page && !page.last && !loadingMore.current) {
              loadingMore.current = true;
              await load(page.page + 1);
              loadingMore.current = false;
            }
          }}
          ListHeaderComponent={
            <>
              <ErrorBanner message={error} onClose={() => setError('')} />
              <Pressable style={styles.composer} onPress={() => router.push('/post/new')}>
                <Text style={common.muted}>What's on your mind? Share news, photos, a job, a seminar…</Text>
              </Pressable>
              {otherCity ? (
                <Text style={styles.note}>Showing posts from this city that are shared with everyone. City-only posts are not shown.</Text>
              ) : null}
            </>
          }
          ListEmptyComponent={<EmptyState icon="📝" title="No posts yet">Be the first to share something!</EmptyState>}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  filters: { flexDirection: 'row', gap: 8, paddingHorizontal: 16, paddingTop: 12, paddingBottom: 4 },
  composer: { backgroundColor: '#fff', borderRadius: 14, borderWidth: 1, borderColor: colors.border, padding: 16, marginBottom: 12 },
  note: { backgroundColor: '#fffbeb', borderColor: '#fde68a', borderWidth: 1, borderRadius: 10, padding: 10, marginBottom: 12, color: colors.muted },
});
