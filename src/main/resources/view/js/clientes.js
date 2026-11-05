(() => {
  const tokenKey = 'gearrent_token';
  const token = sessionStorage.getItem(tokenKey);
  if (!token) {
    window.location.replace('login.html');
    return;
  }
  if (localStorage.getItem('gearRentTheme') === 'dark') document.body.dataset.theme = 'dark';

  window.GR = window.GR || {};
  window.GR.api = window.GR.api || {};
  window.GR.api.clientes = {
    listar: () => window.apiClient.get('/clientes'),
    buscarPorId: (id) => window.apiClient.get(`/clientes/${encodeURIComponent(id)}`),
    cadastrar: (payload) => window.apiClient.post('/clientes', payload),
    atualizar: (id, payload) => window.apiClient.put(`/clientes/${encodeURIComponent(id)}`, payload),
    excluir: (id) => window.apiClient.delete(`/clientes/${encodeURIComponent(id)}`),
  };
  window.GR.auth = window.GR.auth || {};
  if (typeof window.GR.auth.sair !== 'function') {
    window.GR.auth.sair = () => {
      sessionStorage.removeItem(tokenKey);
      sessionStorage.removeItem('token');
      sessionStorage.removeItem('usuario');
      window.location.replace('login.html');
    };
  }

  const state = {
    clientes: [],
    selectedId: null,
    editingId: null,
    pendingDeleteId: null,
    search: '',
    typeFilter: 'TODOS',
    statusFilter: 'TODOS',
    detailRequest: 0,
    lastFocusedElement: null,
  };

  const refs = {
    table: document.getElementById('clientesTableBody'),
    counter: document.getElementById('contadorClientes'),
    search: document.getElementById('clienteSearch'),
    typeFilter: document.getElementById('tipoPessoaFilter'),
    statusFilter: document.getElementById('statusFilter'),
    message: document.getElementById('mensagemClientes'),
    error: document.getElementById('erroClientes'),
    history: document.getElementById('historicoLocacoes'),
    formError: document.getElementById('erroFormulario'),
    form: document.getElementById('clienteForm'),
    modal: document.getElementById('clienteModal'),
    modalTitle: document.getElementById('modalTitle'),
    saveButton: document.getElementById('salvarCliente'),
    editNotice: document.getElementById('avisoDocumentoEdicao'),
    type: document.getElementById('tipoPessoa'),
    nameLabel: document.getElementById('labelNome'),
    documentLabel: document.getElementById('labelDocumento'),
    document: document.getElementById('documentoCliente'),
    name: document.getElementById('nomeCliente'),
    phone: document.getElementById('telefoneCliente'),
    email: document.getElementById('emailCliente'),
    street: document.getElementById('logradouroCliente'),
    number: document.getElementById('numeroCliente'),
    complement: document.getElementById('complementoCliente'),
    neighborhood: document.getElementById('bairroCliente'),
    city: document.getElementById('cidadeCliente'),
    state: document.getElementById('estadoCliente'),
    postalCode: document.getElementById('cepCliente'),
    confirmDialog: document.getElementById('confirmarInativacao'),
    confirmMessage: document.getElementById('confirmMessage'),
    confirmButton: document.getElementById('confirmarAcaoInativar'),
  };

  // Exibe ou limpa o aviso geral de erro da página.
  function setError(message) {
    refs.error.textContent = message;
    refs.error.hidden = !message;
  }

  // Mostra uma falha de validação dentro do formulário do cliente.
  function showFormError(message) {
    refs.formError.textContent = message;
    refs.formError.hidden = !message;
  }

  // Apresenta uma confirmação após uma operação de cliente.
  function showMessage(message) {
    refs.message.textContent = message;
    refs.message.hidden = !message;
  }

  // Traduz respostas HTTP e falhas de conexão em texto para a interface.
  function getErrorMessage(error) {
    if (error.response?.status === 401) {
      window.GR.auth.sair();
      return 'Sua sessão expirou. Entre novamente.';
    }

    const apiMessages = error.response?.data?.mensagens;
    if (Array.isArray(apiMessages) && apiMessages.length) return apiMessages.join(' ');
    if (error.response?.data?.message) return error.response.data.message;
    if (error.response) return `A solicitação falhou (HTTP ${error.response.status}).`;
    return 'Não foi possível conectar à API. Verifique a conexão e tente novamente.';
  }

  // Remove caracteres não numéricos de documentos e telefones.
  function digits(value) {
    return String(value || '').replace(/\D/g, '');
  }

  // Infere se o documento informado corresponde a CPF ou CNPJ.
  function inferPersonType(documentValue) {
    const length = digits(documentValue).length;
    return length === 14 ? 'PJ' : length === 11 ? 'PF' : null;
  }

  // Normaliza o tipo de pessoa usando o tipo selecionado ou o documento.
  function normalizePersonType(type, documentValue) {
    if (type === 'PF' || type === 'FISICA') return 'PF';
    if (type === 'PJ' || type === 'JURIDICA') return 'PJ';
    return inferPersonType(documentValue);
  }

  // Padroniza os campos do cliente recebidos da API para uso na tela.
  function normalizeClient(raw) {
    const documentValue = raw.documento || '';
    return {
      ...raw,
      id: raw.id,
      codigoSequencial: raw.codigoSequencial ?? raw.codigo ?? null,
      tipoPessoa: normalizePersonType(raw.tipoPessoa, documentValue),
      telefone: raw.celular || raw.telefone || '',
      endereco: raw.endereco || raw.enderecoResidencial || null,
    };
  }

  // Calcula as iniciais para o avatar do cliente.
  function initials(name) {
    return String(name || '?')
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part.charAt(0).toLocaleUpperCase('pt-BR'))
      .join('') || '?';
  }

  // Carrega o perfil autenticado e atualiza o cabeçalho da página.
  async function loadProfile() {
    try {
      const response = await window.apiClient.get('/usuarios/me');
      const currentUser = response.data;
      if (!currentUser?.nome) throw new Error('Não foi possível identificar o usuário autenticado.');

      document.getElementById('userNameSidebar').textContent = currentUser.nome;
      document.getElementById('baseNameSidebar').textContent = `Base de ${currentUser.nome}`;
      document.getElementById('avatarSidebar').textContent = initials(currentUser.nome);
      await window.GR.avatar.carregar(
        document.getElementById('avatarSidebar'),
        currentUser.avatarAtualizadoEm,
      );
    } catch (error) {
      if (error.response?.status === 401) {
        window.GR.auth.sair();
        return;
      }
      setError(`${getErrorMessage(error)} O perfil não pôde ser carregado.`);
    }
  }

  // Formata o código sequencial usado para identificar o cliente.
  function codeLabel(client) {
    if (client.codigoSequencial == null) return '—';
    return `CLI-${String(client.codigoSequencial).padStart(4, '0')}`;
  }

  // Filtra a lista de clientes pelo termo de busca atual.
  function filteredClients() {
    const term = state.search.trim().toLocaleLowerCase('pt-BR');
    return state.clientes.filter((client) => {
      const searchable = [
        client.nome,
        client.documento,
        client.codigoSequencial,
        client.id,
      ].filter((value) => value != null).join(' ').toLocaleLowerCase('pt-BR');
      const matchesTerm = !term || searchable.includes(term);
      const matchesType = state.typeFilter === 'TODOS' || client.tipoPessoa === state.typeFilter;
      const matchesStatus = state.statusFilter === 'TODOS' || client.status === state.statusFilter;
      return matchesTerm && matchesType && matchesStatus;
    });
  }

  // Cria uma célula da tabela e atribui seu conteúdo como texto.
  function createCell(text, className = '') {
    const cell = document.createElement('td');
    cell.textContent = text || '—';
    if (className) cell.className = className;
    return cell;
  }

  // Cria um botão acessível associado à ação e ao cliente selecionado.
  function createAction(label, icon, action, client) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = `icon-button ${action === 'editar' ? 'icon-button--edit' : 'icon-button--delete'}`;
    button.dataset.action = action;
    button.dataset.clientId = String(client.id);
    button.setAttribute('aria-label', `${label} ${client.nome}`);
    button.title = `${label} ${client.nome}`;
    button.textContent = icon;
    return button;
  }

  // Renderiza a lista filtrada de clientes e destaca a seleção atual.
  function renderTable() {
    const clients = filteredClients();
    refs.table.replaceChildren();

    if (!clients.length) {
      const row = document.createElement('tr');
      const cell = createCell(
        state.clientes.length ? 'Nenhum cliente corresponde aos filtros.' : 'Nenhum cliente cadastrado nesta base.',
        'table-state',
      );
      cell.colSpan = 6;
      row.append(cell);
      refs.table.append(row);
    } else {
      clients.forEach((client) => {
        const row = document.createElement('tr');
        row.dataset.clientId = String(client.id);
        row.tabIndex = 0;
        row.setAttribute('aria-selected', String(String(client.id) === String(state.selectedId)));
        row.setAttribute('aria-label', `Selecionar cliente ${client.nome}`);

        const typeCell = document.createElement('td');
        const typeBadge = document.createElement('span');
        typeBadge.className = `badge ${client.tipoPessoa === 'PJ' ? 'badge--pj' : client.tipoPessoa === 'PF' ? 'badge--pf' : 'badge--default'}`;
        typeBadge.textContent = client.tipoPessoa || 'N/D';
        typeCell.append(typeBadge);

        const actions = document.createElement('div');
        actions.className = 'table-actions';
        actions.append(
          createAction('Editar', '✏️', 'editar', client),
          createAction('Inativar', '🗑️', 'inativar', client),
        );

        const actionsCell = document.createElement('td');
        actionsCell.append(actions);
        row.append(
          createCell(codeLabel(client)),
          createCell(client.nome),
          typeCell,
          createCell(client.documento),
          createCell(client.telefone),
          actionsCell,
        );
        refs.table.append(row);
      });
    }

    refs.counter.textContent = `${clients.length} de ${state.clientes.length} cliente${state.clientes.length === 1 ? '' : 's'}`;
  }

  // Formata valores financeiros para o padrão monetário brasileiro.
  function formatCurrency(value) {
    const amount = Number(value);
    if (!Number.isFinite(amount)) return 'Não informado';
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(amount);
  }

  // Converte datas recebidas para o formato de exibição local.
  function formatDate(value) {
    if (!value) return 'Data não informada';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return String(value);
    return new Intl.DateTimeFormat('pt-BR').format(date);
  }

  // Monta o endereço do cliente em uma única linha legível.
  function addressText(address) {
    if (!address || typeof address !== 'object') return 'Não informado';
    return [
      [address.logradouro, address.numero].filter(Boolean).join(', '),
      address.complemento,
      address.bairro,
      [address.cidade, address.estado].filter(Boolean).join('/'),
      address.cep ? `CEP ${address.cep}` : '',
    ].filter(Boolean).join(' · ') || 'Não informado';
  }

  // Exibe um estado vazio ou uma mensagem na área de histórico.
  function appendHistoryEmpty(message) {
    const item = document.createElement('li');
    item.className = 'history-empty';
    item.textContent = message;
    refs.history.replaceChildren(item);
  }

  // Limpa os detalhes e orienta quando não há cliente selecionado.
  function renderEmptyDetails(message = 'Selecione um cliente') {
    document.getElementById('detailAvatar').textContent = '?';
    document.getElementById('detailNome').textContent = message;
    document.getElementById('detailCodigo').textContent = 'Código sequencial indisponível';
    document.getElementById('detailTipo').textContent = '—';
    document.getElementById('detailDocumento').textContent = '—';
    document.getElementById('detailTelefone').textContent = '—';
    document.getElementById('detailEmail').textContent = '—';
    document.getElementById('detailEndereco').textContent = '—';
    document.getElementById('detailStatus').textContent = '—';
    document.getElementById('metricTotalLocacoes').textContent = '—';
    document.getElementById('metricLocacoesAndamento').textContent = '—';
    document.getElementById('metricEquipamentosPosse').textContent = '—';
    document.getElementById('metricValorAberto').textContent = '—';
    appendHistoryEmpty('Selecione um cliente para ver os dados disponíveis.');
    document.getElementById('btnNovaLocacao').disabled = true;
  }

  // Preenche o painel lateral com dados, métricas e histórico do cliente.
  function renderDetails(client, historyAvailable = false) {
    if (!client) {
      renderEmptyDetails();
      return;
    }

    document.getElementById('detailAvatar').textContent = initials(client.nome);
    document.getElementById('detailNome').textContent = client.nome || 'Nome não informado';
    document.getElementById('detailCodigo').textContent = client.codigoSequencial == null
      ? 'Código sequencial não fornecido pela API'
      : `Código: ${codeLabel(client)}`;
    document.getElementById('detailTipo').textContent = client.tipoPessoa === 'PF'
      ? 'Pessoa Física'
      : client.tipoPessoa === 'PJ' ? 'Pessoa Jurídica' : 'Não informado pela API';
    document.getElementById('detailDocumento').textContent = client.documento || 'Não informado';
    document.getElementById('detailTelefone').textContent = client.telefone || 'Não informado';
    document.getElementById('detailEmail').textContent = client.email || 'Não informado';
    document.getElementById('detailEndereco').textContent = addressText(client.endereco);
    document.getElementById('detailStatus').textContent = client.status || 'Não informado';

    document.getElementById('metricTotalLocacoes').textContent = client.totalLocacoes ?? '—';
    document.getElementById('metricLocacoesAndamento').textContent = client.locacoesEmAndamento ?? '—';
    document.getElementById('metricEquipamentosPosse').textContent = client.equipamentosEmPosse ?? '—';
    document.getElementById('metricValorAberto').textContent = client.valorAberto == null
      ? '—'
      : formatCurrency(client.valorAberto);
    appendHistoryEmpty(historyAvailable
      ? 'Este cliente ainda não possui locações registradas.'
      : 'O histórico de locações não está disponível na API atual.');
    document.getElementById('btnNovaLocacao').disabled = false;
  }

  // Seleciona o cliente e consulta os detalhes relacionados à sua conta.
  async function selectClient(id) {
    const client = state.clientes.find((item) => String(item.id) === String(id));
    if (!client) return;
    state.selectedId = client.id;
    renderTable();
    renderDetails(client);
    showError('');

    const requestId = ++state.detailRequest;
    try {
      const response = await window.GR.api.clientes.buscarPorId(client.id);
      if (requestId !== state.detailRequest || String(state.selectedId) !== String(client.id)) return;
      const details = normalizeClient(response.data);
      const updated = {
        ...client,
        ...details,
        codigoSequencial: details.codigoSequencial ?? client.codigoSequencial,
      };
      state.clientes = state.clientes.map((item) => String(item.id) === String(client.id) ? updated : item);
      renderTable();
      renderDetails(updated);
    } catch (error) {
      if (requestId !== state.detailRequest) return;
      if (error.response?.status === 401) {
        window.GR.auth.sair();
        return;
      }
      setError(`Os dados da lista foram exibidos, mas não foi possível carregar o endereço completo. ${getErrorMessage(error)}`);
    }
  }

  // Mostra um erro geral e atualiza o painel de detalhes, se necessário.
  function showError(message) {
    setError(message);
  }

  // Busca clientes, atualiza a tabela e opcionalmente preserva a seleção.
  async function loadClients({ preserveSelection = true } = {}) {
    setError('');
    showMessage('Carregando clientes da base…');
    refs.counter.textContent = 'Carregando…';
    try {
      const response = await window.GR.api.clientes.listar();
      if (!Array.isArray(response.data)) throw new Error('A API retornou uma lista de clientes inválida.');
      state.clientes = response.data.map(normalizeClient);
      renderTable();
      showMessage('');

      const selected = preserveSelection
        ? state.clientes.find((client) => String(client.id) === String(state.selectedId))
        : null;
      if (selected) {
        await selectClient(selected.id);
      } else if (state.clientes.length) {
        await selectClient(state.clientes[0].id);
      } else {
        state.selectedId = null;
        renderDetails(null);
      }
    } catch (error) {
      showMessage('');
      refs.counter.textContent = 'Falha ao carregar';
      setError(getErrorMessage(error));
      const row = document.createElement('tr');
      const cell = createCell('Não foi possível carregar a lista. Tente novamente.', 'table-state');
      cell.colSpan = 6;
      row.append(cell);
      refs.table.replaceChildren(row);
      renderDetails(null);
    }
  }

  // Aplica a máscara correspondente ao CPF ou CNPJ informado.
  function formatDocument(value, type) {
    const valueDigits = digits(value).slice(0, type === 'PJ' ? 14 : 11);
    if (type === 'PJ') {
      let formatted = valueDigits;
      if (valueDigits.length > 2) formatted = `${valueDigits.slice(0, 2)}.${valueDigits.slice(2)}`;
      if (valueDigits.length > 5) formatted = `${valueDigits.slice(0, 2)}.${valueDigits.slice(2, 5)}.${valueDigits.slice(5)}`;
      if (valueDigits.length > 8) formatted = `${valueDigits.slice(0, 2)}.${valueDigits.slice(2, 5)}.${valueDigits.slice(5, 8)}/${valueDigits.slice(8)}`;
      if (valueDigits.length > 12) formatted = `${valueDigits.slice(0, 2)}.${valueDigits.slice(2, 5)}.${valueDigits.slice(5, 8)}/${valueDigits.slice(8, 12)}-${valueDigits.slice(12)}`;
      return formatted;
    }
    return valueDigits
      .replace(/^(\d{3})(\d)/, '$1.$2')
      .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
      .replace(/(\d{3})(\d{1,2})$/, '$1-$2');
  }

  // Atualiza o tipo de pessoa e ajusta o formulário para o documento escolhido.
  function setPersonType(type) {
    const isCompany = type === 'PJ';
    refs.nameLabel.textContent = isCompany ? 'Razão Social / Nome Fantasia' : 'Nome completo';
    refs.documentLabel.textContent = isCompany ? 'CNPJ' : 'CPF';
    refs.document.placeholder = isCompany ? '00.000.000/0000-00' : '000.000.000-00';
    refs.document.maxLength = isCompany ? 18 : 14;
    refs.document.value = formatDocument(refs.document.value, type);
  }

  // Valida dígitos verificadores e rejeita CPFs com números repetidos.
  function validCPF(value) {
    const cpf = digits(value);
    if (cpf.length !== 11 || /^(\d)\1{10}$/.test(cpf)) return false;
    const calculate = (length) => {
      let sum = 0;
      for (let i = 0; i < length; i += 1) sum += Number(cpf[i]) * (length + 1 - i);
      const remainder = (sum * 10) % 11;
      return remainder === 10 ? 0 : remainder;
    };
    return calculate(9) === Number(cpf[9]) && calculate(10) === Number(cpf[10]);
  }

  // Valida dígitos verificadores e rejeita CNPJs com números repetidos.
  function validCNPJ(value) {
    const cnpj = digits(value);
    if (cnpj.length !== 14 || /^(\d)\1{13}$/.test(cnpj)) return false;
    const calculate = (length) => {
      const weights = length === 12 ? [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2] : [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
      const sum = weights.reduce((total, weight, index) => total + Number(cnpj[index]) * weight, 0);
      const remainder = sum % 11;
      return remainder < 2 ? 0 : 11 - remainder;
    };
    return calculate(12) === Number(cnpj[12]) && calculate(13) === Number(cnpj[13]);
  }

  // Exibe o diálogo de criação ou edição de cliente.
  function openModal() {
    state.lastFocusedElement = document.activeElement;
    refs.modal.showModal();
    refs.name.focus();
  }

  // Prepara um formulário limpo para cadastrar um novo cliente.
  function openCreateModal() {
    state.editingId = null;
    refs.form.reset();
    showFormError('');
    refs.modalTitle.textContent = 'Novo Cliente';
    refs.saveButton.textContent = 'Salvar cliente';
    refs.editNotice.hidden = true;
    refs.type.disabled = false;
    refs.document.disabled = false;
    refs.type.value = 'PF';
    setPersonType('PF');
    openModal();
  }

  // Carrega o cliente escolhido e prepara o formulário de edição.
  async function openEditModal(id) {
    const client = state.clientes.find((item) => String(item.id) === String(id));
    if (!client) return;
    state.editingId = client.id;
    refs.form.reset();
    showFormError('');
    refs.modalTitle.textContent = 'Editar Cliente';
    refs.saveButton.textContent = 'Salvar alterações';
    refs.editNotice.hidden = false;
    refs.type.disabled = true;
    refs.document.disabled = true;
    refs.type.value = client.tipoPessoa || inferPersonType(client.documento) || 'PF';
    setPersonType(refs.type.value);
    refs.document.value = formatDocument(client.documento || '', refs.type.value);
    refs.name.value = client.nome || '';
    refs.phone.value = client.celular || client.telefone || '';
    refs.email.value = client.email || '';

    try {
      const response = await window.GR.api.clientes.buscarPorId(client.id);
      const full = normalizeClient(response.data);
      const address = full.endereco || {};
      refs.street.value = address.logradouro || '';
      refs.number.value = address.numero || '';
      refs.complement.value = address.complemento || '';
      refs.neighborhood.value = address.bairro || '';
      refs.city.value = address.cidade || '';
      refs.state.value = address.estado || '';
      refs.postalCode.value = address.cep || '';
    } catch (error) {
      if (error.response?.status === 401) {
        window.GR.auth.sair();
        return;
      }
      showFormError(`Não foi possível carregar o endereço completo. ${getErrorMessage(error)}`);
      return;
    }
    openModal();
  }

  // Fecha e limpa o diálogo de cadastro ou edição.
  function closeModal() {
    if (!refs.modal.open) return;
    refs.modal.close();
    state.editingId = null;
    if (state.lastFocusedElement?.isConnected) state.lastFocusedElement.focus();
  }

  // Monta o objeto que será enviado à API com os dados do formulário.
  function buildPayload() {
    const phone = refs.phone.value.trim();
    return {
      nome: refs.name.value.trim(),
      ...(!state.editingId ? { documento: digits(refs.document.value) } : {}),
      email: refs.email.value.trim() || null,
      celular: phone || null,
      telefone: phone || null,
      endereco: {
        logradouro: refs.street.value.trim(),
        numero: refs.number.value.trim(),
        complemento: refs.complement.value.trim() || null,
        bairro: refs.neighborhood.value.trim(),
        cidade: refs.city.value.trim(),
        estado: refs.state.value.trim().toUpperCase(),
        cep: refs.postalCode.value.trim(),
      },
    };
  }

  // Valida campos, documentos e dados obrigatórios antes do envio.
  function validateForm() {
    if (!refs.name.value.trim()) return 'Informe o nome do cliente.';
    if (!state.editingId) {
      const valid = refs.type.value === 'PJ'
        ? validCNPJ(refs.document.value)
        : validCPF(refs.document.value);
      if (!valid) return refs.type.value === 'PJ' ? 'CNPJ inválido. Confira os dígitos.' : 'CPF inválido. Confira os dígitos.';
    }
    if (refs.email.value.trim() && !refs.email.validity.valid) return 'Informe um endereço de e-mail válido.';
    if (!refs.street.value.trim() || !refs.number.value.trim() || !refs.neighborhood.value.trim()
      || !refs.city.value.trim() || !refs.state.value.trim() || !refs.postalCode.value.trim()) {
      return 'Preencha rua, número, bairro, cidade, UF e CEP.';
    }
    if (!/^[A-Za-z]{2}$/.test(refs.state.value.trim())) return 'Informe a UF com duas letras.';
    if (!/^\d{5}-?\d{3}$/.test(refs.postalCode.value.trim())) return 'Informe um CEP válido com 8 números.';
    return null;
  }

  // Envia os dados do formulário e atualiza a lista após o sucesso.
  async function saveClient(event) {
    event.preventDefault();
    showFormError('');
    const validationMessage = validateForm();
    if (validationMessage) {
      showFormError(validationMessage);
      return;
    }

    refs.saveButton.disabled = true;
    refs.saveButton.textContent = 'Salvando…';
    const editingId = state.editingId;
    try {
      const payload = buildPayload();
      if (editingId) {
        await window.GR.api.clientes.atualizar(editingId, payload);
      } else {
        const response = await window.GR.api.clientes.cadastrar(payload);
        state.selectedId = response.data?.id ?? null;
      }
      closeModal();
      showMessage(editingId ? 'Cliente atualizado com sucesso.' : 'Cliente cadastrado com sucesso.');
      await loadClients({ preserveSelection: true });
    } catch (error) {
      showFormError(getErrorMessage(error));
    } finally {
      refs.saveButton.disabled = false;
      refs.saveButton.textContent = state.editingId ? 'Salvar alterações' : 'Salvar cliente';
    }
  }

  // Abre a confirmação para inativar o cliente selecionado.
  function openDeleteConfirmation(id) {
    const client = state.clientes.find((item) => String(item.id) === String(id));
    if (!client) return;
    state.pendingDeleteId = client.id;
    refs.confirmMessage.textContent = `Deseja inativar ${client.nome}? A operação poderá ser bloqueada caso existam locações em andamento ou atrasadas.`;
    refs.confirmDialog.showModal();
  }

  // Confirma a inativação e recarrega a lista de clientes.
  async function confirmDelete() {
    const id = state.pendingDeleteId;
    if (id == null) return;
    refs.confirmButton.disabled = true;
    try {
      const response = await window.GR.api.clientes.excluir(id);
      state.clientes = state.clientes.filter((client) => String(client.id) !== String(id));
      if (String(state.selectedId) === String(id)) state.selectedId = null;
      refs.confirmDialog.close();
      state.pendingDeleteId = null;
      showMessage(response.data?.mensagem || 'Cliente inativado com sucesso.');
      renderTable();
      if (state.clientes.length) {
        await selectClient(state.clientes[0].id);
      } else {
        renderDetails(null);
      }
    } catch (error) {
      refs.confirmDialog.close();
      state.pendingDeleteId = null;
      setError(getErrorMessage(error));
    } finally {
      refs.confirmButton.disabled = false;
    }
  }

  // Encaminha cliques da tabela para seleção, detalhes ou ações do cliente.
  function handleTableClick(event) {
    const actionButton = event.target.closest('button[data-action]');
    if (actionButton) {
      event.stopPropagation();
      const { action, clientId } = actionButton.dataset;
      if (action === 'editar') openEditModal(clientId);
      if (action === 'inativar') openDeleteConfirmation(clientId);
      return;
    }

    const row = event.target.closest('tr[data-client-id]');
    if (row) selectClient(row.dataset.clientId);
  }

  // Permite selecionar uma linha da tabela usando o teclado.
  function handleTableKeydown(event) {
    if (event.target.matches('button')) return;
    if (event.key !== 'Enter' && event.key !== ' ') return;
    const row = event.target.closest('tr[data-client-id]');
    if (!row) return;
    event.preventDefault();
    selectClient(row.dataset.clientId);
  }

  // Formata o telefone conforme os dígitos digitados pelo usuário.
  function formatPhoneInput() {
    const value = digits(refs.phone.value).slice(0, 11);
    if (value.length < 3) {
      refs.phone.value = value ? `(${value}` : '';
      return;
    }
    const area = value.slice(0, 2);
    const subscriber = value.slice(2);
    refs.phone.value = subscriber.length > 8
      ? `(${area}) ${subscriber.slice(0, 5)}-${subscriber.slice(5)}`
      : `(${area}) ${subscriber.slice(0, 4)}${subscriber.length > 4 ? `-${subscriber.slice(4)}` : ''}`;
  }

  // Aplica a máscara de CEP durante o preenchimento do endereço.
  function formatPostalCode() {
    const value = digits(refs.postalCode.value).slice(0, 8);
    refs.postalCode.value = value.length > 5 ? `${value.slice(0, 5)}-${value.slice(5)}` : value;
  }

  // Registra os eventos de busca, formulários, tabela e diálogos.
  function bindEvents() {
    refs.search.addEventListener('input', () => {
      state.search = refs.search.value;
      renderTable();
      const visible = filteredClients();
      if (visible.length && !visible.some((client) => String(client.id) === String(state.selectedId))) {
        selectClient(visible[0].id);
      } else if (!visible.length) {
        state.selectedId = null;
        renderDetails(null);
      }
    });
    refs.typeFilter.addEventListener('change', () => {
      state.typeFilter = refs.typeFilter.value;
      refs.search.dispatchEvent(new Event('input'));
    });
    refs.statusFilter.addEventListener('change', () => {
      state.statusFilter = refs.statusFilter.value;
      refs.search.dispatchEvent(new Event('input'));
    });
    refs.type.addEventListener('change', () => setPersonType(refs.type.value));
    refs.document.addEventListener('input', () => {
      refs.document.value = formatDocument(refs.document.value, refs.type.value);
    });
    refs.phone.addEventListener('input', formatPhoneInput);
    refs.postalCode.addEventListener('input', formatPostalCode);
    refs.state.addEventListener('input', () => {
      refs.state.value = refs.state.value.replace(/[^a-z]/gi, '').slice(0, 2).toUpperCase();
    });
    refs.table.addEventListener('click', handleTableClick);
    refs.table.addEventListener('keydown', handleTableKeydown);
    refs.form.addEventListener('submit', saveClient);
    document.getElementById('btnNovoCliente').addEventListener('click', openCreateModal);
    document.getElementById('btnNovaLocacao').addEventListener('click', () => {
      if (state.selectedId == null) return;
      window.location.href = `locacoes.html?clienteId=${encodeURIComponent(state.selectedId)}`;
    });
    document.getElementById('btnLogout').addEventListener('click', () => window.GR.auth.sair());
    document.querySelectorAll('[data-close-modal]').forEach((button) => button.addEventListener('click', closeModal));
    refs.modal.addEventListener('click', (event) => {
      if (event.target === refs.modal) closeModal();
    });
    refs.modal.addEventListener('close', () => {
      if (state.lastFocusedElement?.isConnected) state.lastFocusedElement.focus();
    });
    document.querySelectorAll('[data-close-confirm]').forEach((button) => button.addEventListener('click', () => {
      refs.confirmDialog.close();
      state.pendingDeleteId = null;
    }));
    document.getElementById('confirmarAcaoInativar').addEventListener('click', confirmDelete);
    refs.confirmDialog.addEventListener('click', (event) => {
      if (event.target === refs.confirmDialog) {
        refs.confirmDialog.close();
        state.pendingDeleteId = null;
      }
    });
    refs.confirmDialog.addEventListener('close', () => {
      state.pendingDeleteId = null;
    });
  }

  bindEvents();
  renderEmptyDetails();
  loadProfile();
  loadClients({ preserveSelection: false });
})();
