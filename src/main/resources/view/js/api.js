// Cria o cliente HTTP compartilhado para as rotas da API.
const apiClient = axios.create({
  baseURL: 'http:/https://gearrent-eo3x.onrender.com/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Centraliza a limpeza da sessão (incluindo o cache visual da sidebar) e o redirecionamento para o login.
window.GR = window.GR || {};
window.GR.auth = window.GR.auth || {};
window.GR.auth.sair = () => {
  sessionStorage.removeItem('gearrent_token');
  sessionStorage.removeItem('token');
  sessionStorage.removeItem('usuario');
  sessionStorage.removeItem('gearrent_sidebar_cache');
  sessionStorage.removeItem('gearrent_avatar_cache');
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

// ============================================================================
// GERENCIAMENTO DA SIDEBAR (CACHE DE PERFIL E NOME DA BASE)
// Evita que o nome do usuário, nome da base e iniciais voltem para o texto
// padrão ("Usuário autenticado" / "GR") ao trocar de módulo ou recarregar.
// ============================================================================
window.GR.sidebar = {
  // Lê os dados salvos da sidebar no sessionStorage da aba atual.
  lerCache() {
    try {
      return JSON.parse(sessionStorage.getItem('gearrent_sidebar_cache') || '{}');
    } catch {
      return {};
    }
  },

  // Mescla novos dados (nome ou baseName) com o cache já salvo no sessionStorage.
  salvarCache(dados) {
    const atual = this.lerCache();
    const novo = { ...atual, ...dados };
    try {
      sessionStorage.setItem('gearrent_sidebar_cache', JSON.stringify(novo));
    } catch {
      // Ignora caso o storage esteja indisponível no navegador
    }
    return novo;
  },

  // Extrai até duas iniciais do nome do usuário (ex.: "João Silva" -> "JS").
  iniciais(nome) {
    return String(nome || 'GR')
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map((parte) => parte.charAt(0).toLocaleUpperCase('pt-BR'))
      .join('') || 'GR';
  },

  // Atualiza os elementos da sidebar no DOM somente quando o valor mudou,
  // e preserva a tag <img> do avatar caso ela já esteja carregada (evitando piscar).
  aplicarNoDom({ nome, baseName } = {}) {
    const cache = this.salvarCache({
      ...(nome ? { nome } : {}),
      ...(baseName ? { baseName } : {}),
    });
    const elNome = document.getElementById('userNameSidebar');
    const elBase = document.getElementById('baseNameSidebar');
    const elAvatar = document.getElementById('avatarSidebar');

    if (elNome && cache.nome && elNome.textContent !== cache.nome) {
      elNome.textContent = cache.nome;
    }
    const textoBase = cache.baseName || (cache.nome ? `Base de ${cache.nome}` : '');
    if (elBase && textoBase && elBase.textContent !== textoBase) {
      elBase.textContent = textoBase;
    }
    if (elAvatar && cache.nome) {
      const textoIniciais = this.iniciais(cache.nome);
      // IMPORTANTE: só escreve textContent se NÃO houver <img> dentro do avatar,
      // pois atribuir textContent removeria o elemento <img> filho e causaria piscar.
      const imgExistente = elAvatar.querySelector('img');
      if (!imgExistente) {
        elAvatar.textContent = textoIniciais;
      }
    }
  },

  // Restaura imediatamente (de forma síncrona) o nome, a base e a foto do usuário
  // a partir do sessionStorage assim que a página abre, antes das chamadas HTTP.
  restaurarImediatamente() {
    const cache = this.lerCache();
    if (cache.nome || cache.baseName) {
      this.aplicarNoDom(cache);
    }
    try {
      const avatarCache = JSON.parse(sessionStorage.getItem('gearrent_avatar_cache') || 'null');
      const elAvatar = document.getElementById('avatarSidebar');
      if (elAvatar && avatarCache?.dataUrl && avatarCache?.v) {
        window.GR.avatar.aplicarImagem(elAvatar, avatarCache.dataUrl, avatarCache.v);
      }
    } catch {
      // Ignora erro de parse
    }
  },
};

// ============================================================================
// CARREGAMENTO DO AVATAR COM CACHE EM SESSÃO
// Reutiliza a imagem se a versão (avatarAtualizadoEm) não mudou, evitando
// apagar e baixar a foto novamente a cada troca de tela.
// ============================================================================
window.GR.avatar = {
  // Insere ou atualiza a tag <img> dentro do container de avatar sem destruí-la.
  aplicarImagem(elemento, src, versao) {
    if (!elemento) return;
    let imagem = elemento.querySelector('img');
    if (!imagem) {
      imagem = document.createElement('img');
      imagem.className = 'avatar__image';
      imagem.alt = '';
      imagem.setAttribute('aria-hidden', 'true');
      elemento.append(imagem);
    }
    if (imagem.src !== src) {
      imagem.src = src;
    }
    elemento.dataset.avatarVersion = String(versao || '');
    elemento.classList.add('avatar--com-foto');
  },

  // Remove a foto do avatar quando o usuário exclui a imagem ou não possui foto.
  limparImagem(elemento) {
    if (!elemento) return;
    const urlAnterior = elemento.dataset.avatarObjectUrl;
    if (urlAnterior) URL.revokeObjectURL(urlAnterior);
    elemento.querySelector('img')?.remove();
    delete elemento.dataset.avatarObjectUrl;
    delete elemento.dataset.avatarVersion;
    elemento.classList.remove('avatar--com-foto');
  },

  // Converte o Blob binário retornado pela API em uma Data URL (base64)
  // para poder guardar no sessionStorage e exibir instantaneamente.
  blobParaDataUrl(blob) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onloadend = () => resolve(reader.result);
      reader.onerror = reject;
      reader.readAsDataURL(blob);
    });
  },

  // Carrega o avatar do usuário autenticado (do cache se a versão for a mesma, ou da API).
  async carregar(elemento, atualizadoEm) {
    if (!elemento) return;
    if (!atualizadoEm) {
      this.limparImagem(elemento);
      sessionStorage.removeItem('gearrent_avatar_cache');
      return;
    }

    const versao = String(atualizadoEm);
    // Se o elemento já está exibindo exatamente esta versão da foto, não faz nada.
    if (elemento.dataset.avatarVersion === versao && elemento.querySelector('img')) {
      return;
    }

    // Se a mesma versão já está salva no sessionStorage, aplica sem fazer requisição de rede.
    try {
      const cache = JSON.parse(sessionStorage.getItem('gearrent_avatar_cache') || 'null');
      if (cache && cache.v === versao && cache.dataUrl) {
        this.aplicarImagem(elemento, cache.dataUrl, versao);
        return;
      }
    } catch {
      // Continua para buscar na API
    }

    // Busca a foto na API apenas quando ainda não está em cache ou quando foi atualizada.
    try {
      const resposta = await apiClient.get(
        `/usuarios/me/avatar?v=${encodeURIComponent(versao)}`,
        { responseType: 'blob' },
      );
      const dataUrl = await this.blobParaDataUrl(resposta.data);
      try {
        sessionStorage.setItem('gearrent_avatar_cache', JSON.stringify({ v: versao, dataUrl }));
      } catch {
        // Ignora limite de quota do storage
      }
      this.aplicarImagem(elemento, dataUrl, versao);
    } catch (erro) {
      if (erro.response?.status === 404) {
        this.limparImagem(elemento);
        return;
      }
      throw erro;
    }
  },
};

