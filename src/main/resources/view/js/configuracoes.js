(() => {
  const tokenKey = 'gearrent_token';
  const token = sessionStorage.getItem(tokenKey);
  if (!token) {
    window.location.replace('login.html');
    return;
  }
  if (localStorage.getItem('gearRentTheme') === 'dark') document.body.dataset.theme = 'dark';

  const refs = {
    table: document.getElementById('operatorsTableBody'),
    message: document.getElementById('settingsMessage'),
    error: document.getElementById('settingsError'),
    profileNotice: document.getElementById('profileNotice'),
    baseNotice: document.getElementById('baseNotice'),
    operatorNotice: document.getElementById('operatorNotice'),
    operatorFormNotice: document.getElementById('operatorFormNotice'),
    profileAvatar: document.getElementById('profileAvatar'),
    avatarFile: document.getElementById('profileAvatarFile'),
    avatarFileName: document.getElementById('profileAvatarFileName'),
    removeAvatar: document.getElementById('removeProfileAvatar'),
    profileFirstName: document.getElementById('profileFirstName'),
    profileLastName: document.getElementById('profileLastName'),
    profileEmail: document.getElementById('profileEmail'),
    profileCpf: document.getElementById('profileCpf'),
    profileBirthDate: document.getElementById('profileBirthDate'),
    theme: document.getElementById('baseTheme'),
    operatorDialog: document.getElementById('operatorDialog'),
    settingsNavLink: document.querySelector('.sidebar__nav a[href="configuracoes.html"]'),
    saveBaseButton: document.getElementById('saveBaseButton'),
    saveProfileButton: document.getElementById('saveProfileButton'),
    changePasswordButton: document.getElementById('changePasswordButton'),
    submitOperatorButton: document.getElementById('submitOperatorButton'),
    auditTable: document.getElementById('auditTableBody'),
    auditNotice: document.getElementById('auditNotice'),
    auditModuleFilter: document.getElementById('auditModuleFilter'),
    auditRetroactiveOnly: document.getElementById('auditRetroactiveOnly'),
    refreshAuditButton: document.getElementById('refreshAuditButton'),
  };
  const state = {
    users: [],
    auditLogs: [],
    isAdmin: false,
    avatarUpdatedAt: null,
    lastQueriedCep: '',
    cepRequestSeq: 0,
  };

  // Converte respostas de erro HTTP em mensagens adequadas para a interface.
  function getErrorMessage(error) {
    if (error.response?.status === 401) {
      window.GR.auth.sair();
      return 'Sua sessão expirou. Entre novamente.';
    }
    const messages = error.response?.data?.mensagens;
    if (Array.isArray(messages) && messages.length) return messages.join(' ');
    if (error.response?.status === 403) return 'Seu usuário não tem permissão para executar esta operação.';
    if (error.response?.status === 404) return 'O registro solicitado não foi encontrado.';
    if (error.response) return `A solicitação falhou (HTTP ${error.response.status}).`;
    return 'Não foi possível conectar à API. Verifique a conexão e tente novamente.';
  }

  // Lê as informações públicas do token tratando Base64URL com padding e UTF-8.
  function tokenClaims() {
    try {
      const parts = String(token || '').split('.');
      if (parts.length !== 3) return {};
      let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      while (base64.length % 4 !== 0) {
        base64 += '=';
      }
      const binary = window.atob(base64);
      const bytes = Uint8Array.from(binary, (c) => c.charCodeAt(0));
      const json = new TextDecoder('utf-8').decode(bytes);
      return JSON.parse(json);
    } catch {
      return {};
    }
  }

  // Gera as iniciais exibidas nos avatares do painel.
  function initials(name) {
    return String(name || 'GR').trim().split(/\s+/).slice(0, 2)
      .map((part) => part.charAt(0).toLocaleUpperCase('pt-BR')).join('') || 'GR';
  }

  // Mostra ou oculta o aviso geral de erro da página.
  function setError(message) {
    refs.error.textContent = message;
    refs.error.hidden = !message;
  }

  // Mostra uma confirmação geral para a última operação concluída.
  function setMessage(message) {
    refs.message.textContent = message;
    refs.message.hidden = !message;
  }

  // Atualiza um aviso específico associado a um formulário.
  function setNotice(element, message) {
    element.textContent = message;
    element.hidden = !message;
  }

  // Evita envios duplicados e comunica visualmente o estado do botão.
  function setBusy(button, busy, busyText, readyText) {
    button.disabled = busy;
    button.textContent = busy ? busyText : readyText;
  }

  // Executa uma consulta autenticada e devolve somente os dados da resposta.
  async function getJSON(path) {
    const response = await window.apiClient.get(path);
    return response.data;
  }

  // Atualiza os dados do usuário e da base no cabeçalho lateral.
  async function updateHeader(user, baseName) {
    const name = user.nome || 'Usuário autenticado';
    if (!refs.profileAvatar.querySelector('img')) {
      refs.profileAvatar.textContent = initials(name);
    }
    if (window.GR?.sidebar) {
      window.GR.sidebar.aplicarNoDom({
        nome: name,
        ...(baseName && baseName !== 'Base GearRent' ? { baseName } : {}),
      });
    } else {
      document.getElementById('userNameSidebar').textContent = name;
      document.getElementById('baseNameSidebar').textContent = baseName || 'Minha base';
      document.getElementById('avatarSidebar').textContent = initials(name);
    }
    state.avatarUpdatedAt = user.avatarAtualizadoEm || null;
    refs.removeAvatar.hidden = !state.avatarUpdatedAt;
    await Promise.all([
      refs.profileAvatar,
      document.getElementById('avatarSidebar'),
    ].map((element) => window.GR.avatar.carregar(element, state.avatarUpdatedAt)));
  }

  // Preenche o formulário de perfil com os dados do usuário atual.
  async function populateProfile(user) {
    refs.profileFirstName.value = user.nome || '';
    refs.profileLastName.value = user.sobrenome || '';
    refs.profileEmail.value = user.email || '';
    refs.profileCpf.value = user.cpf || '';
    refs.profileBirthDate.value = user.dataNascimento || '';
    await updateHeader(user, document.getElementById('baseNameSidebar').textContent);
  }

  // Preenche o formulário de configuração com os dados da locadora.
  function populateBase(base) {
    document.getElementById('baseDisplayName').value = base.nomeFantasia || '';
    document.getElementById('basePhone').value = base.telefone || '';
    document.getElementById('baseDocument').value = base.cnpj || '';
    const address = base.endereco || {};
    document.getElementById('baseStreet').value = address.logradouro || '';
    document.getElementById('baseAddressNumber').value = address.numero || '';
    document.getElementById('baseComplement').value = address.complemento || '';
    document.getElementById('baseNeighborhood').value = address.bairro || '';
    document.getElementById('baseCity').value = address.cidade || '';
    document.getElementById('baseState').value = address.estado || '';
    document.getElementById('basePostalCode').value = window.GR.cep
      ? window.GR.cep.formatar(address.cep || '')
      : (address.cep || '');
    state.lastQueriedCep = window.GR.cep ? window.GR.cep.digitos(address.cep || '') : '';
    const displayBaseName = base.nomeFantasia || 'Minha base';
    if (window.GR?.sidebar) {
      window.GR.sidebar.aplicarNoDom({ baseName: displayBaseName });
    } else {
      document.getElementById('baseNameSidebar').textContent = displayBaseName;
    }
  }

  // Consulta o CEP no backend (GET /api/cep/{cep} -> ViaCEP) e preenche os campos de endereço
  // da base com os dados retornados no EnderecoDTO.
  async function lookupBaseCep(force = false) {
    if (!state.isAdmin || !window.GR?.cep) return;
    const postalCodeInput = document.getElementById('basePostalCode');
    const cepDigits = window.GR.cep.digitos(postalCodeInput.value);
    // Só dispara a busca quando os 8 números do CEP foram digitados
    if (cepDigits.length !== 8) return;
    // Evita repetir a requisição se o CEP não mudou desde a última consulta
    if (!force && cepDigits === state.lastQueriedCep) return;

    state.lastQueriedCep = cepDigits;
    const currentSeq = ++state.cepRequestSeq;
    setNotice(refs.baseNotice, 'Consultando CEP…');

    try {
      const dto = await window.GR.cep.consultar(cepDigits);
      if (currentSeq !== state.cepRequestSeq || !dto) return;

      // Atualiza os campos do formulário da base com os valores do EnderecoDTO
      if (dto.cep) postalCodeInput.value = window.GR.cep.formatar(dto.cep);
      if (dto.logradouro != null) document.getElementById('baseStreet').value = dto.logradouro;
      if (dto.bairro != null) document.getElementById('baseNeighborhood').value = dto.bairro;
      if (dto.cidade != null) document.getElementById('baseCity').value = dto.cidade;
      if (dto.estado != null) document.getElementById('baseState').value = dto.estado;
      const complementInput = document.getElementById('baseComplement');
      if (dto.complemento && !complementInput.value.trim()) {
        complementInput.value = dto.complemento;
      }
      setNotice(refs.baseNotice, '');

      // Posiciona o cursor no campo Número (ou Logradouro se o CEP não tiver rua específica)
      const nextFocus = dto.logradouro
        ? document.getElementById('baseAddressNumber')
        : document.getElementById('baseStreet');
      nextFocus?.focus();
    } catch (error) {
      if (currentSeq !== state.cepRequestSeq) return;
      state.lastQueriedCep = '';
      setNotice(refs.baseNotice, getErrorMessage(error));
    }
  }

  // Cria uma célula de tabela com texto inserido sem interpretar HTML.
  function createCell(text) {
    const cell = document.createElement('td');
    cell.textContent = text || '—';
    return cell;
  }

  // Atualiza a tabela com os operadores e suas ações disponíveis.
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
      if (state.isAdmin && status === 'ATIVO') {
        const action = document.createElement('button');
        action.type = 'button';
        action.className = 'btn btn--secondary operator-action';
        action.textContent = 'Desativar';
        action.dataset.deactivateId = String(user.id);
        action.setAttribute('aria-label', `Desativar ${user.nome || 'operador'}`);
        actionCell.append(action);
      } else if (state.isAdmin && status === 'INATIVO') {
        const action = document.createElement('button');
        action.type = 'button';
        action.className = 'btn btn--secondary operator-action';
        action.textContent = 'Reativar';
        action.dataset.reactivateId = String(user.id);
        action.setAttribute('aria-label', `Reativar ${user.nome || 'operador'}`);
        actionCell.append(action);
      } else {
        const action = document.createElement('button');
        action.type = 'button';
        action.className = 'btn btn--secondary operator-action';
        action.textContent = 'Ação indisponível';
        action.disabled = true;
        action.title = 'Somente administradores podem desativar ou reativar operadores.';
        actionCell.append(action);
      }
      row.append(
        createCell([user.nome, user.sobrenome].filter(Boolean).join(' ')),
        createCell(user.login),
        createCell(user.role),
        createCell(status),
        actionCell,
      );
      refs.table.append(row);
    });
  }

  // Consulta os operadores da base e atualiza sua listagem.
  async function loadUsers() {
    try {
      const users = await getJSON('/usuarios');
      if (!Array.isArray(users)) throw new Error('A resposta de usuários veio em formato inesperado.');
      state.users = users;
      renderUsers();
    } catch (error) {
      state.users = [];
      renderUsers();
      setError(`${getErrorMessage(error)} Não foi possível carregar os operadores da base.`);
    }
  }

  // Busca o usuário autenticado e atualiza o perfil e o cabeçalho.
  async function loadProfile() {
    try {
      const user = await getJSON('/usuarios/me');
      await populateProfile(user);
      if (!user.cpf) setNotice(refs.profileNotice, 'A API não retornou CPF para o perfil. Informe um CPF válido antes de salvar.');
    } catch (error) {
      setNotice(refs.profileNotice, getErrorMessage(error));
    }
  }

  // Carrega os dados da base para o formulário de configurações.
  async function loadBase() {
    try {
      const base = await getJSON('/locadora');
      populateBase(base);
    } catch (error) {
      setNotice(refs.baseNotice, getErrorMessage(error));
    }
  }

  // Formata data e hora dos logs de auditoria no padrão brasileiro.
  function formatAuditDateTime(value) {
    if (!value) return '—';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return String(value);
    return new Intl.DateTimeFormat('pt-BR', {
      dateStyle: 'short',
      timeStyle: 'medium',
    }).format(date);
  }

  // Renderiza a tabela de logs de auditoria.
  function renderAuditLogs() {
    if (!refs.auditTable) return;
    refs.auditTable.replaceChildren();

    if (!state.isAdmin) {
      const row = document.createElement('tr');
      const cell = createCell('Acesso restrito a administradores da base.');
      cell.colSpan = 5;
      cell.className = 'table-state';
      row.append(cell);
      refs.auditTable.append(row);
      return;
    }

    if (!state.auditLogs.length) {
      const row = document.createElement('tr');
      const cell = createCell('Nenhum registro de auditoria encontrado para os filtros selecionados.');
      cell.colSpan = 5;
      cell.className = 'table-state';
      row.append(cell);
      refs.auditTable.append(row);
      return;
    }

    const acaoLabels = {
      CADASTRO_RETROATIVO: 'Cadastro retroativo',
      CADASTRO: 'Cadastro',
      ATUALIZACAO: 'Atualização',
      DEVOLUCAO: 'Devolução',
      CANCELAMENTO: 'Cancelamento',
      INATIVACAO: 'Inativação',
      DESATIVACAO: 'Desativação',
      REATIVACAO: 'Reativação',
    };

    state.auditLogs.forEach((item) => {
      const row = document.createElement('tr');
      if (item.retroativo) row.classList.add('audit-row--retroactive');

      const actionCell = document.createElement('td');
      const badge = document.createElement('span');
      badge.className = item.retroativo ? 'audit-badge audit-badge--retroactive' : 'audit-badge';
      badge.textContent = acaoLabels[item.acao] || item.acao || '—';
      actionCell.append(badge);

      row.append(
        createCell(formatAuditDateTime(item.dataHora)),
        createCell(item.usuarioNome || 'Sistema'),
        createCell(item.modulo || '—'),
        actionCell,
        createCell(item.detalhes || '—'),
      );
      refs.auditTable.append(row);
    });
  }

  // Consulta os logs de auditoria da base (exclusivo para administradores).
  async function loadAuditLogs() {
    if (!refs.auditTable) return;
    if (!state.isAdmin) {
      renderAuditLogs();
      return;
    }
    try {
      const params = new URLSearchParams();
      if (refs.auditModuleFilter?.value) {
        params.set('modulo', refs.auditModuleFilter.value);
      }
      if (refs.auditRetroactiveOnly?.checked) {
        params.set('apenasRetroativos', 'true');
      }
      const query = params.toString();
      const logs = await getJSON(`/auditoria${query ? `?${query}` : ''}`);
      state.auditLogs = Array.isArray(logs) ? logs : [];
      renderAuditLogs();
    } catch (error) {
      state.auditLogs = [];
      renderAuditLogs();
      setNotice(refs.auditNotice, getErrorMessage(error));
    }
  }

  // Alterna os painéis de configuração e sincroniza a aba com a URL.
  function activateTab(name, updateHash = true) {
    const tab = document.querySelector(`[data-tab="${name}"]`);
    const panel = document.getElementById(`panel${name.charAt(0).toUpperCase()}${name.slice(1)}`);
    if (!tab || !panel) return;
    if (refs.settingsNavLink) {
      refs.settingsNavLink.classList.add('nav-item--active');
      refs.settingsNavLink.setAttribute('aria-current', 'page');
    }
    document.querySelectorAll('.settings-tab').forEach((item) => {
      const selected = item === tab;
      item.setAttribute('aria-selected', String(selected));
      item.tabIndex = selected ? 0 : -1;
    });
    document.querySelectorAll('.settings-panel').forEach((item) => {
      item.hidden = item !== panel;
    });
    if (updateHash) {
      history.replaceState(null, '', name === 'profile' ? '#perfil' : `#${name}`);
    }
    tab.focus();
  }

  // Desativa o operador (PATCH /usuarios/{id}/desativar, exige ROLE_ADMIN) e atualiza a tabela.
  async function deactivateUser(id, button) {
    button.disabled = true;
    try {
      const resposta = await window.apiClient.patch(`/usuarios/${encodeURIComponent(id)}/desativar`);
      setError('');
      setMessage(resposta.data?.mensagem || 'Usuário desativado com sucesso.');
      await Promise.all([loadUsers(), loadAuditLogs()]);
    } catch (error) {
      button.disabled = false;
      setError(getErrorMessage(error));
    }
  }

  // Reativa o operador selecionado e atualiza o estado da tabela.
  async function reactivateUser(id, button) {
    button.disabled = true;
    try {
      await window.apiClient.patch(`/usuarios/${encodeURIComponent(id)}/reativar`);
      setError('');
      setMessage('Usuário reativado com sucesso.');
      await Promise.all([loadUsers(), loadAuditLogs()]);
    } catch (error) {
      button.disabled = false;
      setError(getErrorMessage(error));
    }
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
  if (window._grSettingsHashHandler) {
    window.removeEventListener('hashchange', window._grSettingsHashHandler);
  }
  window._grSettingsHashHandler = () => {
    if (!document.getElementById('panelOperators')) return;
    const tabName = window.location.hash === '#perfil' ? 'profile'
      : window.location.hash === '#base' ? 'base'
      : window.location.hash === '#audit' ? 'audit' : 'operators';
    activateTab(tabName, false);
  };
  window.addEventListener('hashchange', window._grSettingsHashHandler);

  document.getElementById('openOperatorDialog').addEventListener('click', () => {
    document.getElementById('operatorForm').reset();
    setNotice(refs.operatorFormNotice, '');
    refs.operatorDialog.showModal();
  });
  document.querySelectorAll('[data-close-operator-dialog]').forEach((button) => {
    button.addEventListener('click', () => refs.operatorDialog.close());
  });
  refs.table.addEventListener('click', (event) => {
    const button = event.target.closest('button[data-reactivate-id]');
    if (button) reactivateUser(button.dataset.reactivateId, button);
    const off = event.target.closest('button[data-deactivate-id]');
    if (off) deactivateUser(off.dataset.deactivateId, off);
  });

  document.getElementById('operatorForm').addEventListener('submit', async (event) => {
    event.preventDefault();
    const button = refs.submitOperatorButton;
    setBusy(button, true, 'Criando…', 'Adicionar operador');
    setNotice(refs.operatorFormNotice, '');
    try {
      await window.apiClient.post('/usuarios', {
        nome: document.getElementById('operatorName').value.trim(),
        sobrenome: document.getElementById('operatorLastName').value.trim(),
        email: document.getElementById('operatorEmail').value.trim(),
        senha: document.getElementById('operatorPassword').value,
        role: document.getElementById('operatorRole').value,
      });
      refs.operatorDialog.close();
      setMessage('Operador criado com sucesso.');
      await Promise.all([loadUsers(), loadAuditLogs()]);
    } catch (error) {
      setNotice(refs.operatorFormNotice, getErrorMessage(error));
    } finally {
      setBusy(button, false, 'Criando…', 'Adicionar operador');
    }
  });

  document.getElementById('basePostalCode').addEventListener('input', (event) => {
    const input = event.target;
    if (window.GR?.cep) {
      input.value = window.GR.cep.formatar(input.value);
      if (window.GR.cep.digitos(input.value).length === 8) {
        lookupBaseCep();
      } else {
        state.lastQueriedCep = '';
      }
    }
  });
  document.getElementById('basePostalCode').addEventListener('blur', () => {
    lookupBaseCep();
  });
  document.getElementById('baseState').addEventListener('input', (event) => {
    event.target.value = event.target.value.replace(/[^a-z]/gi, '').slice(0, 2).toUpperCase();
  });

  document.getElementById('baseForm').addEventListener('submit', async (event) => {
    event.preventDefault();
    const button = refs.saveBaseButton;
    setBusy(button, true, 'Salvando…', 'Salvar dados da base');
    setNotice(refs.baseNotice, '');
    try {
      const name = document.getElementById('baseDisplayName').value.trim();
      const response = await window.apiClient.put('/locadora', {
        nome: name,
        nomeFantasia: name,
        cnpj: document.getElementById('baseDocument').value || null,
        telefone: document.getElementById('basePhone').value.trim(),
        endereco: {
          logradouro: document.getElementById('baseStreet').value.trim(),
          numero: document.getElementById('baseAddressNumber').value.trim(),
          complemento: document.getElementById('baseComplement').value.trim() || null,
          bairro: document.getElementById('baseNeighborhood').value.trim(),
          cidade: document.getElementById('baseCity').value.trim(),
          estado: document.getElementById('baseState').value.trim().toUpperCase(),
          cep: document.getElementById('basePostalCode').value.trim(),
        },
      });
      populateBase(response.data);
      setMessage('Dados da base atualizados com sucesso.');
      await loadAuditLogs();
    } catch (error) {
      setNotice(refs.baseNotice, getErrorMessage(error));
    } finally {
      setBusy(button, false, 'Salvando…', 'Salvar dados da base');
    }
  });

  document.getElementById('profileForm').addEventListener('submit', async (event) => {
    event.preventDefault();
    const button = refs.saveProfileButton;
    const cpfDigits = refs.profileCpf.value.replace(/\D/g, '');
    if (cpfDigits && (cpfDigits.length !== 11 || /^(\d)\1{10}$/.test(cpfDigits))) {
      setNotice(refs.profileNotice, 'Informe um CPF válido com 11 números.');
      return;
    }
    setBusy(button, true, 'Salvando…', 'Salvar perfil');
    setNotice(refs.profileNotice, '');
    try {
      const response = await window.apiClient.put('/usuarios/me', {
        nome: refs.profileFirstName.value.trim(),
        sobrenome: refs.profileLastName.value.trim(),
        email: refs.profileEmail.value.trim(),
        cpf: cpfDigits || null,
      });
      await populateProfile(response.data);
      setMessage('Perfil atualizado com sucesso.');
    } catch (error) {
      setNotice(refs.profileNotice, getErrorMessage(error));
    } finally {
      setBusy(button, false, 'Salvando…', 'Salvar perfil');
    }
  });

  refs.avatarFile.addEventListener('change', async () => {
    const arquivo = refs.avatarFile.files[0];
    if (!arquivo) return;
    refs.avatarFileName.textContent = arquivo.name;

    if (arquivo.size > 2 * 1024 * 1024) {
      setNotice(refs.profileNotice, 'A foto é muito grande. Envie uma imagem de até 2 MB.');
      refs.avatarFile.value = '';
      refs.avatarFileName.textContent = 'Nenhum arquivo selecionado';
      return;
    }

    const dados = new FormData();
    dados.append('arquivo', arquivo);
    refs.avatarFile.disabled = true;
    setNotice(refs.profileNotice, '');
    try {
      const resposta = await window.apiClient.post('/usuarios/avatar', dados, {
        headers: { 'Content-Type': null },
      });
      state.avatarUpdatedAt = resposta.data.avatarAtualizadoEm;
      refs.removeAvatar.hidden = !state.avatarUpdatedAt;
      await Promise.all([
        refs.profileAvatar,
        document.getElementById('avatarSidebar'),
      ].map((element) => window.GR.avatar.carregar(element, state.avatarUpdatedAt)));
      setNotice(refs.profileNotice, resposta.data.mensagem || 'Foto de perfil atualizada.');
    } catch (error) {
      setNotice(refs.profileNotice, getErrorMessage(error));
    } finally {
      refs.avatarFile.disabled = false;
      refs.avatarFile.value = '';
      refs.avatarFileName.textContent = 'Nenhum arquivo selecionado';
    }
  });

  refs.removeAvatar.addEventListener('click', async () => {
    refs.removeAvatar.disabled = true;
    setNotice(refs.profileNotice, '');
    try {
      const resposta = await window.apiClient.delete('/usuarios/avatar');
      state.avatarUpdatedAt = null;
      refs.removeAvatar.hidden = true;
      await Promise.all([
        refs.profileAvatar,
        document.getElementById('avatarSidebar'),
      ].map((element) => window.GR.avatar.carregar(element, null)));
      setNotice(refs.profileNotice, resposta.data.mensagem || 'Foto de perfil removida.');
    } catch (error) {
      setNotice(refs.profileNotice, getErrorMessage(error));
    } finally {
      refs.removeAvatar.disabled = false;
    }
  });

  document.getElementById('passwordForm').addEventListener('submit', async (event) => {
    event.preventDefault();
    const button = refs.changePasswordButton;
    const newPassword = document.getElementById('newPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;
    if (newPassword !== confirmPassword) {
      setError('A confirmação precisa ser igual à nova senha.');
      return;
    }
    setBusy(button, true, 'Atualizando…', 'Atualizar minha senha');
    setError('');
    try {
      await window.apiClient.patch('/usuarios/trocar-senha', {
        senhaAtual: document.getElementById('currentPassword').value,
        novaSenha: newPassword,
        confirmacaoSenha: confirmPassword,
      });
      document.getElementById('passwordForm').reset();
      setMessage('Senha alterada com sucesso.');
    } catch (error) {
      setError(getErrorMessage(error));
    } finally {
      setBusy(button, false, 'Atualizando…', 'Atualizar minha senha');
    }
  });

  refs.theme.value = localStorage.getItem('gearRentTheme') || 'light';
  if (refs.theme.value === 'dark') document.body.dataset.theme = 'dark';
  refs.theme.addEventListener('change', () => {
    if (refs.theme.value === 'dark') document.body.dataset.theme = 'dark';
    else delete document.body.dataset.theme;
    localStorage.setItem('gearRentTheme', refs.theme.value);
    setMessage('Preferência visual aplicada e salva neste navegador.');
  });
  refs.operatorDialog.addEventListener('click', (event) => {
    if (event.target === refs.operatorDialog) refs.operatorDialog.close();
  });

  state.isAdmin = tokenClaims().role === 'ADMIN';
  document.getElementById('openOperatorDialog').disabled = !state.isAdmin;
  refs.saveBaseButton.disabled = !state.isAdmin;
  document.querySelectorAll('#baseForm input:not([readonly])').forEach((input) => {
    input.disabled = !state.isAdmin;
  });
  if (refs.auditModuleFilter) refs.auditModuleFilter.disabled = !state.isAdmin;
  if (refs.auditRetroactiveOnly) refs.auditRetroactiveOnly.disabled = !state.isAdmin;
  if (refs.refreshAuditButton) refs.refreshAuditButton.disabled = !state.isAdmin;
  refs.auditModuleFilter?.addEventListener('change', loadAuditLogs);
  refs.auditRetroactiveOnly?.addEventListener('change', loadAuditLogs);
  refs.refreshAuditButton?.addEventListener('click', loadAuditLogs);
  setNotice(refs.operatorNotice, state.isAdmin
    ? 'Como administrador, você pode cadastrar, desativar e reativar operadores da sua base.'
    : 'Somente administradores podem cadastrar, desativar ou reativar operadores.');
  if (!state.isAdmin) setNotice(refs.baseNotice, 'Somente administradores podem atualizar os dados da base.');
  setNotice(refs.auditNotice, state.isAdmin
    ? 'Registros de auditoria das ações executadas na base. Locações cadastradas de forma retroativa aparecem em destaque.'
    : 'Somente administradores podem visualizar os logs de auditoria da base.');

  const initialTab = window.location.hash === '#perfil' ? 'profile'
    : window.location.hash === '#base' ? 'base'
    : window.location.hash === '#audit' ? 'audit' : 'operators';
  activateTab(initialTab, false);
  loadUsers();
  loadProfile();
  loadBase();
  loadAuditLogs();
})();
