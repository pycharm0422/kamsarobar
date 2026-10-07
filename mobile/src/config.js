import { Platform } from 'react-native';
import { storage } from './storage';

/**
 * Backend API base URL (ends with /api). The build-time default comes from EXPO_PUBLIC_API_URL
 * (see .env.example); members can change it on the login screen ("Server address"), so one APK
 * works with a computer on the Wi-Fi as well as with a hosted site.
 */
export const DEFAULT_API_URL =
  process.env.EXPO_PUBLIC_API_URL || (Platform.OS === 'web' ? 'http://localhost:8080/api' : 'http://10.0.2.2:8080/api');

/** The website, for features that stay on the web (the admin panel). */
export const WEB_URL = process.env.EXPO_PUBLIC_WEB_URL || 'http://localhost:3000';

const KEY = 'kob.apiUrl';
let apiUrl = DEFAULT_API_URL;

export const getApiUrl = () => apiUrl;

export async function loadApiUrl() {
  apiUrl = (await storage.get(KEY)) || DEFAULT_API_URL;
  return apiUrl;
}

/** Accepts "192.168.1.5:8080", "http://192.168.1.5:8080" or "https://site.in/api" and returns ".../api". */
export function normalizeApiUrl(input) {
  let url = String(input || '').trim().replace(/\/+$/, '');
  if (!url) return DEFAULT_API_URL;
  if (!/^https?:\/\//i.test(url)) url = `http://${url}`;
  if (!/\/api$/i.test(url)) url = `${url}/api`;
  return url;
}

/** Checks the server answers like a Kamsar o Bar backend, then remembers it. */
export async function changeApiUrl(input) {
  const url = normalizeApiUrl(input);
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 8000);
  try {
    const response = await fetch(`${url}/cities`, { signal: controller.signal });
    if (!response.ok || !Array.isArray(await response.json())) throw new Error();
  } catch {
    throw new Error(`Could not reach a Kamsar o Bar server at ${url}. Check the address and that your phone is on the same Wi-Fi.`);
  } finally {
    clearTimeout(timer);
  }
  apiUrl = url;
  await storage.set(KEY, url);
  return url;
}