// ============================================================================
// INTEGRAÇÃO COM VIACEP (FORMATAÇÃO E CONSULTA DE ENDEREÇO POR CEP)
// ============================================================================
window.GR.cep = {
  // Extrai apenas os números do CEP, limitando a 8 dígitos.
  digitos(valor) {
    return String(valor || '').replace(/\D/g, '').slice(0, 8);
  },

  // Aplica a máscara 00000-000 conforme o usuário digita.
  formatar(valor) {
    const numeros = this.digitos(valor);
    return numeros.length > 5 ? `${numeros.slice(0, 5)}-${numeros.slice(5)}` : numeros;
  },

  // Consulta o endpoint GET /api/cep/{cep} do backend e retorna os campos do EnderecoDTO.
  async consultar(cep) {
    const numeros = this.digitos(cep);
    if (numeros.length !== 8) return null;
    try {
      const resposta = await apiClient.get(`/cep/${encodeURIComponent(numeros)}`);
      return resposta.data;
    } catch (erro) {
      // Fallback direto ao ViaCEP caso o contêiner do backend ainda não tenha sido reconstruído
      const mensagem = erro.response?.data?.mensagens?.[0] || '';
      if (erro.response?.status === 404 && mensagem.includes('endereço solicitado')) {
        const respostaViaCep = await axios.get(`https://viacep.com.br/ws/${encodeURIComponent(numeros)}/json/`);
        const dados = respostaViaCep.data;
        if (!dados || dados.erro === true || dados.erro === 'true' || !dados.cep) {
          const naoEncontrado = new Error('CEP não encontrado.');
          naoEncontrado.response = {
            status: 404,
            data: { mensagens: ['CEP não encontrado. Confira os números informados.'] },
          };
          throw naoEncontrado;
        }
        return {
          logradouro: (dados.logradouro || '').trim(),
          numero: '',
          complemento: (dados.complemento || '').trim(),
          bairro: (dados.bairro || '').trim(),
          cidade: (dados.localidade || '').trim(),
          estado: (dados.uf || '').trim().toUpperCase(),
          cep: (dados.cep || '').trim(),
        };
      }
      throw erro;
    }
  },
};

