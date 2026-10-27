const clientesMock = [
  {
    id: 1,
    codigo: 'CLI-1001',
    nome: 'Maria da Silva',
    tipoPessoa: 'PF',
    documento: '123.456.789-09',
    telefone: '(11) 98765-4321',
    email: 'maria@email.com',
    endereco: 'Avenida Brasil, 1200, Centro, São Paulo/SP',
    status: 'ATIVO',
    totalLocacoes: 8,
    locacoesEmAndamento: 2,
    equipamentosEmPosse: 3,
    valorAberto: 3420,
    historico: [
      { equipamento: 'Retroescavadeira CAT 320', data: '15/09/2026', status: 'EM_ANDAMENTO' },
      { equipamento: 'Compactador CS-410', data: '02/08/2026', status: 'CONCLUIDA' },
      { equipamento: 'Trator Mini 240', data: '10/07/2026', status: 'ATRASADA' },
    ],
  },
  {
    id: 2,
    codigo: 'CLI-1002',
    nome: 'José Pereira Construções',
    tipoPessoa: 'PJ',
    documento: '42.346.468/0001-22',
    telefone: '(11) 98888-1212',
    email: 'contato@josepereira.com.br',
    endereco: 'Rua das Flores, 300, Jardim Paulista, São Paulo/SP',
    status: 'ATIVO',
    totalLocacoes: 16,
    locacoesEmAndamento: 4,
    equipamentosEmPosse: 5,
    valorAberto: 8420,
    historico: [
      { equipamento: 'Escavadeira 320D', data: '22/09/2026', status: 'EM_ANDAMENTO' },
      { equipamento: 'Caminhão Munck', data: '03/09/2026', status: 'CONCLUIDA' },
      { equipamento: 'Guindaste Portátil', data: '18/08/2026', status: 'CANCELADA' },
    ],
  },
  {
    id: 3,
    codigo: 'CLI-1003',
    nome: 'Ana Costa',
    tipoPessoa: 'PF',
    documento: '315.982.430-18',
    telefone: '(21) 99774-1020',
    email: 'ana.costa@gmail.com',
    endereco: 'Estrada do Mar, 900, Copacabana, Rio de Janeiro/RJ',
    status: 'INATIVO',
    totalLocacoes: 3,
    locacoesEmAndamento: 0,
    equipamentosEmPosse: 0,
    valorAberto: 0,
    historico: [
      { equipamento: 'Mini Escavadeira', data: '04/06/2026', status: 'CONCLUIDA' },
      { equipamento: 'Plataforma de Trabalho', data: '08/05/2026', status: 'CANCELADA' },
    ],
  },
];

const estado = {
  clientes: [...clientesMock],
  clienteSelecionado: clientesMock[0],
  tipoPessoaFiltro: 'TODOS',
  statusFiltro: 'TODOS',
  termoBusca: '',
  editandoClienteId: null,
};

const refs = {
  tabela: document.getElementById('clientesTableBody'),
  contador: document.getElementById('contadorClientes'),
  busca: document.getElementById('clienteSearch'),
  tipoPessoaFilter: document.getElementById('tipoPessoaFilter'),
  statusFilter: document.getElementById('statusFilter'),
  modal: document.getElementById('clienteModal'),
  form: document.getElementById('clienteForm'),
  modalTitle: document.getElementById('modalTitle'),
  tipoPessoa: document.getElementById('tipoPessoa'),
  labelNome: document.getElementById('labelNome'),
  labelDocumento: document.getElementById('labelDocumento'),
  nomeCliente: document.getElementById('nomeCliente'),
  documentoCliente: document.getElementById('documentoCliente'),
  telefoneCliente: document.getElementById('telefoneCliente'),
  emailCliente: document.getElementById('emailCliente'),
  enderecoCliente: document.getElementById('enderecoCliente'),
  detailNome: document.getElementById('detailNome'),
  detailCodigo: document.getElementById('detailCodigo'),
  detailTipo: document.getElementById('detailTipo'),
  detailDocumento: document.getElementById('detailDocumento'),
  detailEmail: document.getElementById('detailEmail'),
  detailEndereco: document.getElementById('detailEndereco'),
  detailStatus: document.getElementById('detailStatus'),
  metricTotalLocacoes: document.getElementById('metricTotalLocacoes'),
  metricLocacoesAndamento: document.getElementById('metricLocacoesAndamento'),
  metricEquipamentosPosse: document.getElementById('metricEquipamentosPosse'),
  metricValorAberto: document.getElementById('metricValorAberto'),
  historicoLocacoes: document.getElementById('historicoLocacoes'),
  btnNovoCliente: document.getElementById('btnNovoCliente'),
  btnNovaLocacao: document.getElementById('btnNovaLocacao'),
};

