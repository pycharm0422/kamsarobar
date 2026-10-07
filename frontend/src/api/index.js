import client from './client';

/** Full URL of an uploaded photo (the API returns paths like "/images/{id}"). */
export const imageUrl = (image) => `${client.defaults.baseURL}${image.url}`;

// Each resource gets its own small API object, so pages depend only on what they use.
const data = (request) => request.then((response) => response.data);

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
  updateSettings: (id, body) => data(client.put(`/cities/${id}/settings`, body)),
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

export const imageApi = {
  upload: (file) => {
    const form = new FormData();
    form.append('file', file);
    // Generous timeout: a 3 MB photo can take a while on a slow mobile connection.
    return data(client.post('/images', form, { timeout: 120000 }));
  },
};

export const eventApi = {
  mine: (params) => data(client.get('/events/mine', { params })),
  upcoming: (params) => data(client.get('/events/upcoming', { params })),
  attend: (postId) => data(client.put(`/posts/${postId}/attendance`)),
  leave: (postId) => data(client.delete(`/posts/${postId}/attendance`)),
};

export const donationApi = {
  summary: () => data(client.get('/donations/summary')),
  citySummary: (cityId) => data(client.get(`/cities/${cityId}/donation-summary`)),
  record: (body) => data(client.post('/donations', body)),
  mine: (params) => data(client.get('/donations/mine', { params })),
  supporters: (cityId, params) => data(client.get(`/cities/${cityId}/supporters`, { params })),
  forCity: (cityId, params) => data(client.get(`/cities/${cityId}/donations`, { params })),
  review: (id, status) => data(client.patch(`/donations/${id}/status`, { status })),
};

export const campaignApi = {
  forCity: (cityId, activeOnly = true) => data(client.get(`/cities/${cityId}/campaigns`, { params: { activeOnly } })),
  create: (cityId, body) => data(client.post(`/cities/${cityId}/campaigns`, body)),
  update: (id, body) => data(client.put(`/campaigns/${id}`, body)),
  remove: (id) => data(client.delete(`/campaigns/${id}`)),
};

export const adminApi = {
  stats: () => data(client.get('/admin/stats')),
  cities: () => data(client.get('/admin/cities')),
  createCity: (body) => data(client.post('/admin/cities', body)),
  updateCity: (id, body) => data(client.put(`/admin/cities/${id}`, body)),
  users: (params) => data(client.get('/admin/users', { params })),
  cityAdmins: () => data(client.get('/admin/city-admins')),
  assignCityAdmin: (userId, cityId) => data(client.post('/admin/city-admins', { userId, cityId })),
  revokeCityAdmin: (userId) => data(client.delete(`/admin/city-admins/${userId}`)),
};
