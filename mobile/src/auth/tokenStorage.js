import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

const KEY = 'kob.token';
let current = null;

/**
 * Keeps the login token in the phone's secure, encrypted storage (Keychain / Keystore).
 * The browser preview has no secure store, so it falls back to localStorage there.
 */
export const tokenStorage = {
  /** Synchronous access for the HTTP client; call load() once at start-up. */
  current: () => current,

  async load() {
    current = Platform.OS === 'web' ? globalThis.localStorage?.getItem(KEY) ?? null : await SecureStore.getItemAsync(KEY);
    return current;
  },

  async save(token) {
    current = token;
    if (Platform.OS === 'web') globalThis.localStorage?.setItem(KEY, token);
    else await SecureStore.setItemAsync(KEY, token);
  },

  async clear() {
    current = null;
    if (Platform.OS === 'web') globalThis.localStorage?.removeItem(KEY);
    else await SecureStore.deleteItemAsync(KEY);
  },
};
