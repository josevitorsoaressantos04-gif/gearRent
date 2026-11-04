// Cria o cliente HTTP compartilhado para as rotas da API.
const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Centraliza a limpeza da sessão e o redirecionamento para o login.
window.GR = window.GR || {};
window.GR.auth = window.GR.auth || {};
window.GR.auth.sair = () => {
  sessionStorage.removeItem('gearrent_token');
  sessionStorage.removeItem('token');
  sessionStorage.removeItem('usuario');
  window.location.replace('login.html');
};

// Anexa o token de autenticação às requisições quando houver sessão ativa.
apiClient.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('gearrent_token');

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

// Disponibiliza o cliente HTTP para os scripts das páginas.
window.apiClient = apiClient;
