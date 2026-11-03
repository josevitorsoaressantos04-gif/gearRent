(() => {
  const tokenKey = 'gearrent_token';
  const token = sessionStorage.getItem(tokenKey);
  if (!token) {
    window.location.replace('login.html');
    return;
  }
  if (localStorage.getItem('gearRentTheme') === 'dark') document.body.dataset.theme = 'dark';

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

  const categoryLabels = {
    FERRAMENTA_ELETRICA: 'Ferramenta elétrica',
    MAQUINARIO_PESADO: 'Maquinário pesado',
    ANDAIME_ESCADA: 'Andaime / escada',
    FERRAMENTA_MANUAL: 'Ferramenta manual',
  };
  const statusLabels = {
    DISPONIVEL: 'Disponível',
    ALUGADO: 'Alugado',
    MANUTENCAO: 'Em manutenção',
    CANCELADO: 'Inativado',
  };
  const state = {
    equipment: [],
    details: new Map(),
    selectedId: null,
    editingId: null,
    pendingDeleteId: null,
    detailRequest: 0,
    search: '',
    category: '',
    status: '',
  };
  const refs = {
    search: document.getElementById('equipmentSearch'),
    category: document.getElementById('equipmentCategory'),
    status: document.getElementById('equipmentStatus'),
    table: document.getElementById('equipmentTableBody'),
    count: document.getElementById('equipmentCount'),
    message: document.getElementById('equipmentMessage'),
    error: document.getElementById('equipmentError'),
    total: document.getElementById('equipmentTotal'),
    available: document.getElementById('equipmentAvailable'),
    maintenance: document.getElementById('equipmentMaintenance'),
    emptyDetail: document.getElementById('equipmentDetailEmpty'),
    detailList: document.getElementById('equipmentDetails'),
    formDialog: document.getElementById('equipmentFormDialog'),
    form: document.getElementById('equipmentForm'),
    formTitle: document.getElementById('equipmentFormTitle'),
    formError: document.getElementById('equipmentFormError'),
    saveButton: document.getElementById('saveEquipment'),
    deleteDialog: document.getElementById('equipmentDeleteDialog'),
    deleteText: document.getElementById('equipmentDeleteText'),
    confirmDelete: document.getElementById('confirmDeleteEquipment'),
    statusField: document.getElementById('equipmentStatusField'),
    statusInput: document.getElementById('equipmentFormStatus'),
  };

  function getErrorMessage(error) {
    if (error.response?.status === 401) {
      window.GR.auth.sair();
      return 'Sua sessão expirou. Entre novamente.';
    }
    const messages = error.response?.data?.mensagens;
    if (Array.isArray(messages) && messages.length) return messages.join(' ');
    if (error.response?.data?.message) return error.response.data.message;
    if (error.response) return `A solicitação falhou (HTTP ${error.response.status}).`;
    return 'Não foi possível conectar à API. Verifique a conexão e tente novamente.';
  }

  function setError(message) {
    refs.error.textContent = message;
    refs.error.hidden = !message;
  }

  function setMessage(message) {
    refs.message.textContent = message;
    refs.message.hidden = !message;
  }

  function setFormError(message) {
    refs.formError.textContent = message;
    refs.formError.hidden = !message;
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
      const response = await window.apiClient.get('/usuarios');
      if (!Array.isArray(response.data)) throw new Error('A resposta de usuários veio em formato inesperado.');
      const current = response.data.find((user) => String(user.id) === String(tokenSubject()));
      if (!current?.nome) throw new Error('Não foi possível identificar o usuário autenticado.');
      document.getElementById('userNameSidebar').textContent = current.nome;
      document.getElementById('userNameTopbar').textContent = current.nome;
      document.getElementById('baseNameSidebar').textContent = `Base de ${current.nome}`;
      document.getElementById('avatarSidebar').textContent = initials(current.nome);
      document.getElementById('avatarTopbar').textContent = initials(current.nome);
    } catch (error) {
      setError(`${getErrorMessage(error)} O perfil da base não pôde ser carregado.`);
    }
  }

  function formatCurrency(value) {
    if (value === null || value === undefined || value === '') return 'Não informado';
    const amount = Number(value);
    if (!Number.isFinite(amount)) return 'Não informado';
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(amount);
  }

  function codeLabel(item) {
    if (item.codigoSequencial == null) return `ID ${item.id}`;
    return `EQP-${String(item.codigoSequencial).padStart(4, '0')}`;
  }

  function statusClass(status) {
    return ({
      DISPONIVEL: 'equipment-status--available',
      ALUGADO: 'equipment-status--rented',
      MANUTENCAO: 'equipment-status--maintenance',
      CANCELADO: 'equipment-status--cancelled',
    })[status] || '';
  }

  function getEquipmentById(id) {
    return state.equipment.find((item) => String(item.id) === String(id));
  }

  function searchableText(item) {
    const detail = state.details.get(String(item.id)) || {};
    return [
      item.id,
      item.codigoSequencial,
      item.nome,
      item.modelo || detail.modelo,
      item.numeroPatrimonio || detail.numeroPatrimonio,
      item.categoria,
      item.status,
    ].filter((value) => value != null).join(' ').toLocaleLowerCase('pt-BR');
  }

  function filteredEquipment() {
    const term = state.search.trim().toLocaleLowerCase('pt-BR');
    return state.equipment.filter((item) => {
      const matchesTerm = !term || searchableText(item).includes(term);
      const matchesCategory = !state.category || item.categoria === state.category;
      const matchesStatus = !state.status || item.status === state.status;
      return matchesTerm && matchesCategory && matchesStatus;
    });
  }

  function makeCell(text) {
    const cell = document.createElement('td');
    cell.textContent = text || '—';
    return cell;
  }

  function actionButton(label, icon, action, item) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'icon-button';
    button.dataset.action = action;
    button.dataset.equipmentId = String(item.id);
    button.textContent = icon;
    button.title = `${label} ${item.nome || 'equipamento'}`;
    button.setAttribute('aria-label', `${label} ${item.nome || 'equipamento'}`);
    return button;
  }

  function renderTable() {
    const equipment = filteredEquipment();
    refs.table.replaceChildren();

    if (!equipment.length) {
      const row = document.createElement('tr');
      const cell = makeCell(state.equipment.length
        ? 'Nenhum equipamento corresponde aos filtros selecionados.'
        : 'Nenhum equipamento cadastrado nesta base.');
      cell.colSpan = 6;
      cell.className = 'table-state';
      row.append(cell);
      refs.table.append(row);
    } else {
      equipment.forEach((item) => {
        const row = document.createElement('tr');
        row.dataset.equipmentId = String(item.id);
        row.tabIndex = 0;
        if (String(state.selectedId) === String(item.id)) row.classList.add('equipment-row--selected');

        const statusCell = document.createElement('td');
        const badge = document.createElement('span');
        badge.className = `equipment-status ${statusClass(item.status)}`;
        badge.textContent = statusLabels[item.status] || item.status || 'Não informado';
        statusCell.append(badge);

        const actions = document.createElement('td');
        actions.className = 'equipment-row-actions';
        actions.append(actionButton('Ver detalhes de', '👁️', 'select', item));
        const edit = actionButton('Editar', '✏️', 'edit', item);
        edit.disabled = item.status === 'ALUGADO';
        edit.title = edit.disabled
          ? 'Equipamentos alugados não podem ser alterados.'
          : `Editar ${item.nome || 'equipamento'}`;
        actions.append(edit);
        const remove = actionButton('Inativar', '🗑️', 'delete', item);
        remove.disabled = item.status === 'ALUGADO';
        remove.title = remove.disabled
          ? 'Equipamentos alugados não podem ser inativados.'
          : `Inativar ${item.nome || 'equipamento'}`;
        actions.append(remove);

        row.append(
          makeCell(codeLabel(item)),
          makeCell(item.nome),
          makeCell(categoryLabels[item.categoria] || item.categoria),
          makeCell(formatCurrency(item.valorDiaria)),
          statusCell,
          actions,
        );
        refs.table.append(row);
      });
    }
    refs.count.textContent = `${equipment.length} de ${state.equipment.length} equipamento(s)`;
  }

  function updateSummary() {
    refs.total.textContent = String(state.equipment.length);
    refs.available.textContent = String(state.equipment.filter((item) => item.status === 'DISPONIVEL').length);
    refs.maintenance.textContent = String(state.equipment.filter((item) => item.status === 'MANUTENCAO').length);
  }

  function resetDetails() {
    refs.emptyDetail.hidden = false;
    refs.detailList.hidden = true;
    document.getElementById('editSelectedEquipment').disabled = true;
    document.getElementById('deleteSelectedEquipment').disabled = true;
  }

  function displayDetails(item) {
    refs.emptyDetail.hidden = true;
    refs.detailList.hidden = false;
    document.getElementById('detailEquipmentName').textContent = item.nome || '—';
    document.getElementById('detailEquipmentCode').textContent = codeLabel(item);
    document.getElementById('detailEquipmentModel').textContent = item.modelo || 'Não informado';
    document.getElementById('detailEquipmentAsset').textContent = item.numeroPatrimonio || 'Não informado';
    document.getElementById('detailEquipmentCategory').textContent = categoryLabels[item.categoria] || item.categoria || 'Não informado';
    document.getElementById('detailEquipmentRate').textContent = formatCurrency(item.valorDiaria);
    document.getElementById('detailEquipmentStatus').textContent = statusLabels[item.status] || item.status || 'Não informado';
    const blocked = item.status === 'ALUGADO';
    document.getElementById('editSelectedEquipment').disabled = blocked;
    document.getElementById('deleteSelectedEquipment').disabled = blocked;
    renderTable();
  }

  async function selectEquipment(id) {
    const rowData = getEquipmentById(id);
    if (!rowData) return;
    state.selectedId = id;
    const requestId = ++state.detailRequest;
    renderTable();
    try {
      const response = await window.apiClient.get(`/equipamentos/${encodeURIComponent(id)}`);
      if (requestId !== state.detailRequest) return;
      const detail = response.data;
      if (!detail || String(detail.id) !== String(id)) {
        throw new Error('A resposta do equipamento veio em formato inesperado.');
      }
      state.details.set(String(id), detail);
      displayDetails(detail);
    } catch (error) {
      if (requestId !== state.detailRequest) return;
      displayDetails(rowData);
      setError(`${getErrorMessage(error)} Não foi possível carregar detalhes adicionais do equipamento.`);
    }
  }

  async function loadEquipment() {
    setError('');
    setMessage('Carregando equipamentos…');
    try {
      const response = await window.apiClient.get('/equipamentos');
      if (!Array.isArray(response.data)) throw new Error('A resposta de equipamentos veio em formato inesperado.');
      state.equipment = response.data;
      state.details.clear();
      updateSummary();
      renderTable();
      setMessage('');
      if (state.selectedId != null && getEquipmentById(state.selectedId)) {
        await selectEquipment(state.selectedId);
      } else if (state.equipment.length) {
        await selectEquipment(state.equipment[0].id);
      } else {
        state.selectedId = null;
        resetDetails();
      }
    } catch (error) {
      state.equipment = [];
      state.selectedId = null;
      updateSummary();
      renderTable();
      resetDetails();
      setMessage('');
      setError(`${getErrorMessage(error)} A listagem utiliza GET /equipamentos, limitada à base do token.`);
    }
  }

  function openForm(item = null) {
    state.editingId = item ? item.id : null;
    setFormError('');
    refs.form.reset();
    refs.formTitle.textContent = item ? 'Editar equipamento' : 'Novo equipamento';
    refs.saveButton.textContent = item ? 'Salvar alterações' : 'Salvar equipamento';
    refs.statusField.hidden = !item;
    if (item) {
      document.getElementById('equipmentName').value = item.nome || '';
      document.getElementById('equipmentModel').value = item.modelo || '';
      document.getElementById('equipmentAssetNumber').value = item.numeroPatrimonio || '';
      document.getElementById('equipmentFormCategory').value = item.categoria || '';
      document.getElementById('equipmentDailyRate').value = item.valorDiaria ?? '';
      refs.statusInput.value = item.status === 'DISPONIVEL' || item.status === 'MANUTENCAO' ? item.status : '';
    }
    refs.formDialog.showModal();
  }

  function formPayload() {
    const payload = {
      nome: document.getElementById('equipmentName').value.trim(),
      modelo: document.getElementById('equipmentModel').value.trim() || null,
      numeroPatrimonio: document.getElementById('equipmentAssetNumber').value.trim() || null,
      categoria: document.getElementById('equipmentFormCategory').value,
      valorDiaria: Number(document.getElementById('equipmentDailyRate').value),
    };
    if (state.editingId != null) payload.status = refs.statusInput.value || null;
    return payload;
  }

  async function saveEquipment(event) {
    event.preventDefault();
    setFormError('');
    const payload = formPayload();
    if (!payload.nome || !payload.categoria || !Number.isFinite(payload.valorDiaria) || payload.valorDiaria <= 0) {
      setFormError('Informe nome, categoria e um valor de diária maior que zero.');
      return;
    }

    refs.saveButton.disabled = true;
    try {
      if (state.editingId == null) {
        await window.apiClient.post('/equipamentos', payload);
        setMessage('Equipamento cadastrado com sucesso.');
      } else {
        await window.apiClient.put(`/equipamentos/${encodeURIComponent(state.editingId)}`, payload);
        setMessage('Equipamento atualizado com sucesso.');
      }
      refs.formDialog.close();
      await loadEquipment();
    } catch (error) {
      setFormError(getErrorMessage(error));
    } finally {
      refs.saveButton.disabled = false;
    }
  }

  function openDeleteConfirmation(item) {
    state.pendingDeleteId = item.id;
    refs.deleteText.textContent = `Deseja inativar ${item.nome || 'este equipamento'} (${codeLabel(item)})?`;
    refs.deleteDialog.showModal();
  }

  async function deletePendingEquipment() {
    if (state.pendingDeleteId == null) return;
    refs.confirmDelete.disabled = true;
    setError('');
    try {
      await window.apiClient.delete(`/equipamentos/${encodeURIComponent(state.pendingDeleteId)}`);
      refs.deleteDialog.close();
      if (String(state.selectedId) === String(state.pendingDeleteId)) {
        state.selectedId = null;
        resetDetails();
      }
      state.pendingDeleteId = null;
      setMessage('Equipamento inativado. O histórico foi preservado.');
      await loadEquipment();
    } catch (error) {
      setError(`${getErrorMessage(error)} Não foi possível inativar o equipamento.`);
    } finally {
      refs.confirmDelete.disabled = false;
    }
  }

  function closeDialog(dialog) {
    if (dialog.open) dialog.close();
  }

  document.getElementById('btnLogout').addEventListener('click', () => window.GR.auth.sair());
  document.getElementById('openEquipmentCreate').addEventListener('click', () => openForm());
  document.getElementById('editSelectedEquipment').addEventListener('click', () => {
    const item = state.details.get(String(state.selectedId)) || getEquipmentById(state.selectedId);
    if (item && item.status !== 'ALUGADO') openForm(item);
  });
  document.getElementById('deleteSelectedEquipment').addEventListener('click', () => {
    const item = state.details.get(String(state.selectedId)) || getEquipmentById(state.selectedId);
    if (item && item.status !== 'ALUGADO') openDeleteConfirmation(item);
  });
  refs.table.addEventListener('click', (event) => {
    const button = event.target.closest('button[data-action]');
    const row = event.target.closest('tr[data-equipment-id]');
    const id = button?.dataset.equipmentId || row?.dataset.equipmentId;
    if (!id) return;
    const item = getEquipmentById(id);
    if (!item) return;
    const action = button?.dataset.action || 'select';
    if (action === 'select') selectEquipment(id);
    if (action === 'edit' && item.status !== 'ALUGADO') openForm(state.details.get(String(id)) || item);
    if (action === 'delete' && item.status !== 'ALUGADO') openDeleteConfirmation(item);
  });
  refs.table.addEventListener('keydown', (event) => {
    if ((event.key === 'Enter' || event.key === ' ') && event.target.matches('tr[data-equipment-id]')) {
      event.preventDefault();
      selectEquipment(event.target.dataset.equipmentId);
    }
  });
  refs.search.addEventListener('input', () => {
    state.search = refs.search.value;
    renderTable();
  });
  refs.category.addEventListener('change', () => {
    state.category = refs.category.value;
    renderTable();
  });
  refs.status.addEventListener('change', () => {
    state.status = refs.status.value;
    renderTable();
  });
  refs.form.addEventListener('submit', saveEquipment);
  refs.confirmDelete.addEventListener('click', deletePendingEquipment);
  document.querySelectorAll('[data-close-equipment-form]').forEach((button) => {
    button.addEventListener('click', () => closeDialog(refs.formDialog));
  });
  document.querySelectorAll('[data-close-equipment-delete]').forEach((button) => {
    button.addEventListener('click', () => {
      closeDialog(refs.deleteDialog);
      state.pendingDeleteId = null;
    });
  });
  [refs.formDialog, refs.deleteDialog].forEach((dialog) => {
    dialog.addEventListener('click', (event) => {
      if (event.target === dialog) closeDialog(dialog);
    });
  });

  loadProfile();
  loadEquipment();
})();
