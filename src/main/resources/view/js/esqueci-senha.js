// Mostra uma mensagem única de erro e oculta qualquer sucesso anterior.
function mostrarErro(texto) {
  const erroEl = document.getElementById('mensagemErro');
  const sucessoEl = document.getElementById('mensagemSucesso');

  erroEl.textContent = texto;
  erroEl.hidden = false;
  sucessoEl.hidden = true;
  sucessoEl.textContent = '';
}

// Apresenta uma lista de mensagens retornadas pela API.
function mostrarListaErros(mensagens) {
  const erroEl = document.getElementById('mensagemErro');
  const sucessoEl = document.getElementById('mensagemSucesso');

  erroEl.textContent = '';
  erroEl.hidden = false;
  sucessoEl.hidden = true;
  sucessoEl.textContent = '';

  const lista = document.createElement('ul');
  lista.className = 'message-list';

  mensagens.forEach((mensagem) => {
    const item = document.createElement('li');
    item.textContent = mensagem;
    lista.appendChild(item);
  });

  erroEl.appendChild(lista);
}

// Exibe o resultado positivo e remove avisos de erro anteriores.
function mostrarSucesso(texto) {
  const erroEl = document.getElementById('mensagemErro');
  const sucessoEl = document.getElementById('mensagemSucesso');

  sucessoEl.textContent = texto;
  sucessoEl.hidden = false;
  erroEl.hidden = true;
  erroEl.textContent = '';
}

// Limpa as áreas de mensagem antes de uma nova solicitação.
function limparMensagens() {
  const erroEl = document.getElementById('mensagemErro');
  const sucessoEl = document.getElementById('mensagemSucesso');

  erroEl.hidden = true;
  erroEl.textContent = '';

  sucessoEl.hidden = true;
  sucessoEl.textContent = '';
}

// Extrai erros da resposta HTTP ou retorna mensagens adequadas para a conexão.
function extrairMensagens(erro) {
  if (erro?.response?.data?.mensagens && Array.isArray(erro.response.data.mensagens)) {
    return erro.response.data.mensagens;
  }

  if (!erro?.response) {
    return ['Não foi possível conectar ao sistema. Verifique sua internet e tente novamente.'];
  }

  return ['Algo deu errado. Tente novamente em instantes.'];
}

// Indica visualmente se o envio de recuperação está em andamento.
function setBotaoRecuperacao(ativo) {
  const botao = document.getElementById('btnEnviar');
  botao.disabled = !ativo;

  if (!ativo) {
    botao.textContent = 'Enviando...';
    return;
  }

  botao.textContent = 'Enviar Instruções';
}

// Aguarda o intervalo de segurança antes de permitir outro pedido de recuperação.
function iniciarContagemRecuperacao(segundos) {
  const botao = document.getElementById('btnEnviar');
  let restante = segundos;

  const intervalo = setInterval(() => {
    restante -= 1;
    botao.textContent = `Você poderá pedir novamente em ${restante} segundos`;

    if (restante <= 0) {
      clearInterval(intervalo);
      botao.disabled = false;
      botao.textContent = 'Enviar Instruções';
    }
  }, 1000);
}

// Valida o e-mail e solicita à API o envio das instruções de recuperação.
async function solicitarRecuperacao() {
  limparMensagens();

  const email = document.getElementById('email').value.trim();

  if (!email) {
    mostrarErro('Por favor, informe o seu e-mail.');
    return;
  }

  setBotaoRecuperacao(false);

  try {
    const resposta = await axios.post(`${API_URL}/api/auth/esqueci-senha`, { email });

    const mensagem = resposta?.data?.mensagem || 'Instruções enviadas com sucesso.';
    mostrarSucesso(mensagem);
    iniciarContagemRecuperacao(60);
  } catch (erro) {
    const mensagens = extrairMensagens(erro);
    mostrarListaErros(mensagens);
    setBotaoRecuperacao(true);
  }
}

// Expõe a ação para o formulário da página.
window.solicitarRecuperacao = solicitarRecuperacao;
