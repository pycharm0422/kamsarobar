import Ionicons from '@expo/vector-icons/Ionicons';
import { router, Tabs } from 'expo-router';
import { Pressable } from 'react-native';
import { colors } from '../../theme';

const icon = (name) => ({ color, size }) => <Ionicons name={name} size={size} color={color} />;

export default function TabsLayout() {
  return (
    <Tabs
      screenOptions={{
        tabBarActiveTintColor: colors.primary,
        tabBarInactiveTintColor: colors.muted,
        headerTitleStyle: { fontWeight: '800', color: colors.text },
        sceneStyle: { backgroundColor: colors.bg },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: 'Posts',
          tabBarIcon: icon('newspaper-outline'),
          headerRight: () => (
            <Pressable onPress={() => router.push('/post/new')} style={{ paddingHorizontal: 16 }} accessibilityLabel="New post">
              <Ionicons name="add-circle" size={30} color={colors.primary} />
            </Pressable>
          ),
        }}
      />
      <Tabs.Screen name="events" options={{ title: 'Events', tabBarIcon: icon('calendar-outline') }} />
      <Tabs.Screen name="find" options={{ title: 'Find', tabBarIcon: icon('search-outline') }} />
      <Tabs.Screen name="donate" options={{ title: 'Donate', tabBarIcon: icon('heart-outline') }} />
      <Tabs.Screen name="me" options={{ title: 'Me', tabBarIcon: icon('person-circle-outline') }} />
    </Tabs>
  );
}