function formatarMoeda(valor) {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(Number(valor || 0));
}

function mascararDocumento(valor) {
  const digits = valor.replace(/\D/g, '').slice(0, 14);

  if (refs.tipoPessoa.value === 'PJ') {
    return digits.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/, '$1.$2.$3/$4-$5');
  }

  return digits.replace(/^(\d{3})(\d{3})(\d{3})(\d{2})$/, '$1.$2.$3-$4');
}

function atualizarTipoDocumentoVisual() {
  const tipo = refs.tipoPessoa.value;
  refs.labelNome.textContent = tipo === 'PJ' ? 'Razão Social / Nome Fantasia' : 'Nome Completo';
  refs.labelDocumento.textContent = tipo === 'PJ' ? 'CNPJ' : 'CPF';
  refs.documentoCliente.placeholder = tipo === 'PJ' ? '00.000.000/0000-00' : '000.000.000-00';
  refs.documentoCliente.value = mascararDocumento(refs.documentoCliente.value || '');
}

function aplicarFiltros() {
  const termo = estado.termoBusca.trim().toLowerCase();
  const clientesFiltrados = estado.clientes.filter((cliente) => {
    const concat = [
      cliente.nome,
      cliente.codigo,
      cliente.documento,
      cliente.tipoPessoa,
      cliente.status,
    ]
      .join(' ')
      .toLowerCase();

    const atendeBusca = !termo || concat.includes(termo);
    const atendeTipo = estado.tipoPessoaFiltro === 'TODOS' || cliente.tipoPessoa === estado.tipoPessoaFiltro;
    const atendeStatus = estado.statusFiltro === 'TODOS' || cliente.status === estado.statusFiltro;

    return atendeBusca && atendeTipo && atendeStatus;
  });

  renderTabela(clientesFiltrados);
  refs.contador.textContent = `${clientesFiltrados.length} registro${clientesFiltrados.length === 1 ? '' : 's'}`;

  if (!clientesFiltrados.length) {
    estado.clienteSelecionado = null;
    renderDetalhesVazio();
    return;
  }

  const selecionadoExiste = clientesFiltrados.some((cliente) => cliente.id === estado.clienteSelecionado?.id);
  if (!selecionadoExiste) {
    estado.clienteSelecionado = clientesFiltrados[0];
  }

  renderDetalhes(estado.clienteSelecionado);
}

function renderTabela(clientes) {
  refs.tabela.innerHTML = clientes
    .map(
      (cliente) => `
        <tr data-cliente-id="${cliente.id}" tabindex="0" role="button" aria-label="Selecionar cliente ${cliente.nome}">
          <td>${cliente.codigo}</td>
          <td>${cliente.nome}</td>
          <td><span class="badge ${cliente.tipoPessoa === 'PF' ? 'badge--pf' : 'badge--pj'}">${cliente.tipoPessoa}</span></td>
          <td>${cliente.documento}</td>
          <td>${cliente.telefone}</td>
          <td>
            <div class="table-actions">
              <button type="button" class="icon-button icon-button--edit" data-action="editar" data-id="${cliente.id}" aria-label="Editar cliente ${cliente.nome}">✏️</button>
              <button type="button" class="icon-button icon-button--delete" data-action="inativar" data-id="${cliente.id}" aria-label="Inativar cliente ${cliente.nome}">🗑️</button>
            </div>
          </td>
        </tr>
      `
    )
    .join('');

  refs.tabela.querySelectorAll('tr[data-cliente-id]').forEach((linha) => {
    linha.addEventListener('click', (event) => {
      if (event.target.closest('button')) return;
      const id = Number(linha.dataset.clienteId);
      const cliente = estado.clientes.find((item) => item.id === id);
      if (cliente) {
        estado.clienteSelecionado = cliente;
        renderDetalhes(cliente);
      }
    });
  });

  refs.tabela.querySelectorAll('[data-action]').forEach((botao) => {
    botao.addEventListener('click', (event) => {
      event.stopPropagation();
      const action = botao.dataset.action;
      const id = Number(botao.dataset.id);
      if (action === 'editar') {
        abrirModalEdicao(id);
      }
      if (action === 'inativar') {
        confirmarInativacao(id);
      }
    });
  });
}

