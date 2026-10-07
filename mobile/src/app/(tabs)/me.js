import { router, useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { Linking, ScrollView, Text, View } from 'react-native';
import { profileApi } from '../../api';
import { useAuth } from '../../auth/AuthContext';
import { Button, Card } from '../../components/ui';
import { WEB_URL } from '../../config';
import { colors, common } from '../../theme';
import { displayMobile } from '../../utils/format';

export default function MeScreen() {
  const { user, isAdmin, logout } = useAuth();
  const [profile, setProfile] = useState(null);

  useFocusEffect(useCallback(() => { profileApi.mine().then(setProfile).catch(() => {}); }, []));

  return (
    <ScrollView style={common.screen} contentContainerStyle={common.content}>
      <Card>
        <Text style={common.h1}>{user.name}</Text>
        <Text style={common.muted}>📍 {user.city.name} · {displayMobile(user.mobile)}</Text>
        {user.role !== 'MEMBER' ? (
          <Text style={[common.small, { color: colors.primary, marginTop: 4 }]}>
            {user.role === 'MAIN_ADMIN' ? 'Main admin' : `City admin of ${user.managedCity?.name}`}
          </Text>
        ) : null}
        {profile && !profile.completed ? (
          <View style={{ backgroundColor: '#fef3c7', borderRadius: 10, padding: 12, marginTop: 12 }}>
            <Text style={common.text}>Add your work details so members can find you for referrals and advice.</Text>
          </View>
        ) : null}
        {profile?.completed ? (
          <Text style={[common.text, { marginTop: 10 }]}>
            {[profile.position, profile.currentCompany].filter(Boolean).join(' at ')}
          </Text>
        ) : null}
      </Card>
      <View style={{ gap: 10 }}>
        <Button title={profile && !profile.completed ? 'Complete my profile' : 'Edit my profile'} onPress={() => router.push('/profile/edit')} />
        <Button variant="ghost" title="🔔 Notification settings" onPress={() => router.push('/notifications')} />
        {isAdmin ? (
          <Button variant="ghost" title="🛠 Open admin panel (website)" onPress={() => Linking.openURL(`${WEB_URL}/admin`)} />
        ) : null}
        <Button variant="ghost" title="Log out" onPress={logout} />
      </View>
    </ScrollView>
  );
}
