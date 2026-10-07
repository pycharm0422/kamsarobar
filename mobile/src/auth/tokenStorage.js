import { storage } from '../storage';

const KEY = 'kob.token';
let current = null;

/** The login token, kept in the phone's secure, encrypted storage (Keychain / Keystore). */
export const tokenStorage = {
  /** Synchronous access for the HTTP client; call load() once at start-up. */
  current: () => current,

  async load() {
    current = await storage.get(KEY);
    return current;
  },

  async save(token) {
    current = token;
    await storage.set(KEY, token);
  },

  async clear() {
    current = null;
    await storage.remove(KEY);
  },
};