// ============================================================================
// NAVEGAÇÃO FLUIDA ENTRE MÓDULOS (SPA - SINGLE PAGE APPLICATION)
// Intercepta os cliques nos links do menu lateral (<aside class="sidebar">)
// para trocar somente o conteúdo de .main-panel e os <dialog> do módulo.
// Assim, a sidebar permanece 100% estática no DOM e preserva suas animações CSS.
// ============================================================================
window.GR.nav = {
  emAndamento: false,

  // Extrai apenas o nome do arquivo HTML da URL (ex.: "clientes.html?id=1" -> "clientes.html").
  paginaDeUrl(url) {
    const limpo = String(url || '').split('#')[0].split('?')[0];
    return limpo.split('/').pop() || 'visao-geral.html';
  },

  // Atualiza qual botão da sidebar recebe a classe visual .nav-item--active,
  // mantendo a transição suave do CSS sem recriar a sidebar.
  atualizarItemAtivo(paginaAlvo) {
    document.querySelectorAll('.sidebar__nav a.nav-item').forEach((link) => {
      const paginaLink = this.paginaDeUrl(link.getAttribute('href'));
      const ativo = paginaLink === paginaAlvo;
      link.classList.toggle('nav-item--active', ativo);
      if (ativo) {
        link.setAttribute('aria-current', 'page');
      } else {
        link.removeAttribute('aria-current');
      }
    });
  },

  // Carrega o HTML do módulo de destino em segundo plano e substitui apenas a .main-panel.
  async irPara(urlDestino, { pushHistory = true } = {}) {
    if (this.emAndamento) return;
    const paginaAlvo = this.paginaDeUrl(urlDestino);
    const paginaAtual = this.paginaDeUrl(window.location.pathname);

    // Se for apenas mudança de âncora (#hash) na mesma página, altera apenas o hash.
    if (paginaAlvo === paginaAtual && String(urlDestino).includes('#')) {
      window.location.hash = String(urlDestino).split('#')[1];
      return;
    }

    this.emAndamento = true;
    this.atualizarItemAtivo(paginaAlvo);

    try {
      // 1. Busca o documento HTML do módulo clicado
      const resposta = await fetch(urlDestino, { cache: 'no-cache' });
      if (!resposta.ok) {
        window.location.href = urlDestino;
        return;
      }
      const html = await resposta.text();
      const doc = new DOMParser().parseFromString(html, 'text/html');
      const novoMain = doc.querySelector('.main-panel');
      const atualMain = document.querySelector('.main-panel');
      if (!novoMain || !atualMain) {
        window.location.href = urlDestino;
        return;
      }

      // 2. Atualiza o título da aba e as classes específicas do <body> mantendo o tema escuro/claro
      document.title = doc.title || document.title;
      const temaAtual = localStorage.getItem('gearRentTheme') === 'dark' ? 'dark' : null;
      document.body.className = doc.body.className;
      if (temaAtual === 'dark') {
        document.body.dataset.theme = 'dark';
      } else {
        delete document.body.dataset.theme;
      }

      // 3. Troca apenas o painel central (.main-panel) com efeito suave de opacidade;
      //    a <aside class="sidebar"> NÃO é tocada nem recarregada.
      novoMain.classList.add('main-panel--entering');
      atualMain.replaceWith(novoMain);
      requestAnimationFrame(() => {
        novoMain.classList.remove('main-panel--entering');
      });

      // 4. Remove os <dialog> do módulo anterior e adiciona os <dialog> do novo módulo
      document.querySelectorAll('body > dialog').forEach((dlg) => dlg.remove());
      doc.querySelectorAll('body > dialog').forEach((dlg) => {
        document.body.append(dlg);
      });

      // 5. Atualiza a URL na barra de endereços do navegador (suportando Voltar/Avançar)
      if (pushHistory) {
        window.history.pushState({ url: urlDestino }, doc.title || '', urlDestino);
      }
      window.scrollTo({ top: 0, behavior: 'instant' });

      // 6. Carrega e executa o script JS específico do novo módulo (ex.: clientes.js, locacoes.js)
      const scriptsPagina = [...doc.querySelectorAll('script[src]')]
        .map((s) => s.getAttribute('src'))
        .filter((src) => src && !src.includes('axios') && !src.endsWith('config.js') && !src.endsWith('api.js'));

      for (const src of scriptsPagina) {
        await new Promise((resolve, reject) => {
          const script = document.createElement('script');
          script.src = `${src}?t=${Date.now()}`;
          script.onload = () => {
            script.remove();
            resolve();
          };
          script.onerror = reject;
          document.body.append(script);
        });
      }
    } catch {
      // Em caso de falha inesperada, faz a navegação tradicional como segurança
      window.location.href = urlDestino;
    } finally {
      this.emAndamento = false;
    }
  },

  // Registra os ouvintes de clique nos links da sidebar e no botão Voltar/Avançar do navegador.
  inicializar() {
    window.GR.sidebar.restaurarImediatamente();

    const nav = document.querySelector('.sidebar__nav');
    if (!nav || nav.dataset.spaBound === 'true') return;
    nav.dataset.spaBound = 'true';

    nav.addEventListener('click', (event) => {
      const link = event.target.closest('a.nav-item');
      if (!link) return;
      // Permite abrir em nova aba normalmente com Ctrl+Clique ou botão do meio
      if (event.metaKey || event.ctrlKey || event.shiftKey || event.altKey || event.button !== 0) return;
      const href = link.getAttribute('href');
      if (!href || href.startsWith('http') || href.startsWith('#')) return;

      event.preventDefault();
      const paginaAlvo = this.paginaDeUrl(href);
      const paginaAtual = this.paginaDeUrl(window.location.pathname);
      // Se clicou no módulo que já está aberto, não recarrega
      if (paginaAlvo === paginaAtual && !href.includes('?') && !href.includes('#')) {
        return;
      }
      this.irPara(href);
    });

    // Suporta os botões Voltar e Avançar do navegador mantendo a sidebar estática
    window.addEventListener('popstate', () => {
      const destino = `${window.location.pathname.split('/').pop() || 'visao-geral.html'}${window.location.search}${window.location.hash}`;
      this.irPara(destino, { pushHistory: false });
    });
  },
};

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => window.GR.nav.inicializar());
} else {
  window.GR.nav.inicializar();
}
