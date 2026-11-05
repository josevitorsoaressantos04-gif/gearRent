// Guarda o token de uso único recebido no link de redefinição.
let tokenAtual = '';

// Mostra um erro e oculta a mensagem de sucesso.
function mostrarErro(texto) {
  const erroEl = document.getElementById('mensagemErro');
  const sucessoEl = document.getElementById('mensagemSucesso');

  erroEl.textContent = texto;
  erroEl.hidden = false;
  sucessoEl.hidden = true;
  sucessoEl.textContent = '';
}

// Mostra o resultado positivo e remove mensagens de erro anteriores.
function mostrarSucesso(texto) {
  const erroEl = document.getElementById('mensagemErro');
  const sucessoEl = document.getElementById('mensagemSucesso');

  sucessoEl.textContent = texto;
  sucessoEl.hidden = false;
  erroEl.hidden = true;
  erroEl.textContent = '';
}

// Limpa as áreas de aviso antes de iniciar uma nova ação.
function limparMensagens() {
  const erroEl = document.getElementById('mensagemErro');
  const sucessoEl = document.getElementById('mensagemSucesso');

  erroEl.hidden = true;
  erroEl.textContent = '';

  sucessoEl.hidden = true;
  sucessoEl.textContent = '';
}

// Converte erros da API ou de conexão em mensagens exibíveis.
function extrairMensagens(erro) {
  if (erro?.response?.data?.mensagens && Array.isArray(erro.response.data.mensagens)) {
    return erro.response.data.mensagens;
  }

  if (!erro?.response) {
    return ['Não foi possível conectar ao sistema. Verifique sua internet e tente novamente.'];
  }

  return ['Algo deu errado. Tente novamente em instantes.'];
}

// Alterna a visibilidade dos dois campos de senha ao mesmo tempo.
function alternarVisibilidadeSenha() {
  const novaSenha = document.getElementById('novaSenha');
  const confirmacaoSenha = document.getElementById('confirmacaoSenha');
  const mostrar = document.getElementById('btnMostrarSenha');

  const tipoAtual = novaSenha.type === 'password' ? 'text' : 'password';
  novaSenha.type = tipoAtual;
  confirmacaoSenha.type = tipoAtual;
  mostrar.textContent = tipoAtual === 'password' ? 'Mostrar senhas' : 'Ocultar senhas';
}

// Atualiza disponibilidade e rótulo do botão de salvamento.
function setBotaoSalvar(ativo, texto) {
  const botao = document.getElementById('btnSalvar');
  botao.disabled = !ativo;
  botao.textContent = texto;
}

// Oculta o formulário quando o link recebido não pode ser utilizado.
function esconderFormulario() {
  document.getElementById('formRedefinir').hidden = true;
  document.getElementById('areaLinkNovo').hidden = false;
}

// Verifica tamanho da nova senha e igualdade entre senha e confirmação.
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

// Envia a nova senha com o token e trata sucesso, expiração ou falha.
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
        window.location.href = 'login.html';
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

// Lê o token do link, remove-o da barra de endereço e prepara o formulário.
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

// Inicializa a tela ao carregar e expõe as ações usadas pelo HTML.
window.onload = inicializarPaginaRedefinicao;
window.alternarVisibilidadeSenha = alternarVisibilidadeSenha;
window.redefinirSenha = redefinirSenha;
