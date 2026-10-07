import { router, Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { AuthProvider, useAuth } from '../auth/AuthContext';
import { listenForNotificationTaps } from '../notifications/push';
import { colors } from '../theme';

SplashScreen.preventAutoHideAsync().catch(() => {});

export default function RootLayout() {
  return (
    <SafeAreaProvider>
      <AuthProvider>
        <StatusBar style="dark" />
        <RootStack />
      </AuthProvider>
    </SafeAreaProvider>
  );
}

function RootStack() {
  const { user, ready } = useAuth();

  useEffect(() => {
    if (ready) SplashScreen.hideAsync().catch(() => {});
  }, [ready]);

  // Tapping a notification opens the post it is about.
  useEffect(() => {
    if (!user) return undefined;
    return listenForNotificationTaps((postId) => router.push(`/post/${postId}`));
  }, [user]);

  if (!ready) return null;

  return (
    <Stack
      screenOptions={{
        headerTintColor: colors.primary,
        headerTitleStyle: { color: colors.text, fontWeight: '700' },
        contentStyle: { backgroundColor: colors.bg },
      }}
    >
      <Stack.Protected guard={!!user}>
        <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
        <Stack.Screen name="post/[id]" options={{ title: 'Post' }} />
        <Stack.Screen name="post/new" options={{ title: 'New post', presentation: 'modal' }} />
        <Stack.Screen name="profile/edit" options={{ title: 'My profile' }} />
        <Stack.Screen name="notifications" options={{ title: 'Notifications' }} />
      </Stack.Protected>
      <Stack.Protected guard={!user}>
        <Stack.Screen name="login" options={{ headerShown: false }} />
        <Stack.Screen name="register" options={{ title: 'Join Kamsar o Bar' }} />
      </Stack.Protected>
    </Stack>
  );
}
