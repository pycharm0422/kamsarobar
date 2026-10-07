import axios from 'axios';
import { tokenStorage } from '../auth/tokenStorage';
import { API_URL } from '../config';

const client = axios.create({ baseURL: API_URL, timeout: 20000 });

let onUnauthorized = () => {};

/** The auth layer registers what to do when the server says the login is no longer valid (expired / blocked). */
export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler;
}

client.interceptors.request.use((config) => {
  const token = tokenStorage.current();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && tokenStorage.current()) onUnauthorized();
    return Promise.reject(error);
  },
);

export default client;