function renderDetalhesVazio() {
  refs.detailNome.textContent = 'Nenhum cliente encontrado';
  refs.detailCodigo.textContent = 'Código: -';
  refs.detailTipo.textContent = '-';
  refs.detailDocumento.textContent = '-';
  refs.detailEmail.textContent = '-';
  refs.detailEndereco.textContent = '-';
  refs.detailStatus.textContent = '-';
  refs.metricTotalLocacoes.textContent = '0';
  refs.metricLocacoesAndamento.textContent = '0';
  refs.metricEquipamentosPosse.textContent = '0';
  refs.metricValorAberto.textContent = 'R$ 0,00';
  refs.historicoLocacoes.innerHTML = '<li>Nenhum histórico disponível.</li>';
}

function renderDetalhes(cliente) {
  if (!cliente) {
    renderDetalhesVazio();
    return;
  }

  refs.detailNome.textContent = cliente.nome;
  refs.detailCodigo.textContent = `Código: ${cliente.codigo}`;
  refs.detailTipo.textContent = cliente.tipoPessoa === 'PF' ? 'Pessoa Física' : 'Pessoa Jurídica';
  refs.detailDocumento.textContent = cliente.documento;
  refs.detailEmail.textContent = cliente.email || '-';
  refs.detailEndereco.textContent = cliente.endereco || '-';
  refs.detailStatus.textContent = cliente.status;

  refs.metricTotalLocacoes.textContent = cliente.totalLocacoes ?? 0;
  refs.metricLocacoesAndamento.textContent = cliente.locacoesEmAndamento ?? 0;
  refs.metricEquipamentosPosse.textContent = cliente.equipamentosEmPosse ?? 0;
  refs.metricValorAberto.textContent = formatarMoeda(cliente.valorAberto ?? 0);

  refs.historicoLocacoes.innerHTML = (cliente.historico || [])
    .map(
      (item) => `
        <li class="history-item">
          <div>
            <strong>${item.equipamento}</strong>
            <span>${item.data}</span>
          </div>
          <span class="badge ${statusClass(item.status)}">${item.status}</span>
        </li>
      `
    )
    .join('') || '<li>Nenhum histórico disponível.</li>';
}

function statusClass(status) {
  const classes = {
    EM_ANDAMENTO: 'badge--andamento',
    CONCLUIDA: 'badge--concluida',
    ATRASADA: 'badge--atrasada',
    CANCELADA: 'badge--cancelada',
  };

  return classes[status] || 'badge--default';
}

function abrirModalEdicao(id) {
  const cliente = estado.clientes.find((item) => item.id === id);
  if (!cliente) return;

  estado.editandoClienteId = id;
  refs.modalTitle.textContent = 'Editar Cliente';
  refs.form.reset();
  refs.tipoPessoa.value = cliente.tipoPessoa;
  refs.nomeCliente.value = cliente.nome;
  refs.documentoCliente.value = cliente.documento;
  refs.telefoneCliente.value = cliente.telefone;
  refs.emailCliente.value = cliente.email;
  refs.enderecoCliente.value = cliente.endereco;
  atualizarTipoDocumentoVisual();
  abrirModal();
}

function abrirModalCadastro() {
  estado.editandoClienteId = null;
  refs.modalTitle.textContent = 'Novo Cliente';
  refs.form.reset();
  refs.tipoPessoa.value = 'PF';
  atualizarTipoDocumentoVisual();
  abrirModal();
}

function abrirModal() {
  refs.modal.classList.add('is-open');
  refs.modal.setAttribute('aria-hidden', 'false');
}

function fecharModal() {
  refs.modal.classList.remove('is-open');
  refs.modal.setAttribute('aria-hidden', 'true');
}

