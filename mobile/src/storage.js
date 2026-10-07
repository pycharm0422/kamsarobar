import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

/** Small key-value store: the phone's encrypted storage, or localStorage in the browser preview. */
export const storage = {
  get: async (key) => (Platform.OS === 'web' ? globalThis.localStorage?.getItem(key) ?? null : SecureStore.getItemAsync(key)),
  set: async (key, value) => (Platform.OS === 'web' ? globalThis.localStorage?.setItem(key, value) : SecureStore.setItemAsync(key, value)),
  remove: async (key) => (Platform.OS === 'web' ? globalThis.localStorage?.removeItem(key) : SecureStore.deleteItemAsync(key)),
};
