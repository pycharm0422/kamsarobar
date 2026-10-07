import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { authApi, userApi } from '../api';
import { tokenStore } from '../api/client';

const AuthContext = createContext(null);
const USER_KEY = 'kob.user';

const readCachedUser = () => {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY));
  } catch {
    return null;
  }
};

export function AuthProvider({ children }) {
  const [user, setUserState] = useState(() => (tokenStore.get() ? readCachedUser() : null));
  const [ready, setReady] = useState(!tokenStore.get());
  // True after the user clicks "Log out", so route guards send them home rather than to the login page.
  const [signedOut, setSignedOut] = useState(false);

  const setUser = useCallback((next) => {
    setUserState(next);
    if (next) localStorage.setItem(USER_KEY, JSON.stringify(next));
    else localStorage.removeItem(USER_KEY);
  }, []);

  const logout = useCallback(() => {
    tokenStore.clear();
    setSignedOut(true);
    setUser(null);
  }, [setUser]);

  // Refresh the user from the server on start-up (role may have changed, e.g. made a city admin).
  useEffect(() => {
    if (!tokenStore.get()) return;
    userApi
      .me()
      .then(setUser)
      .catch(() => logout())
      .finally(() => setReady(true));
  }, [setUser, logout]);

  useEffect(() => {
    const onLogout = () => setUser(null);
    window.addEventListener('kob:logout', onLogout);
    return () => window.removeEventListener('kob:logout', onLogout);
  }, [setUser]);

  const handleAuth = useCallback(
    (response) => {
      tokenStore.set(response.token);
      setSignedOut(false);
      setUser(response.user);
      return response.user;
    },
    [setUser],
  );

  const value = useMemo(
    () => ({
      user,
      ready,
      signedOut,
      isAuthenticated: Boolean(user),
      isMainAdmin: user?.role === 'MAIN_ADMIN',
      isCityAdmin: user?.role === 'CITY_ADMIN',
      isAdmin: user?.role === 'MAIN_ADMIN' || user?.role === 'CITY_ADMIN',
      login: (body) => authApi.login(body).then(handleAuth),
      register: (body) => authApi.register(body).then(handleAuth),
      logout,
      setUser,
    }),
    [user, ready, signedOut, handleAuth, logout, setUser],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
