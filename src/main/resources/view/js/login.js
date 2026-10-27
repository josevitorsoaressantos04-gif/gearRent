function limparMensagens() {
  const mensagemErro = document.getElementById('mensagemErro');
  const mensagemSucesso = document.getElementById('mensagemSucesso');

  mensagemErro.hidden = true;
  mensagemErro.textContent = '';

  mensagemSucesso.hidden = true;
  mensagemSucesso.textContent = '';
}

function mostrarMensagem(tipo, texto) {
  const mensagemErro = document.getElementById('mensagemErro');
  const mensagemSucesso = document.getElementById('mensagemSucesso');

  if (tipo === 'erro') {
    mensagemErro.textContent = texto;
    mensagemErro.hidden = false;
    mensagemSucesso.hidden = true;
    mensagemSucesso.textContent = '';
    return;
  }

  mensagemSucesso.textContent = texto;
  mensagemSucesso.hidden = false;
  mensagemErro.hidden = true;
  mensagemErro.textContent = '';
}

function realizarLogin() {
  limparMensagens();

  const email = document.getElementById('email').value.trim();
  const senha = document.getElementById('senha').value.trim();

  if (!email || !senha) {
    mostrarMensagem('erro', 'Informe seu e-mail e sua senha para continuar.');
    return;
  }

  const payload = {
    email: email,
    senha: senha,
  };

  apiClient
    .post('/auth/login', payload)
    .then((response) => {
      const token = response.data.token;
      const mensagem = response.data.mensagem || 'Login efetuado com sucesso!';

      sessionStorage.setItem('gearrent_token', token);
      mostrarMensagem('sucesso', mensagem);
      document.getElementById('loginForm').reset();

      setTimeout(() => {
        window.location.href = 'visao-geral.html';
      }, 600);
    })
    .catch((erro) => {
      const mensagens = erro?.response?.data?.mensagens;

      if (Array.isArray(mensagens) && mensagens.length > 0) {
        mostrarMensagem('erro', mensagens.join(' '));
      } else {
        mostrarMensagem('erro', 'Não foi possível entrar no sistema. Tente novamente.');
      }
    });
}

window.realizarLogin = realizarLogin;
