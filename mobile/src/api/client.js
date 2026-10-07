import axios from 'axios';
import { tokenStorage } from '../auth/tokenStorage';
import { getApiUrl } from '../config';

const client = axios.create({ timeout: 20000 });

let onUnauthorized = () => {};

/** The auth layer registers what to do when the server says the login is no longer valid (expired / blocked). */
export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler;
}

client.interceptors.request.use((config) => {
  config.baseURL = getApiUrl(); // the server can be changed on the login screen
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
