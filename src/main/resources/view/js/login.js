// Limpa os avisos que foram apresentados na tentativa anterior.
function limparMensagens() {
  const mensagemErro = document.getElementById('mensagemErro');
  const mensagemSucesso = document.getElementById('mensagemSucesso');

  mensagemErro.hidden = true;
  mensagemErro.textContent = '';

  mensagemSucesso.hidden = true;
  mensagemSucesso.textContent = '';
}

// Exibe o aviso solicitado e oculta a mensagem do tipo oposto.
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

// Valida as credenciais, autentica o usuário e inicia a sessão local.
async function realizarLogin(event) {
  if (event && typeof event.preventDefault === 'function') {
    event.preventDefault();
  }
  limparMensagens();

  const email = document.getElementById('email').value.trim();
  const senha = document.getElementById('senha').value.trim();
  const btnLogin = document.getElementById('btnLogin') || document.querySelector('#loginForm button');

  if (!email || !senha) {
    mostrarMensagem('erro', 'Informe seu e-mail e sua senha para continuar.');
    return;
  }

  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    mostrarMensagem('erro', 'O e-mail informado não parece correto. Exemplo: nome@email.com');
    return;
  }

  if (btnLogin) {
    btnLogin.disabled = true;
    btnLogin.textContent = 'Entrando...';
  }

  try {
    const response = await apiClient.post('/auth/login', { email, senha });
    const token = response.data.token;
    const mensagem = response.data.mensagem || 'Login efetuado com sucesso!';

    sessionStorage.setItem('gearrent_token', token);
    mostrarMensagem('sucesso', mensagem);
    document.getElementById('loginForm').reset();

    setTimeout(() => {
      window.location.href = 'visao-geral.html';
    }, 600);
  } catch (erro) {
    if (btnLogin) {
      btnLogin.disabled = false;
      btnLogin.textContent = 'Entrar';
    }
    const mensagens = erro?.response?.data?.mensagens;

    if (Array.isArray(mensagens) && mensagens.length > 0) {
      mostrarMensagem('erro', mensagens.join(' '));
    } else {
      mostrarMensagem('erro', 'Não foi possível entrar no sistema. Tente novamente.');
    }
  }
}

document.getElementById('loginForm')?.addEventListener('submit', realizarLogin);

// Disponibiliza a rotina para o manipulador declarado na página.
window.realizarLogin = realizarLogin;
