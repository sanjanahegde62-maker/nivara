import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080',
  headers: { 'Content-Type': 'application/json' },
});

// Attach JWT on every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('nivara_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Global 401 → redirect to login
api.interceptors.response.use(
  (r) => r,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('nivara_token');
      localStorage.removeItem('nivara_user');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

export default api;
