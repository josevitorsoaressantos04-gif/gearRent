const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

window.GR = window.GR || {};
window.GR.auth = window.GR.auth || {};
window.GR.auth.sair = () => {
  sessionStorage.removeItem('gearrent_token');
  sessionStorage.removeItem('token');
  sessionStorage.removeItem('usuario');
  window.location.replace('login.html');
};

apiClient.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('gearrent_token');

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

window.apiClient = apiClient;
