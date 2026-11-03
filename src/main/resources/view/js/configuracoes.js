(() => {
  const tokenKey = 'gearrent_token';
  const token = sessionStorage.getItem(tokenKey);
  if (!token) {
    window.location.replace('login.html');
    return;
  }

  window.GR = window.GR || {};
  window.GR.auth = window.GR.auth || {};
  if (typeof window.GR.auth.sair !== 'function') {
    window.GR.auth.sair = () => {
      sessionStorage.removeItem(tokenKey);
      sessionStorage.removeItem('token');
      sessionStorage.removeItem('usuario');
      window.location.replace('login.html');
    };
  }

  const refs = {
    table: document.getElementById('operatorsTableBody'),
    message: document.getElementById('settingsMessage'),
    error: document.getElementById('settingsError'),
    profileName: document.getElementById('profileDisplayName'),
    profileAvatar: document.getElementById('profileAvatar'),
    profileFirstName: document.getElementById('profileFirstName'),
    profileEmail: document.getElementById('profileEmail'),
    profileBirthDate: document.getElementById('profileBirthDate'),
    theme: document.getElementById('baseTheme'),
    operatorDialog: document.getElementById('operatorDialog'),
  };
  const state = {
    users: [],
    currentUserId: null,
  };

  function getErrorMessage(error) {
    if (error.response?.status === 401) {
      window.GR.auth.sair();
      return 'Sua sessão expirou. Entre novamente.';
    }
    const messages = error.response?.data?.mensagens;
    if (Array.isArray(messages) && messages.length) return messages.join(' ');
    if (error.response?.status === 404) return 'Este recurso ainda não está disponível na API atual.';
    if (error.response) return `A solicitação falhou (HTTP ${error.response.status}).`;
    return 'Não foi possível conectar à API. Verifique a conexão e tente novamente.';
  }

  async function getJSON(path) {
    const response = await window.apiClient.get(path);
    return response.data;
  }

  function setError(message) {
    refs.error.textContent = message;
    refs.error.hidden = !message;
  }

  function setMessage(message) {
    refs.message.textContent = message;
    refs.message.hidden = !message;
  }

  function getTokenSubject() {
    try {
      const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      return JSON.parse(window.atob(payload)).sub;
    } catch {
      return null;
    }
  }

  function initials(name) {
    return String(name || 'GR').trim().split(/\s+/).slice(0, 2)
      .map((part) => part.charAt(0).toLocaleUpperCase('pt-BR')).join('') || 'GR';
  }

  function formatDateForInput(value) {
    if (!value) return '';
    const match = String(value).match(/^(\d{4}-\d{2}-\d{2})/);
    return match ? match[1] : '';
  }

  function populateProfile(user) {
    refs.profileName.textContent = user.nome;
    refs.profileFirstName.value = user.nome || '';
    refs.profileEmail.value = user.login || '';
    refs.profileBirthDate.value = formatDateForInput(user.dataNascimento);
    refs.profileAvatar.textContent = initials(user.nome);
    document.getElementById('userNameSidebar').textContent = user.nome;
    document.getElementById('userNameTopbar').textContent = user.nome;
    document.getElementById('baseNameSidebar').textContent = `Base de ${user.nome}`;
    document.getElementById('avatarSidebar').textContent = initials(user.nome);
    document.getElementById('avatarTopbar').textContent = initials(user.nome);
  }

  function createCell(text) {
    const cell = document.createElement('td');
    cell.textContent = text || '—';
    return cell;
  }

  function renderUsers() {
    refs.table.replaceChildren();
    if (!state.users.length) {
      const row = document.createElement('tr');
      const cell = createCell('Nenhum operador encontrado para esta base.');
      cell.colSpan = 5;
      cell.className = 'table-state';
      row.append(cell);
      refs.table.append(row);
      return;
    }

    state.users.forEach((user) => {
      const row = document.createElement('tr');
      const status = String(user.status || 'Não informado');
      const actionCell = document.createElement('td');
      const action = document.createElement('button');
      action.type = 'button';
      action.className = 'btn btn--secondary operator-action';
      action.textContent = status === 'ATIVO' ? 'Desativação bloqueada' : 'Reativação indisponível';
      action.disabled = true;
      action.title = 'A API ainda não protege a gestão de acesso por papel de administrador.';
      action.setAttribute('aria-label', `Gestão de acesso de ${user.nome || 'operador'} indisponível`);
      actionCell.append(action);
      row.append(
        createCell(user.nome),
        createCell(user.login),
        createCell('Não definido na API'),
        createCell(status),
        actionCell,
      );
      refs.table.append(row);
    });
  }

  async function loadUsers() {
    setError('');
    setMessage('Carregando usuários da base…');
    try {
      const users = await getJSON('/usuarios');
      if (!Array.isArray(users)) throw new Error('A resposta de usuários veio em formato inesperado.');
      state.users = users;
      state.currentUserId = getTokenSubject();
      const current = users.find((user) => String(user.id) === String(state.currentUserId));
      if (current?.nome) populateProfile(current);
      else setError('A lista foi carregada, mas não foi possível identificar seu usuário autenticado.');
      renderUsers();
      setMessage('');
    } catch (error) {
      state.users = [];
      renderUsers();
      setMessage('');
      setError(`${getErrorMessage(error)} A consulta de operadores utiliza GET /usuarios, restrita ao tenant autenticado.`);
    }
  }

  function activateTab(name, updateHash = true) {
    const tab = document.querySelector(`[data-tab="${name}"]`);
    const panel = document.getElementById(`panel${name.charAt(0).toUpperCase()}${name.slice(1)}`);
    if (!tab || !panel) return;
    document.querySelectorAll('.settings-tab').forEach((item) => {
      const selected = item === tab;
      item.setAttribute('aria-selected', String(selected));
      item.tabIndex = selected ? 0 : -1;
    });
    document.querySelectorAll('.settings-panel').forEach((item) => {
      item.hidden = item !== panel;
    });
    if (updateHash) {
      const hash = name === 'profile' ? '#perfil' : `#${name}`;
      history.replaceState(null, '', hash);
    }
    tab.focus();
  }

  function closeDialog(dialog) {
    if (dialog.open) dialog.close();
  }

  document.getElementById('btnLogout').addEventListener('click', () => window.GR.auth.sair());
  document.querySelectorAll('.settings-tab').forEach((tab) => {
    tab.addEventListener('click', () => activateTab(tab.dataset.tab));
    tab.addEventListener('keydown', (event) => {
      if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
      event.preventDefault();
      const tabs = [...document.querySelectorAll('.settings-tab')];
      const index = tabs.indexOf(tab);
      const targetIndex = event.key === 'Home' ? 0
        : event.key === 'End' ? tabs.length - 1
          : (index + (event.key === 'ArrowRight' ? 1 : -1) + tabs.length) % tabs.length;
      activateTab(tabs[targetIndex].dataset.tab);
    });
  });
  window.addEventListener('hashchange', () => {
    const tabName = window.location.hash === '#perfil' ? 'profile'
      : window.location.hash === '#base' ? 'base' : 'operators';
    activateTab(tabName, false);
  });
  document.getElementById('openOperatorDialog').addEventListener('click', () => refs.operatorDialog.showModal());
  document.querySelectorAll('[data-close-operator-dialog]').forEach((button) => {
    button.addEventListener('click', () => closeDialog(refs.operatorDialog));
  });
  document.getElementById('operatorForm').addEventListener('submit', (event) => {
    event.preventDefault();
    setError('Não foi enviado: ainda não existe uma rota segura de cadastro de operador vinculada à base atual.');
  });
  document.getElementById('baseForm').addEventListener('submit', (event) => {
    event.preventDefault();
    setError('Os dados da base não foram enviados; a API ainda não oferece atualização de Locadora.');
  });
  document.getElementById('profileForm').addEventListener('submit', (event) => {
    event.preventDefault();
    setError('O perfil não foi enviado; a rota atual exige dados que a consulta não fornece e senha no corpo da atualização.');
  });
  document.getElementById('passwordForm').addEventListener('submit', (event) => {
    event.preventDefault();
    setError('A senha não foi enviada; solicite a implementação de uma rota autenticada de troca de senha.');
  });
  refs.theme.value = localStorage.getItem('gearRentTheme') || 'light';
  if (refs.theme.value === 'dark') document.body.dataset.theme = 'dark';
  refs.theme.addEventListener('change', () => {
    if (refs.theme.value === 'dark') {
      document.body.dataset.theme = 'dark';
    } else {
      delete document.body.dataset.theme;
    }
    localStorage.setItem('gearRentTheme', refs.theme.value);
    setMessage('Preferência visual aplicada e salva neste navegador.');
  });
  refs.operatorDialog.addEventListener('click', (event) => {
    if (event.target === refs.operatorDialog) closeDialog(refs.operatorDialog);
  });

  const initialTab = window.location.hash === '#perfil' ? 'profile'
    : window.location.hash === '#base' ? 'base' : 'operators';
  activateTab(initialTab, false);
  loadUsers();
})();
