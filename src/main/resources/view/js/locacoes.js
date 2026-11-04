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
    financialRequest: 0,
    rows: [],
    filters: {},
    clients: [],
    equipment: [],
    pendingAction: null,
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
    revenuePeriod: document.getElementById('rentalRevenuePeriod'),
    completedCount: document.getElementById('rentalCompletedCount'),
    fines: document.getElementById('rentalFines'),
    discounts: document.getElementById('rentalDiscounts'),
    openAmount: document.getElementById('rentalOpenAmount'),
    activeAmount: document.getElementById('rentalActiveAmount'),
    active: document.getElementById('rentalActiveCount'),
    overdueAmount: document.getElementById('rentalOverdueAmount'),
    overdueCount: document.getElementById('rentalOverdueCount'),
    overdueFines: document.getElementById('rentalOverdueFines'),
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
    paymentType: document.getElementById('newRentalPaymentType'),
    estimateDays: document.getElementById('estimatedRentalDays'),
    estimateTotal: document.getElementById('estimatedRentalTotal'),
    submitNewRental: document.getElementById('submitNewRental'),
    confirmAction: document.getElementById('confirmRentalAction'),
  };

  // Restaura e salva a preferência de visibilidade de cada indicador.
  function setupKpiVisibility(pageKey) {
    const storagePrefix = `gearRentHiddenKpi:${pageKey}:`;
    document.querySelectorAll('[data-kpi-visibility]').forEach((checkbox) => {
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

  // Traduz falhas de rede e respostas HTTP em mensagens para o usuário.
  function getErrorMessage(error) {
    if (error.response?.status === 401) {
      window.GR.auth.sair();
      return 'Sua sessão expirou. Entre novamente.';
    }
    const apiMessages = error.response?.data?.mensagens;
    if (Array.isArray(apiMessages) && apiMessages.length) return apiMessages.join(' ');
    if (error.response?.status === 404) return 'O registro solicitado não foi encontrado.';
    if (error.response) return `A solicitação falhou (HTTP ${error.response.status}).`;
    if (!error.isAxiosError && error.message) return error.message;
    return 'Não foi possível conectar à API. Verifique a conexão e tente novamente.';
  }

  // Consulta a API autenticada e retorna o corpo da resposta.
  async function getJSON(path) {
    const response = await window.apiClient.get(path);
    return response.data;
  }

  // Exibe ou limpa a área de erro da página de locações.
  function setError(message) {
    refs.error.textContent = message;
    refs.error.hidden = !message;
  }

  // Exibe ou limpa a mensagem de estado da página.
  function setMessage(message) {
    refs.message.textContent = message;
    refs.message.hidden = !message;
  }

  // Formata valores válidos como moeda brasileira.
  function currency(value) {
    if (value === null || value === undefined || value === '') return 'Não informado';
    const amount = Number(value);
    return Number.isFinite(amount)
      ? new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(amount)
      : 'Não informado';
  }

  // Formata quantidades inteiras sem converter valores inválidos em zero.
  function formatCount(value) {
    const count = Number(value);
    return value !== null && value !== undefined && value !== ''
      && Number.isSafeInteger(count) && count >= 0
      ? new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 0 }).format(count)
      : '—';
  }

  // Exibe datas no formato brasileiro sem deslocamento de fuso horário.
  function formatDate(value) {
    if (!value) return '—';
    const match = String(value).match(/^(\d{4})-(\d{2})-(\d{2})/);
    if (match) return `${match[3]}/${match[2]}/${match[1]}`;
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? '—' : new Intl.DateTimeFormat('pt-BR').format(date);
  }

  // Calcula as iniciais usadas nos avatares do usuário.
  function initials(name) {
    return String(name || 'GR').trim().split(/\s+/).slice(0, 2)
      .map((part) => part.charAt(0).toLocaleUpperCase('pt-BR')).join('') || 'GR';
  }

  // Obtém o identificador do usuário autenticado a partir do token.
  function tokenSubject() {
    try {
      const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      return JSON.parse(window.atob(payload)).sub;
    } catch {
      return null;
    }
  }

  // Carrega o perfil para preencher nome, base e avatares da página.
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

  // Filtra localmente as locações carregadas pela busca textual atual.
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

  // Cria uma célula segura para apresentar valores na tabela.
  function makeCell(text, className = '') {
    const cell = document.createElement('td');
    cell.textContent = text || '—';
    if (className) cell.className = className;
    return cell;
  }

  // Retorna a classe visual correspondente ao status da locação.
  function statusClass(status) {
    return ({
      EM_ANDAMENTO: 'status--andamento',
      CONCLUIDA: 'status--concluida',
      ATRASADA: 'status--atrasada',
      CANCELADA: 'status--cancelada',
    })[status] || '';
  }

  // Cria botão acessível associado à ação e à locação selecionada.
  function makeAction(label, icon, rental, action, disabled = false) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'icon-button';
    button.textContent = icon;
    button.dataset.action = action;
    button.dataset.rentalId = String(rental.id);
    button.setAttribute('aria-label', `${label}, locação ${rental.id}`);
    button.title = label;
    button.disabled = disabled;
    return button;
  }

  // Renderiza as locações e sincroniza os contadores e a paginação.
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
        const canCancel = rental.status === 'EM_ANDAMENTO';
        const canReturn = ['EM_ANDAMENTO', 'ATRASADA'].includes(rental.status);
        actions.append(
          makeAction('Ver detalhes do contrato', '👁️', rental, 'details'),
          makeAction('Registrar devolução', '📥', rental, 'return', !canReturn),
          makeAction('Cancelar locação', '🚫', rental, 'cancel', !canCancel),
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

  // Monta filtros e paginação para a consulta de locações.
  function queryString() {
    const params = new URLSearchParams({ page: String(state.page), size: String(pageSize) });
    Object.entries(state.filters).forEach(([key, value]) => {
      if (value) params.set(key, value);
    });
    return params.toString();
  }

  // Busca os resultados da página atual e trata estados de sucesso ou erro.
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

  // Atualiza cards financeiros e operacionais usando o período selecionado.
  async function loadSummary() {
    const requestId = ++state.financialRequest;
    const query = new URLSearchParams();
    ['dataInicio', 'dataFim'].forEach((key) => {
      if (state.filters[key]) query.set(key, state.filters[key]);
    });
    const suffix = query.toString() ? `?${query.toString()}` : '';

    try {
      const summary = await getJSON(`/financeiro/resumo${suffix}`);
      if (!summary || !summary.valoresEmAberto) {
        throw new Error('A resposta do resumo financeiro veio em um formato inesperado.');
      }
      if (requestId !== state.financialRequest) return;

      const open = summary.valoresEmAberto;
      refs.revenue.textContent = currency(summary.receitaTotal);
      refs.revenuePeriod.textContent = summary.dataInicio && summary.dataFim
        ? `${formatDate(summary.dataInicio)} a ${formatDate(summary.dataFim)}`
        : 'Período não informado';
      refs.completedCount.textContent = formatCount(summary.quantidadeConcluidas);
      refs.fines.textContent = currency(summary.totalMultas);
      refs.discounts.textContent = currency(summary.totalDescontos);
      refs.openAmount.textContent = currency(open.valorTotal);
      refs.activeAmount.textContent = currency(open.valorEmAndamento);
      refs.active.textContent = formatCount(open.quantidadeEmAndamento);
      refs.overdueAmount.textContent = currency(open.valorAtrasadas);
      refs.overdueCount.textContent = formatCount(open.quantidadeAtrasadas);
      refs.overdueFines.textContent = currency(open.valorMultasAtrasadas);
    } catch (error) {
      if (requestId !== state.financialRequest) return;
      [
        refs.revenue,
        refs.completedCount,
        refs.fines,
        refs.discounts,
        refs.openAmount,
        refs.activeAmount,
        refs.active,
        refs.overdueAmount,
        refs.overdueCount,
        refs.overdueFines,
      ].forEach((element) => {
        element.textContent = 'Indisponível';
      });
      refs.revenuePeriod.textContent = 'Não foi possível carregar o período.';
      setError(`${getErrorMessage(error)} Os indicadores financeiros não puderam ser carregados.`);
    }
  }

  // Adiciona um campo de nome e valor aos detalhes do contrato.
  function appendDefinition(parent, term, value) {
    const wrapper = document.createElement('div');
    const title = document.createElement('dt');
    const description = document.createElement('dd');
    title.textContent = term;
    description.textContent = value == null || value === '' ? 'Não informado' : String(value);
    wrapper.append(title, description);
    parent.append(wrapper);
  }

  // Busca e apresenta os detalhes completos da locação selecionada.
  async function openDetails(rental) {
    refs.detailsTitle.textContent = `Detalhes da locação · ID ${rental.id}`;
    refs.detailsContent.replaceChildren();
    refs.detailsContent.textContent = 'Carregando detalhes do contrato…';
    refs.detailsDialog.showModal();
    try {
      rental = await getJSON(`/locacoes/${encodeURIComponent(rental.id)}`);
    } catch (error) {
      refs.detailsContent.textContent = getErrorMessage(error);
      return;
    }

    refs.detailsContent.replaceChildren();
    const details = document.createElement('dl');
    details.className = 'dialog-details';
    appendDefinition(details, 'Cliente', rental.clienteNome || rental.cliente?.nome);
    appendDefinition(details, 'Status', rental.status);
    appendDefinition(details, 'Data de retirada', formatDate(rental.dataRetirada));
    appendDefinition(details, 'Devolução prevista', formatDate(rental.dataPrevista));
    appendDefinition(details, 'Devolução real', formatDate(rental.dataDevolucao || rental.dataReal));
    appendDefinition(details, 'Dias de atraso', rental.diasAtraso);
    appendDefinition(details, 'Diárias', currency(rental.valorDiarias ?? rental.diarias));
    appendDefinition(details, 'Desconto', currency(rental.valorDesconto));
    appendDefinition(details, 'Multa', currency(rental.valorMulta));
    appendDefinition(details, 'Total', currency(rental.valorTotal ?? rental.valorFinalCobrado));
    refs.detailsContent.append(details);

    const title = document.createElement('h3');
    title.textContent = 'Equipamentos vinculados';
    const list = document.createElement('ul');
    list.className = 'dialog-item-list';
    (Array.isArray(rental.itens) ? rental.itens : []).forEach((item) => {
      const li = document.createElement('li');
      li.textContent = [
        item.nome,
        item.modelo,
        item.numeroPatrimonio ? `Patrimônio ${item.numeroPatrimonio}` : '',
        item.valorDiaria == null ? '' : `${currency(item.valorDiaria)}/dia`,
      ]
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
    note.textContent = 'Valores e informações do contrato retornados pela API.';
    refs.detailsContent.append(note);
  }

  // Prepara a confirmação da ação disponível para a locação.
  function showRentalAction(rental, action) {
    const isReturn = action === 'return';
    refs.actionTitle.textContent = isReturn ? 'Registrar devolução' : 'Cancelar locação';
    refs.actionContent.replaceChildren();
    const notice = document.createElement('p');
    notice.className = 'form-notice';
    notice.textContent = isReturn
      ? `Confirme a devolução da locação ${rental.id}. O servidor calculará o valor final e eventuais multas.`
      : `Confirme o cancelamento da locação ${rental.id}. Os equipamentos serão liberados pelo servidor.`;
    refs.actionContent.append(notice);
    refs.returnDateGroup.hidden = !isReturn;
    refs.returnDate.value = '';
    refs.confirmAction.textContent = isReturn ? 'Confirmar devolução' : 'Confirmar cancelamento';
    refs.confirmAction.dataset.action = action;
    state.pendingAction = { id: rental.id, action };
    refs.actionDialog.showModal();
  }

  // Envia a ação confirmada e recarrega locações e indicadores.
  async function confirmRentalAction() {
    if (!state.pendingAction) return;
    const { id, action } = state.pendingAction;
    refs.confirmAction.disabled = true;
    refs.confirmAction.textContent = 'Processando…';
    try {
      if (action === 'return') {
        const body = refs.returnDate.value ? { dataDevolucao: refs.returnDate.value } : {};
        await window.apiClient.patch(`/locacoes/${encodeURIComponent(id)}/devolucao`, body);
      } else {
        await window.apiClient.patch(`/locacoes/${encodeURIComponent(id)}/cancelamento`);
      }
      refs.actionDialog.close();
      state.pendingAction = null;
      setError('');
      await Promise.all([loadRentals(), loadSummary()]);
      setMessage(action === 'return' ? 'Devolução registrada com sucesso.' : 'Locação cancelada com sucesso.');
    } catch (error) {
      const notice = document.createElement('p');
      notice.className = 'form-notice';
      notice.textContent = getErrorMessage(error);
      refs.actionContent.replaceChildren(notice);
    } finally {
      refs.confirmAction.disabled = false;
      refs.confirmAction.textContent = action === 'return' ? 'Confirmar devolução' : 'Confirmar cancelamento';
    }
  }

  // Adiciona um equipamento selecionável ao formulário de nova locação.
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

  // Carrega clientes e equipamentos que podem ser usados na nova locação.
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

  // Acumula e exibe erros de validação ou consulta do formulário.
  function reportNewRentalError(message) {
    refs.newError.textContent = refs.newError.hidden ? message : `${refs.newError.textContent} ${message}`;
    refs.newError.hidden = false;
  }

  // Converte uma data do campo HTML em dia UTC para cálculos consistentes.
  function dateValueToUtcDay(value) {
    if (!value) return null;
    const [year, month, day] = value.split('-').map(Number);
    return Date.UTC(year, month - 1, day);
  }

  // Recalcula a estimativa informativa conforme datas e equipamentos escolhidos.
  function updateEstimate() {
    const start = dateValueToUtcDay(refs.startDate.value);
    const due = dateValueToUtcDay(refs.dueDate.value);
    const selected = [...refs.equipmentList.querySelectorAll('input[name="equipment"]:checked')];
    if (start == null || due == null || due <= start || !selected.length) {
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

  // Valida filtros de período e solicita novamente a lista de locações.
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
    Promise.all([loadRentals(), loadSummary()]);
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
    Promise.all([loadRentals(), loadSummary()]);
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
    else showRentalAction(rental, button.dataset.action);
  });
  document.getElementById('openNewRental').addEventListener('click', async () => {
    document.getElementById('newRentalForm').reset();
    refs.newError.hidden = true;
    refs.newError.textContent = '';
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
  document.getElementById('newRentalForm').addEventListener('submit', async (event) => {
    event.preventDefault();
    const equipmentIds = [...refs.equipmentList.querySelectorAll('input[name="equipment"]:checked')]
      .map((input) => Number(input.value));
    if (!equipmentIds.length) {
      reportNewRentalError('Selecione pelo menos um equipamento para criar a locação.');
      return;
    }
    if (refs.dueDate.value <= refs.startDate.value) {
      reportNewRentalError('A data prevista de devolução deve ser posterior à data de retirada.');
      return;
    }

    const payload = {
      clienteId: Number(refs.newClient.value),
      equipamentosIds: equipmentIds,
      dataRetirada: refs.startDate.value,
      dataPrevista: refs.dueDate.value,
      tipoPagamento: refs.paymentType.value || null,
    };
    refs.submitNewRental.disabled = true;
    refs.submitNewRental.textContent = 'Criando…';
    refs.newError.hidden = true;
    try {
      const response = await window.apiClient.post('/locacoes', payload);
      refs.newDialog.close();
      document.getElementById('newRentalForm').reset();
      state.page = 0;
      await Promise.all([loadRentals(), loadSummary()]);
      setMessage(`Locação ${response.data.codigoSequencial ?? response.data.id} criada com sucesso.`);
    } catch (error) {
      reportNewRentalError(getErrorMessage(error));
    } finally {
      refs.submitNewRental.disabled = false;
      refs.submitNewRental.textContent = 'Criar locação';
    }
  });
  refs.confirmAction.addEventListener('click', confirmRentalAction);
  [refs.detailsDialog, refs.actionDialog, refs.newDialog].forEach((dialog) => {
    dialog.addEventListener('click', (event) => {
      if (event.target === dialog) dialog.close();
    });
  });

  const savedTheme = localStorage.getItem('gearRentTheme');
  if (savedTheme === 'dark') document.body.dataset.theme = 'dark';
  setupKpiVisibility('rentals');
  loadProfile();
  loadSummary();
  loadRentals();
})();
