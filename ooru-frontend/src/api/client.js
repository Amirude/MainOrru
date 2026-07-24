import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
});

// Attach the JWT (if we have one) to every outgoing request.
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('ooru_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// If the backend says our token is no longer valid, clear it so the app returns to the login screen.
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('ooru_token');
      localStorage.removeItem('ooru_user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export function extractErrorMessage(error) {
  if (error.response && error.response.data) {
    const data = error.response.data;
    if (typeof data.message === 'string') return data.message;
    // Field validation errors come back as { fieldName: "message" }
    const firstKey = Object.keys(data)[0];
    if (firstKey) return data[firstKey];
  }
  return 'Something went wrong. Please try again.';
}
