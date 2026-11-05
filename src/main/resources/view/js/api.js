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

// Carrega a foto autenticada nos avatares e mantém as iniciais como fallback.
window.GR.avatar = {
  async carregar(elemento, atualizadoEm) {
    const urlAnterior = elemento.dataset.avatarObjectUrl;
    if (urlAnterior) URL.revokeObjectURL(urlAnterior);

    elemento.querySelector('img')?.remove();
    delete elemento.dataset.avatarObjectUrl;
    elemento.classList.remove('avatar--com-foto');
    if (!atualizadoEm) return;

    try {
      const resposta = await apiClient.get(
        `/usuarios/me/avatar?v=${encodeURIComponent(atualizadoEm)}`,
        { responseType: 'blob' },
      );
      const url = URL.createObjectURL(resposta.data);
      const imagem = document.createElement('img');
      imagem.className = 'avatar__image';
      imagem.alt = '';
      imagem.setAttribute('aria-hidden', 'true');
      imagem.src = url;
      elemento.append(imagem);
      elemento.dataset.avatarObjectUrl = url;
      elemento.classList.add('avatar--com-foto');
    } catch (erro) {
      if (erro.response?.status !== 404) throw erro;
    }
  },
};
