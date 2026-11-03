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

  const pageSize = 10;
  const state = {
    page: 0,
    totalPages: 0,
    totalElements: 0,
    rows: [],
    filters: {},
    clients: [],
    equipment: [],
  };
  const refs = {
    search: document.getElementById('rentalSearch'),
    status: document.getElementById('rentalStatusFilter'),
    start: document.getElementById('rentalStartFilter'),
    end: document.getElementById('rentalEndFilter'),
    table: document.getElementById('rentalsTableBody'),
    count: document.getElementById('rentalResultCount'),
    page: document.getElementById('rentalPageLabel'),
    previous: document.getElementById('previousRentalPage'),
    next: document.getElementById('nextRentalPage'),
    revenue: document.getElementById('rentalRevenue'),
    active: document.getElementById('rentalActiveCount'),
    message: document.getElementById('rentalMessage'),
    error: document.getElementById('rentalError'),
    detailsDialog: document.getElementById('rentalDetailsDialog'),
    detailsContent: document.getElementById('rentalDialogContent'),
    detailsTitle: document.getElementById('rentalDialogTitle'),
    actionDialog: document.getElementById('rentalActionDialog'),
    actionTitle: document.getElementById('rentalActionTitle'),
    actionContent: document.getElementById('rentalActionContent'),
    returnDateGroup: document.getElementById('returnDateGroup'),
    returnDate: document.getElementById('actualReturnDate'),
    newDialog: document.getElementById('newRentalDialog'),
    newClient: document.getElementById('newRentalClient'),
    equipmentList: document.getElementById('availableEquipmentList'),
    newError: document.getElementById('newRentalError'),
    startDate: document.getElementById('newRentalStart'),
    dueDate: document.getElementById('newRentalDue'),
    estimateDays: document.getElementById('estimatedRentalDays'),
    estimateTotal: document.getElementById('estimatedRentalTotal'),
  };

  function getErrorMessage(error) {
    if (error.response?.status === 401) {
      window.GR.auth.sair();
      return 'Sua sessão expirou. Entre novamente.';
    }
    const apiMessages = error.response?.data?.mensagens;
    if (Array.isArray(apiMessages) && apiMessages.length) return apiMessages.join(' ');
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

  function currency(value) {
    if (value === null || value === undefined || value === '') return 'Não informado';
    const amount = Number(value);
    return Number.isFinite(amount)
      ? new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(amount)
      : 'Não informado';
  }

  function formatDate(value) {
    if (!value) return '—';
    const match = String(value).match(/^(\d{4})-(\d{2})-(\d{2})/);
    if (match) return `${match[3]}/${match[2]}/${match[1]}`;
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? '—' : new Intl.DateTimeFormat('pt-BR').format(date);
  }

  function initials(name) {
    return String(name || 'GR').trim().split(/\s+/).slice(0, 2)
      .map((part) => part.charAt(0).toLocaleUpperCase('pt-BR')).join('') || 'GR';
  }

  function tokenSubject() {
    try {
      const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      return JSON.parse(window.atob(payload)).sub;
    } catch {
      return null;
    }
  }

  async function loadProfile() {
    try {
      const users = await getJSON('/usuarios');
      const current = Array.isArray(users)
        ? users.find((user) => String(user.id) === String(tokenSubject()))
        : null;
      if (!current?.nome) throw new Error('Não foi possível identificar o usuário autenticado.');
      document.getElementById('userNameSidebar').textContent = current.nome;
      document.getElementById('userNameTopbar').textContent = current.nome;
      document.getElementById('baseNameSidebar').textContent = `Base de ${current.nome}`;
      document.getElementById('avatarSidebar').textContent = initials(current.nome);
      document.getElementById('avatarTopbar').textContent = initials(current.nome);
    } catch (error) {
      setError(`${getErrorMessage(error)} Não foi possível carregar o perfil da base.`);
    }
  }

  function currentRows() {
    const term = refs.search.value.trim().toLocaleLowerCase('pt-BR');
    if (!term) return state.rows;
    return state.rows.filter((rental) => {
      const equipmentText = (Array.isArray(rental.itens) ? rental.itens : [])
        .map((item) => [item.nome, item.modelo, item.numeroPatrimonio].filter(Boolean).join(' '))
        .join(' ');
      const text = [
        rental.id,
        rental.codigoSequencial,
        rental.cliente?.nome,
        rental.status,
        equipmentText,
      ].filter(Boolean).join(' ').toLocaleLowerCase('pt-BR');
      return text.includes(term);
    });
  }

  function makeCell(text, className = '') {
    const cell = document.createElement('td');
    cell.textContent = text || '—';
    if (className) cell.className = className;
    return cell;
  }

  function statusClass(status) {
    return ({
      EM_ANDAMENTO: 'status--andamento',
      CONCLUIDA: 'status--concluida',
      ATRASADA: 'status--atrasada',
      CANCELADA: 'status--cancelada',
    })[status] || '';
  }

  function makeAction(label, icon, rental, action, disabled = false) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'icon-button';
    button.textContent = icon;
    button.dataset.action = action;
    button.dataset.rentalId = String(rental.id);
    button.setAttribute('aria-label', `${label}, locação ${rental.id}`);
    button.title = disabled ? `${label}: indisponível na API atual` : label;
    button.disabled = disabled;
    return button;
  }

  function renderRows() {
    const rows = currentRows();
    refs.table.replaceChildren();
    if (!rows.length) {
      const row = document.createElement('tr');
      const cell = makeCell(
        state.rows.length ? 'Nenhuma locação corresponde à busca nesta página.' : 'Nenhuma locação encontrada para os filtros selecionados.',
        'table-state',
      );
      cell.colSpan = 8;
      row.append(cell);
      refs.table.append(row);
    } else {
      rows.forEach((rental) => {
        const row = document.createElement('tr');
        const items = Array.isArray(rental.itens) ? rental.itens : [];
        const equipmentCell = makeCell(items.length
          ? items.map((item) => [item.nome, item.modelo].filter(Boolean).join(' · ')).join(', ')
          : 'Equipamento não informado');
        const statusCell = document.createElement('td');
        const badge = document.createElement('span');
        badge.className = `status ${statusClass(rental.status)}`;
        badge.textContent = rental.status || 'Não informado';
        statusCell.append(badge);

        const actions = document.createElement('td');
        actions.className = 'rental-actions';
        actions.append(
          makeAction('Ver detalhes do contrato', '👁️', rental, 'details'),
          makeAction('Registrar devolução', '📥', rental, 'return', rental.status !== 'EM_ANDAMENTO'),
          makeAction('Cancelar locação', '🚫', rental, 'cancel', rental.status !== 'EM_ANDAMENTO'),
        );
        row.append(
          makeCell(`ID ${rental.id ?? '—'}`),
          makeCell(rental.cliente?.nome || 'Cliente não informado'),
          equipmentCell,
          makeCell(formatDate(rental.dataRetirada)),
          makeCell(formatDate(rental.dataPrevista)),
          makeCell(currency(rental.valorFinalCobrado)),
          statusCell,
          actions,
        );
        refs.table.append(row);
      });
    }

    refs.count.textContent = `${rows.length} nesta página · ${state.totalElements} no total`;
    refs.page.textContent = `Página ${state.page + 1} de ${Math.max(state.totalPages, 1)}`;
    refs.previous.disabled = state.page <= 0;
    refs.next.disabled = state.totalPages === 0 || state.page >= state.totalPages - 1;
  }

  function queryString() {
    const params = new URLSearchParams({ page: String(state.page), size: String(pageSize) });
    Object.entries(state.filters).forEach(([key, value]) => {
      if (value) params.set(key, value);
    });
    return params.toString();
  }

  async function loadRentals() {
    setError('');
    setMessage('Carregando locações…');
    try {
      const page = await getJSON(`/visao-geral/locacoes?${queryString()}`);
      if (!page || !Array.isArray(page.content)) throw new Error('A resposta de locações veio em formato inesperado.');
      state.rows = page.content;
      state.totalPages = Number(page.totalPages) || 0;
      state.totalElements = Number(page.totalElements) || 0;
      renderRows();
      setMessage('');
    } catch (error) {
      state.rows = [];
      state.totalPages = 0;
      state.totalElements = 0;
      renderRows();
      setMessage('');
      setError(`${getErrorMessage(error)} A consulta de listagem utiliza GET /visao-geral/locacoes.`);
    }
  }

  async function loadSummary() {
    try {
      const summary = await getJSON('/visao-geral/resumo');
      refs.revenue.textContent = currency(summary.faturamentoTotal);
      refs.active.textContent = summary.emAndamento ?? '—';
    } catch (error) {
      refs.revenue.textContent = 'Indisponível';
      refs.active.textContent = 'Indisponível';
      setError(`${getErrorMessage(error)} Os indicadores financeiros não puderam ser carregados.`);
    }
  }

  function appendDefinition(parent, term, value) {
    const wrapper = document.createElement('div');
    const title = document.createElement('dt');
    const description = document.createElement('dd');
    title.textContent = term;
    description.textContent = value == null || value === '' ? 'Não informado' : String(value);
    wrapper.append(title, description);
    parent.append(wrapper);
  }

  function openDetails(rental) {
    refs.detailsTitle.textContent = `Detalhes da locação · ID ${rental.id}`;
    refs.detailsContent.replaceChildren();
    const details = document.createElement('dl');
    details.className = 'dialog-details';
    appendDefinition(details, 'Cliente', rental.cliente?.nome);
    appendDefinition(details, 'Status', rental.status);
    appendDefinition(details, 'Data de retirada', formatDate(rental.dataRetirada));
    appendDefinition(details, 'Devolução prevista', formatDate(rental.dataPrevista));
    appendDefinition(details, 'Devolução real', formatDate(rental.dataReal));
    appendDefinition(details, 'Diárias calculadas pela API', rental.diarias);
    appendDefinition(details, 'Valor final cobrado', currency(rental.valorFinalCobrado));
    refs.detailsContent.append(details);

    const title = document.createElement('h3');
    title.textContent = 'Equipamentos vinculados';
    const list = document.createElement('ul');
    list.className = 'dialog-item-list';
    (Array.isArray(rental.itens) ? rental.itens : []).forEach((item) => {
      const li = document.createElement('li');
      li.textContent = [item.nome, item.modelo, item.numeroPatrimonio ? `Patrimônio ${item.numeroPatrimonio}` : '']
        .filter(Boolean).join(' · ') || 'Equipamento não informado';
      list.append(li);
    });
    if (!list.childElementCount) {
      const li = document.createElement('li');
      li.textContent = 'A API não retornou itens para esta locação.';
      list.append(li);
    }
    refs.detailsContent.append(title, list);

    const note = document.createElement('p');
    note.className = 'form-notice';
    note.textContent = 'A API fornece apenas os dados sintéticos da locação; não há documento de contrato para download.';
    refs.detailsContent.append(note);
    refs.detailsDialog.showModal();
  }

  function showUnavailableAction(rental, action) {
    const isReturn = action === 'return';
    refs.actionTitle.textContent = isReturn ? 'Registrar devolução' : 'Cancelar locação';
    refs.actionContent.replaceChildren();
    const notice = document.createElement('p');
    notice.className = 'form-notice';
    notice.textContent = isReturn
      ? `A API não possui rota para registrar a devolução da locação ${rental.id}, atualizar seu status ou calcular diárias e multa. Nenhuma alteração foi feita.`
      : `A API não possui rota para cancelar a locação ${rental.id}. Nenhuma alteração foi feita.`;
    refs.actionContent.append(notice);
    refs.returnDateGroup.hidden = !isReturn;
    refs.returnDate.value = '';
    refs.actionDialog.showModal();
  }

  function addEquipmentOption(equipment) {
    const label = document.createElement('label');
    label.className = 'equipment-option';
    const checkbox = document.createElement('input');
    checkbox.type = 'checkbox';
    checkbox.name = 'equipment';
    checkbox.value = String(equipment.id);
    checkbox.dataset.dailyValue = String(equipment.valorDiaria ?? '');
    const description = document.createElement('span');
    const dailyValue = Number(equipment.valorDiaria);
    const priceText = Number.isFinite(dailyValue) ? currency(dailyValue) : 'diária não informada';
    description.textContent = `${equipment.nome || 'Equipamento'}${equipment.modelo ? ` · ${equipment.modelo}` : ''} — ${priceText}/dia`;
    label.append(checkbox, description);
    refs.equipmentList.append(label);
  }

  async function loadNewRentalOptions() {
    refs.newError.hidden = true;
    refs.newClient.replaceChildren(new Option('Carregando clientes…', ''));
    refs.equipmentList.textContent = 'Carregando equipamentos…';
    const [clientsResult, equipmentResult] = await Promise.allSettled([
      getJSON('/clientes'),
      getJSON('/equipamentos/disponiveis'),
    ]);

    refs.newClient.replaceChildren(new Option('Selecione um cliente', ''));
    if (clientsResult.status === 'fulfilled' && Array.isArray(clientsResult.value)) {
      state.clients = clientsResult.value;
      state.clients.forEach((client) => {
        const code = client.codigoSequencial == null ? '' : `CLI-${String(client.codigoSequencial).padStart(4, '0')} · `;
        refs.newClient.add(new Option(`${code}${client.nome || 'Cliente'}`, String(client.id)));
      });
    } else {
      state.clients = [];
      const error = clientsResult.status === 'rejected' ? clientsResult.reason : new Error('Formato de clientes inesperado.');
      reportNewRentalError(`Clientes: ${getErrorMessage(error)}`);
    }

    refs.equipmentList.replaceChildren();
    if (equipmentResult.status === 'fulfilled' && Array.isArray(equipmentResult.value)) {
      state.equipment = equipmentResult.value;
      if (!state.equipment.length) {
        refs.equipmentList.textContent = 'Nenhum equipamento disponível para locação.';
      } else {
        state.equipment.forEach(addEquipmentOption);
      }
    } else {
      state.equipment = [];
      refs.equipmentList.textContent = 'Não foi possível carregar os equipamentos disponíveis.';
      const error = equipmentResult.status === 'rejected' ? equipmentResult.reason : new Error('Formato de equipamentos inesperado.');
      reportNewRentalError(`Equipamentos: ${getErrorMessage(error)}`);
    }
    updateEstimate();
  }

  function reportNewRentalError(message) {
    refs.newError.textContent = refs.newError.hidden ? message : `${refs.newError.textContent} ${message}`;
    refs.newError.hidden = false;
  }

  function dateValueToUtcDay(value) {
    if (!value) return null;
    const [year, month, day] = value.split('-').map(Number);
    return Date.UTC(year, month - 1, day);
  }

  function updateEstimate() {
    const start = dateValueToUtcDay(refs.startDate.value);
    const due = dateValueToUtcDay(refs.dueDate.value);
    const selected = [...refs.equipmentList.querySelectorAll('input[name="equipment"]:checked')];
    if (start == null || due == null || due < start || !selected.length) {
      refs.estimateDays.textContent = 'Diárias estimadas: —';
      refs.estimateTotal.textContent = 'Total estimado: —';
      return;
    }
    const days = Math.max(1, Math.ceil((due - start) / 86400000));
    const dailyValues = selected.map((input) => Number(input.dataset.dailyValue));
    if (dailyValues.some((value) => !Number.isFinite(value))) {
      refs.estimateDays.textContent = `Diárias estimadas: ${days}`;
      refs.estimateTotal.textContent = 'Total estimado: diária não informada';
      return;
    }
    const dailyTotal = dailyValues.reduce((total, value) => total + value, 0);
    refs.estimateDays.textContent = `Diárias estimadas: ${days}`;
    refs.estimateTotal.textContent = `Total estimado: ${currency(days * dailyTotal)}`;
  }

  function applyFilters() {
    if (refs.start.value && refs.end.value && refs.start.value > refs.end.value) {
      setError('A data inicial não pode ser posterior à data final.');
      return;
    }
    state.page = 0;
    state.filters = {
      status: refs.status.value,
      dataInicio: refs.start.value,
      dataFim: refs.end.value,
    };
    loadRentals();
  }

  document.getElementById('btnLogout').addEventListener('click', () => window.GR.auth.sair());
  document.getElementById('applyRentalFilters').addEventListener('click', applyFilters);
  document.getElementById('clearRentalFilters').addEventListener('click', () => {
    refs.search.value = '';
    refs.status.value = '';
    refs.start.value = '';
    refs.end.value = '';
    state.filters = {};
    state.page = 0;
    loadRentals();
  });
  refs.search.addEventListener('input', renderRows);
  refs.previous.addEventListener('click', () => {
    if (state.page > 0) {
      state.page -= 1;
      loadRentals();
    }
  });
  refs.next.addEventListener('click', () => {
    if (state.page < state.totalPages - 1) {
      state.page += 1;
      loadRentals();
    }
  });
  refs.table.addEventListener('click', (event) => {
    const button = event.target.closest('button[data-action]');
    if (!button) return;
    const rental = state.rows.find((item) => String(item.id) === button.dataset.rentalId);
    if (!rental) return;
    if (button.dataset.action === 'details') openDetails(rental);
    else showUnavailableAction(rental, button.dataset.action);
  });
  document.getElementById('openNewRental').addEventListener('click', async () => {
    refs.newDialog.showModal();
    await loadNewRentalOptions();
  });
  document.querySelectorAll('[data-close-dialog]').forEach((button) => {
    button.addEventListener('click', () => refs.detailsDialog.close());
  });
  document.querySelectorAll('[data-close-action-dialog]').forEach((button) => {
    button.addEventListener('click', () => refs.actionDialog.close());
  });
  document.querySelectorAll('[data-close-new-dialog]').forEach((button) => {
    button.addEventListener('click', () => refs.newDialog.close());
  });
  refs.equipmentList.addEventListener('change', updateEstimate);
  refs.startDate.addEventListener('change', updateEstimate);
  refs.dueDate.addEventListener('change', updateEstimate);
  document.getElementById('newRentalForm').addEventListener('submit', (event) => {
    event.preventDefault();
    reportNewRentalError('A API ainda não disponibiliza a criação de contratos; nenhuma locação foi enviada.');
  });
  [refs.detailsDialog, refs.actionDialog, refs.newDialog].forEach((dialog) => {
    dialog.addEventListener('click', (event) => {
      if (event.target === dialog) dialog.close();
    });
  });

  const savedTheme = localStorage.getItem('gearRentTheme');
  if (savedTheme === 'dark') document.body.dataset.theme = 'dark';
  loadProfile();
  loadSummary();
  loadRentals();
})();
