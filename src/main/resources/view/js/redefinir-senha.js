let tokenAtual = '';

function mostrarErro(texto) {
  const erroEl = document.getElementById('mensagemErro');
  const sucessoEl = document.getElementById('mensagemSucesso');

  erroEl.textContent = texto;
  erroEl.hidden = false;
  sucessoEl.hidden = true;
  sucessoEl.textContent = '';
}

function mostrarSucesso(texto) {
  const erroEl = document.getElementById('mensagemErro');
  const sucessoEl = document.getElementById('mensagemSucesso');

  sucessoEl.textContent = texto;
  sucessoEl.hidden = false;
  erroEl.hidden = true;
  erroEl.textContent = '';
}

function limparMensagens() {
  const erroEl = document.getElementById('mensagemErro');
  const sucessoEl = document.getElementById('mensagemSucesso');

  erroEl.hidden = true;
  erroEl.textContent = '';

  sucessoEl.hidden = true;
  sucessoEl.textContent = '';
}

function extrairMensagens(erro) {
  if (erro?.response?.data?.mensagens && Array.isArray(erro.response.data.mensagens)) {
    return erro.response.data.mensagens;
  }

  if (!erro?.response) {
    return ['Não foi possível conectar ao sistema. Verifique sua internet e tente novamente.'];
  }

  return ['Algo deu errado. Tente novamente em instantes.'];
}

function alternarVisibilidadeSenha() {
  const novaSenha = document.getElementById('novaSenha');
  const confirmacaoSenha = document.getElementById('confirmacaoSenha');
  const mostrar = document.getElementById('btnMostrarSenha');

  const tipoAtual = novaSenha.type === 'password' ? 'text' : 'password';
  novaSenha.type = tipoAtual;
  confirmacaoSenha.type = tipoAtual;
  mostrar.textContent = tipoAtual === 'password' ? 'Mostrar senhas' : 'Ocultar senhas';
}

function setBotaoSalvar(ativo, texto) {
  const botao = document.getElementById('btnSalvar');
  botao.disabled = !ativo;
  botao.textContent = texto;
}

function esconderFormulario() {
  document.getElementById('formRedefinir').hidden = true;
  document.getElementById('areaLinkNovo').hidden = false;
}

function validarCamposAntesDoEnvio() {
  const novaSenha = document.getElementById('novaSenha').value;
  const confirmacaoSenha = document.getElementById('confirmacaoSenha').value;

  if (novaSenha.length < 6) {
    return 'A nova senha deve ter no mínimo 6 caracteres.';
  }

  if (novaSenha !== confirmacaoSenha) {
    return 'As senhas digitadas não são iguais. Digite novamente.';
  }

  return null;
}

async function redefinirSenha() {
  limparMensagens();

  const erroValidacao = validarCamposAntesDoEnvio();
  if (erroValidacao) {
    mostrarErro(erroValidacao);
    return;
  }

  if (!tokenAtual) {
    mostrarErro('Este link não é válido. Peça um novo link para criar a sua senha.');
    esconderFormulario();
    return;
  }

  setBotaoSalvar(false, 'Salvando...');

  try {
    const resposta = await axios.post(`${API_URL}/api/auth/redefinir-senha`, {
      token: tokenAtual,
      novaSenha: document.getElementById('novaSenha').value,
      confirmacaoSenha: document.getElementById('confirmacaoSenha').value,
    });

    mostrarSucesso(`${resposta.data.mensagem} Você será levado para a tela de entrada em 3 segundos...`);
    document.getElementById('novaSenha').value = '';
    document.getElementById('confirmacaoSenha').value = '';
    setBotaoSalvar(false, 'Senha salva');

    let segundos = 3;
    const intervalo = setInterval(() => {
      segundos -= 1;
      mostrarSucesso(`Senha alterada com sucesso! Você será levado para a tela de entrada em ${segundos} segundos...`);

      if (segundos <= 0) {
        clearInterval(intervalo);
        window.location.href = 'index.html';
      }
    }, 1000);
  } catch (erro) {
    const mensagens = extrairMensagens(erro);
    const mensagem = mensagens[0] || 'Algo deu errado. Tente novamente em instantes.';
    mostrarErro(mensagem);

    if (erro?.response?.status === 400 && /link inválido|expirado|já usado/i.test(mensagem)) {
      document.getElementById('areaLinkNovo').hidden = false;
    }

    setBotaoSalvar(true, 'Salvar Nova Senha');
  }
}

function inicializarPaginaRedefinicao() {
  const params = new URLSearchParams(window.location.search);
  tokenAtual = params.get('token') || '';

  window.history.replaceState(null, '', window.location.pathname);

  if (!tokenAtual) {
    mostrarErro('Este link não é válido. Peça um novo link para criar a sua senha.');
    esconderFormulario();
    return;
  }

  document.getElementById('novaSenha').focus();
}

window.onload = inicializarPaginaRedefinicao;
window.alternarVisibilidadeSenha = alternarVisibilidadeSenha;
window.redefinirSenha = redefinirSenha;