function confirmarInativacao(id) {
  const cliente = estado.clientes.find((item) => item.id === id);
  if (!cliente) return;

  const confirmar = window.confirm(`Deseja realmente inativar o cliente ${cliente.nome}?`);
  if (!confirmar) return;

  cliente.status = 'INATIVO';
  aplicarFiltros();
}

function validarFormulario() {
  const nome = refs.nomeCliente.value.trim();
  const documento = refs.documentoCliente.value.replace(/\D/g, '');
  const telefone = refs.telefoneCliente.value.trim();
  const email = refs.emailCliente.value.trim();
  const endereco = refs.enderecoCliente.value.trim();

  if (!nome || !documento || !telefone || !email || !endereco) {
    return 'Preencha todos os campos do cliente.';
  }

  if (refs.tipoPessoa.value === 'PF' && documento.length !== 11) {
    return 'CPF inválido. Informe 11 números.';
  }

  if (refs.tipoPessoa.value === 'PJ' && documento.length !== 14) {
    return 'CNPJ inválido. Informe 14 números.';
  }

  return null;
}

function montarClientePayload() {
  return {
    id: estado.editandoClienteId ?? Date.now(),
    codigo: estado.editandoClienteId ? estado.clientes.find((item) => item.id === estado.editandoClienteId)?.codigo : `CLI-${Date.now().toString().slice(-4)}`,
    nome: refs.nomeCliente.value.trim(),
    tipoPessoa: refs.tipoPessoa.value,
    documento: refs.documentoCliente.value,
    telefone: refs.telefoneCliente.value.trim(),
    email: refs.emailCliente.value.trim(),
    endereco: refs.enderecoCliente.value.trim(),
    status: 'ATIVO',
    totalLocacoes: 0,
    locacoesEmAndamento: 0,
    equipamentosEmPosse: 0,
    valorAberto: 0,
    historico: [],
  };
}

function salvarCliente(event) {
  event.preventDefault();
  const erro = validarFormulario();
  if (erro) {
    window.alert(erro);
    return;
  }

  const payload = montarClientePayload();

  if (estado.editandoClienteId) {
    estado.clientes = estado.clientes.map((cliente) =>
      cliente.id === estado.editandoClienteId ? { ...cliente, ...payload } : cliente
    );
  } else {
    estado.clientes = [payload, ...estado.clientes];
  }

  estado.clienteSelecionado = estado.clientes.find((cliente) => cliente.id === payload.id) || estado.clientes[0];
  fecharModal();
  aplicarFiltros();
}

function inicializarEventos() {
  refs.busca.addEventListener('input', (event) => {
    estado.termoBusca = event.target.value;
    aplicarFiltros();
  });

  refs.clienteSearch.addEventListener('input', (event) => {
    estado.termoBusca = event.target.value;
    aplicarFiltros();
  });

  refs.tipoPessoaFilter.addEventListener('change', (event) => {
    estado.tipoPessoaFiltro = event.target.value;
    aplicarFiltros();
  });

  refs.statusFilter.addEventListener('change', (event) => {
    estado.statusFiltro = event.target.value;
    aplicarFiltros();
  });

  refs.tipoPessoa.addEventListener('change', atualizarTipoDocumentoVisual);
  refs.documentoCliente.addEventListener('input', () => {
    refs.documentoCliente.value = mascararDocumento(refs.documentoCliente.value);
  });

  refs.btnNovoCliente.addEventListener('click', abrirModalCadastro);
  refs.btnNovaLocacao.addEventListener('click', () => {
    if (estado.clienteSelecionado) {
      window.alert(`Fluxo de nova locação para ${estado.clienteSelecionado.nome} em desenvolvimento.`);
    }
  });

  refs.form.addEventListener('submit', salvarCliente);

  document.querySelectorAll('[data-close-modal]').forEach((element) => {
    element.addEventListener('click', fecharModal);
  });

  document.getElementById('btnLogout').addEventListener('click', () => {
    sessionStorage.removeItem('gearrent_token');
    window.location.href = 'login.html';
  });
}

function init() {
  inicializarEventos();
  aplicarFiltros();
  atualizarTipoDocumentoVisual();
  renderDetalhes(estado.clienteSelecionado);
}

init();
