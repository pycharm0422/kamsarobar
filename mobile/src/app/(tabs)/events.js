import { useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { FlatList, View } from 'react-native';
import { eventApi } from '../../api';
import { useAuth } from '../../auth/AuthContext';
import CityPicker from '../../components/CityPicker';
import PostCard from '../../components/PostCard';
import { EmptyState, ErrorBanner, Loading, Segmented } from '../../components/ui';
import { common } from '../../theme';
import { errorMessage } from '../../utils/errors';

export default function EventsScreen() {
  const { user } = useAuth();
  const [tab, setTab] = useState('mine');
  const [cityId, setCityId] = useState(String(user.city.id));
  const [items, setItems] = useState(null);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    try {
      const page = tab === 'mine' ? await eventApi.mine({ size: 50 }) : await eventApi.upcoming({ cityId: cityId || undefined, size: 50 });
      setItems(page.content);
      setError('');
    } catch (e) {
      setError(errorMessage(e));
      setItems([]);
    }
  }, [tab, cityId]);

  useFocusEffect(useCallback(() => { setItems(null); load(); }, [load]));

  const onChange = (updated) =>
    setItems((list) =>
      tab === 'mine' && !updated.event.attending ? list.filter((p) => p.id !== updated.id) : list.map((p) => (p.id === updated.id ? updated : p)),
    );

  return (
    <View style={common.screen}>
      <FlatList
        data={items || []}
        keyExtractor={(p) => String(p.id)}
        contentContainerStyle={common.content}
        ListHeaderComponent={
          <>
            <Segmented value={tab} onChange={setTab} options={[{ value: 'mine', label: 'My upcoming events' }, { value: 'all', label: 'Discover' }]} />
            {tab === 'all' ? <CityPicker value={cityId} onChange={setCityId} allLabel="All cities" /> : null}
            <ErrorBanner message={error} onClose={() => setError('')} />
            {items === null ? <Loading /> : null}
          </>
        }
        renderItem={({ item }) => <PostCard post={item} onChange={onChange} />}
        ListEmptyComponent={
          items === null ? null : (
            <EmptyState icon="🗓" title={tab === 'mine' ? 'No upcoming events yet' : 'No upcoming events here'}>
              {tab === 'mine'
                ? 'Open Discover and tap “Add to my events” on any seminar or event. You will get a reminder before it starts.'
                : 'Organising something? Post it as a Seminar or Event from the Posts tab.'}
            </EmptyState>
          )
        }
      />
    </View>
  );
}
