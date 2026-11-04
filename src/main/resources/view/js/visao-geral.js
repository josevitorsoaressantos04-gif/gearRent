(() => {
  // Exige uma sessão válida antes de inicializar os dados do painel.
  const tokenKey = 'gearrent_token';
  const token = sessionStorage.getItem(tokenKey);
  if (!token) {
    window.location.replace('login.html');
    return;
  }
  // Restaura o tema escolhido pelo usuário em visitas anteriores.
  if (localStorage.getItem('gearRentTheme') === 'dark') document.body.dataset.theme = 'dark';

  // Mantém paginação, resultados atuais e filtros aplicados durante o uso da tela.
  const pageSize = 10;
  const messages = new Set();
  const state = {
    page: 0,
    totalPages: 0,
    totalElements: 0,
    rows: [],
    filters: {},
  };

  // Guarda referências aos elementos usados pelas funções e eventos do painel.
  const elements = {
    toggleFilters: document.getElementById('toggleFiltros'),
    filterPanel: document.getElementById('filtroAvancado'),
    category: document.getElementById('filtroCategoria'),
    model: document.getElementById('filtroModelo'),
    equipmentStatus: document.getElementById('filtroStatusEquipamento'),
    rentalStatus: document.getElementById('filtroStatusLocacao'),
    client: document.getElementById('filtroCliente'),
    assetNumber: document.getElementById('filtroPatrimonio'),
    startDate: document.getElementById('filtroDataInicio'),
    endDate: document.getElementById('filtroDataFim'),
    search: document.getElementById('buscaRapida'),
    tableBody: document.getElementById('locacoesTableBody'),
    pageLabel: document.getElementById('paginaAtual'),
    previousPage: document.getElementById('paginaAnterior'),
    nextPage: document.getElementById('proximaPagina'),
    resultCount: document.getElementById('totalResultados'),
    message: document.getElementById('mensagemDashboard'),
    error: document.getElementById('erroDashboard'),
    dialog: document.getElementById('locacaoDialog'),
    dialogContent: document.getElementById('locacaoDialogContent'),
    dialogTitle: document.getElementById('dialogTitle'),
    editNotice: document.getElementById('avisoEdicao'),
  };

  // Restaura e persiste as preferências de exibição dos cards desta página.
  function setupKpiVisibility(pageKey) {
    const storagePrefix = `gearRentHiddenKpi:${pageKey}:`;
    const checkboxes = document.querySelectorAll('[data-kpi-visibility]');
    checkboxes.forEach((checkbox) => {
      const cardKey = checkbox.dataset.kpiVisibility;
      const card = document.querySelector(`[data-kpi-card="${cardKey}"]`);
      if (!card) return;

      checkbox.checked = localStorage.getItem(`${storagePrefix}${cardKey}`) !== 'true';
      card.hidden = !checkbox.checked;
      checkbox.addEventListener('change', () => {
        card.hidden = !checkbox.checked;
        localStorage.setItem(`${storagePrefix}${cardKey}`, String(!checkbox.checked));
      });
    });
  }

  // Encerra a sessão usando o comportamento compartilhado de autenticação.
  function logout() {
    window.GR.auth.sair();
  }

  // Consulta a API e converte erros HTTP ou de conexão em mensagens legíveis.
  async function getJSON(path) {
    try {
      const response = await window.apiClient.get(path);
      return response.data;
    } catch (error) {
      if (error.response?.status === 401) {
        logout();
        throw new Error('Sua sessão expirou. Entre novamente.');
      }

      const messagesFromApi = error.response?.data?.mensagens;
      if (Array.isArray(messagesFromApi) && messagesFromApi.length > 0) {
        throw new Error(messagesFromApi.join(' '));
      }
      if (error.response) {
        throw new Error(`Não foi possível consultar os dados (HTTP ${error.response.status}).`);
      }
      throw new Error('Não foi possível conectar à API. Verifique a conexão e tente novamente.');
    }
  }
  // Atualiza a área de erro com todas as mensagens registradas.
  function updateMessages() {
    elements.error.textContent = [...messages].join(' ');
    elements.error.hidden = messages.size === 0;
  }

  // Registra um erro sem apagar mensagens anteriores da mesma operação.
  function reportError(error) {
    messages.add(error.message || 'Ocorreu um erro ao carregar os dados.');
    updateMessages();
  }

  // Limpa os erros apresentados antes de iniciar uma nova consulta.
  function clearErrors() {
    messages.clear();
    updateMessages();
  }

  // Gera as iniciais usadas nos avatares a partir do nome do usuário.
  function initials(name) {
    return name
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part.charAt(0).toLocaleUpperCase('pt-BR'))
      .join('');
  }

  // Lê o identificador do usuário autenticado no payload do token JWT.
  function getTokenSubject() {
    try {
      const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      return JSON.parse(window.atob(payload)).sub;
    } catch {
      return null;
    }
  }

  // Consulta os usuários e preenche nome, base e avatares do perfil atual.
  async function loadProfile() {
    const users = await getJSON('/usuarios');
    if (!Array.isArray(users)) {
      throw new Error('A resposta de usuários está em um formato inesperado.');
    }

    const currentUserId = getTokenSubject();
    const currentUser = users.find((user) => String(user.id) === String(currentUserId));
    if (!currentUser || !currentUser.nome) {
      throw new Error('Não foi possível identificar seu perfil autenticado.');
    }

    const name = currentUser.nome;
    document.getElementById('userNameSidebar').textContent = name;
    document.getElementById('userNameTopbar').textContent = name;
    document.getElementById('baseNameSidebar').textContent = `Base de ${name}`;
    document.getElementById('avatarSidebar').textContent = initials(name);
    document.getElementById('avatarTopbar').textContent = initials(name);
  }

  // Transforma valores enumerados em rótulos legíveis para os filtros.
  function readableEnum(value) {
    return value
      .toLocaleLowerCase('pt-BR')
      .replace(/_/g, ' ')
      .replace(/(^|\s)\S/g, (letter) => letter.toLocaleUpperCase('pt-BR'));
  }

  // Substitui as opções de um seletor pelo placeholder e pelos valores recebidos.
  function addOptions(select, values, placeholder, labelForValue = readableEnum) {
    select.replaceChildren(new Option(placeholder, ''));
    values.forEach((value) => select.add(new Option(labelForValue(value), value)));
  }

  // Busca na API as opções disponíveis para categoria, modelo e cliente.
  async function loadFilterOptions() {
    const options = await getJSON('/visao-geral/filtros-opcoes');
    const categories = options.filtros?.categorias;
    const models = options.filtros?.modelos;
    const clients = options.clientes;
    if (!Array.isArray(categories) || !Array.isArray(models) || !Array.isArray(clients)) {
      throw new Error('As opções de filtros vieram em um formato inesperado.');
    }

    addOptions(elements.category, categories, 'Todas');
    addOptions(elements.model, models, 'Todos', (model) => model);
    elements.client.replaceChildren(new Option('Todos', ''));
    clients.forEach((client) => {
      const label = `CLI-${String(client.codigoSequencial).padStart(4, '0')} — ${client.nome}`;
      elements.client.add(new Option(label, String(client.id)));
    });
  }

  // Formata valores numéricos como moeda brasileira, tratando dados ausentes.
  function formatCurrency(value) {
    const amount = Number(value);
    if (value === null || value === undefined || value === '' || !Number.isFinite(amount)) {
      return 'Não informado';
    }
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(amount);
  }

  // Formata quantidades inteiras e rejeita valores ausentes ou inválidos.
  function formatCount(value) {
    const count = Number(value);
    return value !== null && value !== undefined && value !== ''
      && Number.isSafeInteger(count) && count >= 0
      ? new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 0 }).format(count)
      : '—';
  }

  // Carrega os indicadores operacionais gerais exibidos nos cards.
  async function loadSummary() {
    const summary = await getJSON('/visao-geral/resumo');
    document.getElementById('kpiTotal').textContent = formatCount(summary.totalLocacoes);
    document.getElementById('kpiAtrasadas').textContent = formatCount(summary.atrasadas);
  }

  // Consulta o resumo financeiro usando o período atualmente aplicado.
  async function loadFinancialSummary() {
    const query = new URLSearchParams();
    ['dataInicio', 'dataFim'].forEach((key) => {
      if (state.filters[key]) query.set(key, state.filters[key]);
    });
    const queryString = query.toString();
    const suffix = queryString ? `?${queryString}` : '';
    const summary = await getJSON(`/financeiro/resumo${suffix}`);
    if (!summary || summary.receitaTotal === undefined || !summary.valoresEmAberto) {
      throw new Error('A resposta do resumo financeiro veio em um formato inesperado.');
    }
    document.getElementById('kpiFaturamento').textContent = formatCurrency(summary.receitaTotal);
    document.getElementById('kpiAndamento').textContent = formatCount(
      summary.valoresEmAberto.quantidadeEmAndamento
    );
  }

  // Monta os parâmetros de paginação e filtros para a listagem de locações.
  function buildFilterQuery() {
    const query = new URLSearchParams({
      page: String(state.page),
      size: String(pageSize),
    });
    Object.entries(state.filters).forEach(([key, value]) => {
      if (value) query.set(key, value);
    });
    return query.toString();
  }

  // Retorna a busca textual normalizada para comparação sem diferença de caixa.
  function currentSearch() {
    return elements.search.value.trim().toLocaleLowerCase('pt-BR');
  }

  // Filtra localmente a página carregada por cliente, status ou equipamento.
  function visibleRows() {
    const searchTerm = currentSearch();
    if (!searchTerm) return state.rows;
    return state.rows.filter((rental) => {
      const equipmentText = (rental.itens || [])
        .map((item) => [item.nome, item.modelo, item.numeroPatrimonio].filter(Boolean).join(' '))
        .join(' ');
      const searchable = [
        rental.cliente?.nome,
        rental.cliente?.codigoSequencial,
        rental.status,
        equipmentText,
      ].filter(Boolean).join(' ').toLocaleLowerCase('pt-BR');
      return searchable.includes(searchTerm);
    });
  }

  // Cria uma célula de tabela e define seu texto com segurança.
  function makeCell(text, className = '') {
    const cell = document.createElement('td');
    if (className) cell.className = className;
    cell.textContent = text;
    return cell;
  }

  // Associa cada status da locação à classe visual correspondente.
  function statusClass(status) {
    const classes = {
      EM_ANDAMENTO: 'status--andamento',
      CONCLUIDA: 'status--concluida',
      ATRASADA: 'status--atrasada',
      CANCELADA: 'status--cancelada',
    };
    return classes[status] || '';
  }

  // Cria um botão de ação acessível e vinculado à locação indicada.
  function makeAction(label, icon, rentalId, action) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'icon-button';
    button.setAttribute('aria-label', label);
    button.dataset.rentalId = rentalId;
    button.dataset.action = action;
    button.textContent = icon;
    return button;
  }

  // Desenha as locações visíveis e atualiza contador e controles de paginação.
  function renderRows() {
    const rows = visibleRows();
    elements.tableBody.replaceChildren();

    if (rows.length === 0) {
      const emptyRow = document.createElement('tr');
      const emptyCell = makeCell(
        state.rows.length ? 'Nenhum resultado corresponde à busca nesta página.' : 'Nenhuma locação encontrada para os filtros selecionados.',
        'table-state',
      );
      emptyCell.colSpan = 6;
      emptyRow.append(emptyCell);
      elements.tableBody.append(emptyRow);
    } else {
      rows.forEach((rental) => {
        const row = document.createElement('tr');
        const code = rental.cliente?.codigoSequencial;
        const items = Array.isArray(rental.itens) ? rental.itens : [];
        const itemCell = document.createElement('td');
        const itemContent = document.createElement('div');
        itemContent.className = 'cell-with-action';
        const itemText = document.createElement('span');
        itemText.textContent = items.length > 1
          ? `${items.length} equipamentos`
          : items[0]?.nome || 'Item não informado';
        itemContent.append(itemText);
        if (items.length > 1) {
          itemContent.append(makeAction('Visualizar todos os itens da locação', '👁️', rental.id, 'view'));
        }
        itemCell.append(itemContent);

        const valueCell = document.createElement('td');
        const dayCount = document.createElement('strong');
        dayCount.textContent = `${rental.diarias ?? 0} diária(s)`;
        const dailyValue = document.createElement('span');
        dailyValue.className = 'unavailable-value';
        dailyValue.textContent = 'Soma das diárias indisponível';
        const chargedValue = document.createElement('small');
        chargedValue.textContent = `Total cobrado: ${formatCurrency(rental.valorFinalCobrado)}`;
        valueCell.append(dayCount, dailyValue, chargedValue);

        const statusCell = document.createElement('td');
        const badge = document.createElement('span');
        badge.className = `status ${statusClass(rental.status)}`;
        badge.textContent = rental.status || 'Não informado';
        statusCell.append(badge);

        const actionsCell = document.createElement('td');
        actionsCell.append(makeAction('Abrir dados da locação', '✏️', rental.id, 'edit'));
        row.append(
          makeCell(rental.cliente?.nome || 'Cliente não informado'),
          makeCell(code == null ? '—' : `CLI-${String(code).padStart(4, '0')}`),
          itemCell,
          valueCell,
          statusCell,
          actionsCell,
        );
        elements.tableBody.append(row);
      });
    }

    elements.resultCount.textContent = `${rows.length} nesta página · ${state.totalElements} no total`;
    elements.pageLabel.textContent = `Página ${state.page + 1} de ${Math.max(state.totalPages, 1)}`;
    elements.previousPage.disabled = state.page <= 0;
    elements.nextPage.disabled = state.totalPages === 0 || state.page >= state.totalPages - 1;
  }

  // Busca a página atual de locações e apresenta resultados ou erro na interface.
  async function loadRentals() {
    elements.message.textContent = 'Carregando locações…';
    elements.message.hidden = false;
    try {
      const page = await getJSON(`/visao-geral/locacoes?${buildFilterQuery()}`);
      if (!Array.isArray(page.content)) {
        throw new Error('A resposta de locações veio em um formato inesperado.');
      }
      state.rows = page.content;
      state.totalPages = Number(page.totalPages) || 0;
      state.totalElements = Number(page.totalElements) || 0;
      renderRows();
      elements.message.hidden = true;
    } catch (error) {
      const row = document.createElement('tr');
      const cell = makeCell('Não foi possível carregar as locações. Verifique a mensagem acima e tente novamente.', 'table-state');
      cell.colSpan = 6;
      row.append(cell);
      elements.tableBody.replaceChildren(row);
      elements.message.hidden = true;
      reportError(error);
    }
  }

  // Adiciona um par de título e valor aos detalhes de uma locação.
  function appendDefinition(parent, term, description) {
    const wrapper = document.createElement('div');
    const title = document.createElement('dt');
    const value = document.createElement('dd');
    title.textContent = term;
    value.textContent = description;
    wrapper.append(title, value);
    parent.append(wrapper);
  }

  // Abre o diálogo com os dados da locação e seus equipamentos associados.
  function openRentalDialog(rentalId, action) {
    const rental = state.rows.find((row) => String(row.id) === String(rentalId));
    if (!rental) return;

    const items = Array.isArray(rental.itens) ? rental.itens : [];
    elements.dialogContent.replaceChildren();
    elements.dialogTitle.textContent = action === 'edit' ? 'Dados da locação' : 'Itens da locação';
    elements.editNotice.hidden = action !== 'edit';

    const details = document.createElement('dl');
    details.className = 'dialog-details';
    appendDefinition(details, 'Cliente', rental.cliente?.nome || 'Não informado');
    appendDefinition(details, 'Código do cliente', rental.cliente?.codigoSequencial == null
      ? 'Não informado'
      : `CLI-${String(rental.cliente.codigoSequencial).padStart(4, '0')}`);
    appendDefinition(details, 'Status', rental.status || 'Não informado');
    appendDefinition(details, 'Diárias', String(rental.diarias ?? 0));
    appendDefinition(details, 'Total cobrado', formatCurrency(rental.valorFinalCobrado));
    elements.dialogContent.append(details);

    const listHeading = document.createElement('h3');
    listHeading.textContent = 'Equipamentos';
    const list = document.createElement('ul');
    list.className = 'dialog-item-list';
    items.forEach((item) => {
      const listItem = document.createElement('li');
      const detailsText = [
        item.nome,
        item.modelo,
        item.numeroPatrimonio ? `Patrimônio ${item.numeroPatrimonio}` : '',
      ].filter(Boolean).join(' · ');
      listItem.textContent = detailsText || 'Equipamento não informado';
      list.append(listItem);
    });
    elements.dialogContent.append(listHeading, list);
    elements.dialog.showModal();
  }

  // Valida e aplica os filtros; atualiza a listagem e a receita do período.
  function applyFilters() {
    clearErrors();
    if (elements.startDate.value && elements.endDate.value
      && elements.startDate.value > elements.endDate.value) {
      reportError(new Error('A data inicial não pode ser posterior à data final.'));
      return;
    }

    state.page = 0;
    state.filters = {
      categoria: elements.category.value,
      modelo: elements.model.value,
      numeroPatrimonio: elements.assetNumber.value.trim(),
      status: elements.rentalStatus.value,
      clienteId: elements.client.value,
      dataInicio: elements.startDate.value,
      dataFim: elements.endDate.value,
    };
    Promise.all([
      loadRentals(),
      loadFinancialSummary().catch(reportError),
    ]);
  }

  // Preenche as datas conforme o período rápido escolhido pelo usuário.
  function setPeriod(period) {
    const today = new Date();
    let start;
    let end;
    if (period === 'current-month') {
      start = new Date(today.getFullYear(), today.getMonth(), 1);
      end = new Date(today.getFullYear(), today.getMonth() + 1, 0);
    } else if (period === 'previous-month') {
      start = new Date(today.getFullYear(), today.getMonth() - 1, 1);
      end = new Date(today.getFullYear(), today.getMonth(), 0);
    } else if (period === 'current-year') {
      start = new Date(today.getFullYear(), 0, 1);
      end = new Date(today.getFullYear(), 11, 31);
    } else {
      start = new Date(today.getFullYear() - 1, 0, 1);
      end = new Date(today.getFullYear() - 1, 11, 31);
    }

    const toDateInput = (date) => [
      date.getFullYear(),
      String(date.getMonth() + 1).padStart(2, '0'),
      String(date.getDate()).padStart(2, '0'),
    ].join('-');
    elements.startDate.value = toDateInput(start);
    elements.endDate.value = toDateInput(end);
    document.querySelectorAll('[data-period]').forEach((button) => {
      button.classList.toggle('chip--active', button.dataset.period === period);
    });
  }

  // Expande ou recolhe o painel que contém os filtros avançados.
  elements.toggleFilters.addEventListener('click', () => {
    const expanded = elements.toggleFilters.getAttribute('aria-expanded') === 'true';
    elements.filterPanel.hidden = expanded;
    elements.toggleFilters.setAttribute('aria-expanded', String(!expanded));
  });
  // Liga as ações de aplicar e limpar aos controles de filtro da tela.
  document.getElementById('aplicarFiltros').addEventListener('click', applyFilters);
  document.getElementById('limparFiltros').addEventListener('click', () => {
    document.getElementById('filtroAvancado').querySelectorAll('select').forEach((select) => {
      if (!select.disabled) select.value = '';
    });
    elements.assetNumber.value = '';
    elements.startDate.value = '';
    elements.endDate.value = '';
    elements.search.value = '';
    document.querySelectorAll('[data-period]').forEach((button) => button.classList.remove('chip--active'));
    applyFilters();
  });
  // Permite selecionar um dos períodos predefinidos para os filtros de data.
  document.querySelectorAll('[data-period]').forEach((button) => {
    button.addEventListener('click', () => setPeriod(button.dataset.period));
  });
  // Atualiza a busca local e a navegação entre páginas da listagem.
  elements.search.addEventListener('input', renderRows);
  elements.previousPage.addEventListener('click', () => {
    if (state.page > 0) {
      state.page -= 1;
      loadRentals();
    }
  });
  elements.nextPage.addEventListener('click', () => {
    if (state.page < state.totalPages - 1) {
      state.page += 1;
      loadRentals();
    }
  });
  // Abre os detalhes da locação correspondente ao botão acionado na tabela.
  elements.tableBody.addEventListener('click', (event) => {
    const button = event.target.closest('button[data-action]');
    if (button) openRentalDialog(button.dataset.rentalId, button.dataset.action);
  });
  // Fecha o diálogo pelo botão ou quando o usuário clica em sua área externa.
  document.getElementById('fecharDialog').addEventListener('click', () => elements.dialog.close());
  elements.dialog.addEventListener('click', (event) => {
    if (event.target === elements.dialog) elements.dialog.close();
  });
  // Registra a ação de sair e inicializa os cards personalizados.
  document.getElementById('btnLogout').addEventListener('click', logout);

  setupKpiVisibility('overview');

  // Carrega em paralelo perfil, filtros, indicadores e locações da página inicial.
  Promise.all([
    loadProfile().catch(reportError),
    loadFilterOptions().catch(reportError),
    loadSummary().catch(reportError),
    loadFinancialSummary().catch(reportError),
    loadRentals(),
  ]);
})();
