import Constants from 'expo-constants';
import * as Device from 'expo-device';
import * as Notifications from 'expo-notifications';
import { Platform } from 'react-native';

const supported = Platform.OS !== 'web';

if (supported) {
  // Show notifications even while the app is open.
  Notifications.setNotificationHandler({
    handleNotification: async () => ({
      shouldShowBanner: true,
      shouldShowList: true,
      shouldPlaySound: true,
      shouldSetBadge: false,
    }),
  });
}

/**
 * Asks for permission and returns this phone's Expo push token, or null when notifications are not possible
 * (simulator, permission denied, or the app was not built with an EAS project id).
 */
export async function getPushToken() {
  if (!supported || !Device.isDevice) return null;
  if (Platform.OS === 'android') {
    await Notifications.setNotificationChannelAsync('default', {
      name: 'Posts and events',
      importance: Notifications.AndroidImportance.HIGH,
      vibrationPattern: [0, 250, 250, 250],
      lightColor: '#0f766e',
    });
  }
  let { status } = await Notifications.getPermissionsAsync();
  if (status !== 'granted') {
    ({ status } = await Notifications.requestPermissionsAsync());
  }
  if (status !== 'granted') return null;
  const projectId = Constants.expoConfig?.extra?.eas?.projectId ?? Constants.easConfig?.projectId;
  if (!projectId) {
    console.warn('Push notifications need an EAS project id (run `npx eas-cli@latest init`). See mobile/README.md.');
    return null;
  }
  try {
    return (await Notifications.getExpoPushTokenAsync({ projectId })).data;
  } catch (e) {
    console.warn('Could not get a push token', e);
    return null;
  }
}

/** Opens the post a notification is about, when the member taps it (also on a cold start). */
export function listenForNotificationTaps(openPost) {
  if (!supported) return () => {};
  const open = (response) => {
    const postId = response?.notification?.request?.content?.data?.postId;
    if (postId) openPost(postId);
  };
  const last = Notifications.getLastNotificationResponse();
  if (last) {
    open(last);
    Notifications.clearLastNotificationResponse();
  }
  const subscription = Notifications.addNotificationResponseReceivedListener(open);
  return () => subscription.remove();
}
