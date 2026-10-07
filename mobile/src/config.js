import { Platform } from 'react-native';

/** Backend API base URL (ends with /api). Set EXPO_PUBLIC_API_URL in mobile/.env - see .env.example. */
export const API_URL =
  process.env.EXPO_PUBLIC_API_URL || (Platform.OS === 'web' ? 'http://localhost:8080/api' : 'http://10.0.2.2:8080/api');

/** The website, for features that stay on the web (the admin panel). */
export const WEB_URL = process.env.EXPO_PUBLIC_WEB_URL || 'http://localhost:3000';
