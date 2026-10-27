const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('gearrent_token');

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

window.apiClient = apiClient;
