import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { Platform } from 'react-native';
import { authApi, notificationApi, userApi } from '../api';
import { setUnauthorizedHandler } from '../api/client';
import { loadApiUrl } from '../config';
import { getPushToken } from '../notifications/push';
import { tokenStorage } from './tokenStorage';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [ready, setReady] = useState(false);
  const pushToken = useRef(null);

  const signOutLocally = useCallback(async () => {
    await tokenStorage.clear();
    setUser(null);
  }, []);

  /** Registers this phone for push notifications (best effort; the app works without it). */
  const registerPush = useCallback(async () => {
    const token = await getPushToken();
    if (token) {
      pushToken.current = token;
      notificationApi.registerDevice(token, Platform.OS).catch(() => {});
    }
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(signOutLocally); // expired or blocked: back to the login screen
    (async () => {
      try {
        await loadApiUrl();
        if (await tokenStorage.load()) {
          setUser(await userApi.me());
          registerPush();
        }
      } catch {
        await signOutLocally();
      } finally {
        setReady(true);
      }
    })();
  }, [signOutLocally, registerPush]);

  const handleAuth = useCallback(
    async (response) => {
      await tokenStorage.save(response.token);
      setUser(response.user);
      registerPush();
      return response.user;
    },
    [registerPush],
  );

  const logout = useCallback(async () => {
    if (pushToken.current) {
      await notificationApi.unregisterDevice(pushToken.current).catch(() => {});
    }
    await signOutLocally();
  }, [signOutLocally]);

  const value = useMemo(
    () => ({
      user,
      ready,
      isAdmin: user?.role === 'MAIN_ADMIN' || user?.role === 'CITY_ADMIN',
      login: (body) => authApi.login(body).then(handleAuth),
      register: (body) => authApi.register(body).then(handleAuth),
      logout,
      setUser,
    }),
    [user, ready, handleAuth, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
