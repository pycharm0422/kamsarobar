import { Platform } from 'react-native';
import { tokenStorage } from '../auth/tokenStorage';
import { API_URL } from '../config';
import client from './client';

// One small API object per resource (same shape as the website's), so screens depend only on what they use.
const data = (request) => request.then((response) => response.data);

export const imageUrl = (image) => `${API_URL}${image.url}`;

export const authApi = {
  register: (body) => data(client.post('/auth/register', body)),
  login: (body) => data(client.post('/auth/login', body)),
};

export const userApi = {
  me: () => data(client.get('/users/me')),
  update: (body) => data(client.put('/users/me', body)),
  changePassword: (body) => data(client.put('/users/me/password', body)),
};

export const profileApi = {
  mine: () => data(client.get('/profile/me')),
  save: (body) => data(client.put('/profile/me', body)),
  suggestCompanies: (q) => data(client.get('/suggestions/companies', { params: { q } })),
  suggestExpertise: (q) => data(client.get('/suggestions/expertise', { params: { q } })),
};

export const cityApi = {
  list: () => data(client.get('/cities')),
  get: (id) => data(client.get(`/cities/${id}`)),
};

export const directoryApi = {
  referrers: (params) => data(client.get('/directory/referrers', { params })),
  experts: (params) => data(client.get('/directory/experts', { params })),
};

export const postApi = {
  feed: (params) => data(client.get('/posts', { params })),
  get: (id) => data(client.get(`/posts/${id}`)),
  create: (body) => data(client.post('/posts', body)),
  update: (id, body) => data(client.put(`/posts/${id}`, body)),
  remove: (id) => data(client.delete(`/posts/${id}`)),
  comments: (postId, params) => data(client.get(`/posts/${postId}/comments`, { params })),
  addComment: (postId, body) => data(client.post(`/posts/${postId}/comments`, body)),
  updateComment: (id, body) => data(client.put(`/comments/${id}`, body)),
  removeComment: (id) => data(client.delete(`/comments/${id}`)),
};

export const eventApi = {
  mine: (params) => data(client.get('/events/mine', { params })),
  upcoming: (params) => data(client.get('/events/upcoming', { params })),
  attend: (postId) => data(client.put(`/posts/${postId}/attendance`)),
  leave: (postId) => data(client.delete(`/posts/${postId}/attendance`)),
};

export const donationApi = {
  citySummary: (cityId) => data(client.get(`/cities/${cityId}/donation-summary`)),
  record: (body) => data(client.post('/donations', body)),
  mine: (params) => data(client.get('/donations/mine', { params })),
  campaigns: (cityId) => data(client.get(`/cities/${cityId}/campaigns`)),
};

export const notificationApi = {
  registerDevice: (token, platform) => data(client.put('/notifications/devices', { token, platform })),
  unregisterDevice: (token) => data(client.delete('/notifications/devices', { params: { token } })),
  settings: () => data(client.get('/notifications/settings')),
  updateSettings: (body) => data(client.put('/notifications/settings', body)),
};

/**
 * Uploads one (already resized) photo. Uses fetch because multipart uploads of local files are more
 * reliable with React Native's own networking than through axios.
 */
export async function uploadImage({ uri, name, type }) {
  const form = new FormData();
  if (Platform.OS === 'web') {
    const blob = await (await fetch(uri)).blob();
    form.append('file', blob, name);
  } else {
    form.append('file', { uri, name, type });
  }
  const response = await fetch(`${API_URL}/images`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${tokenStorage.current()}` },
    body: form,
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    const error = new Error(body.message || 'Upload failed');
    error.response = { status: response.status, data: body };
    throw error;
  }
  return body;
}
