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

function mascararCPF(input) {
  let valor = input.value.replace(/\D/g, '').slice(0, 11);

  if (valor.length <= 9) {
    input.value = valor.replace(/(\d{3})(\d)/, '$1.$2');
    return;
  }

  if (valor.length <= 11) {
    input.value = valor.replace(/(\d{3})(\d{3})(\d{1,2})$/, '$1.$2-$3');
    return;
  }

  input.value = valor.replace(/(\d{3})(\d{3})(\d{3})(\d{2})$/, '$1.$2.$3-$4');
}

function validarFormularioCadastro() {
  const nome = document.getElementById('nome').value.trim();
  const sobrenome = document.getElementById('sobrenome').value.trim();
  const dataNascimento = document.getElementById('dataNascimento').value;
  const cpf = document.getElementById('cpf').value.replace(/\D/g, '');
  const email = document.getElementById('email').value.trim();
  const senha = document.getElementById('senha').value;
  const confirmarSenha = document.getElementById('confirmarSenha').value;

  if (!nome || !sobrenome || !dataNascimento || !cpf || !email || !senha || !confirmarSenha) {
    return 'Preencha todos os campos para concluir o cadastro.';
  }

  if (cpf.length !== 11 || !/^\d+$/.test(cpf)) {
    return 'Informe um CPF válido com 11 números.';
  }

  if (senha.length < 6) {
    return 'A senha deve ter pelo menos 6 caracteres.';
  }

  if (senha !== confirmarSenha) {
    return 'A senha e a confirmação de senha precisam ser iguais.';
  }

  return null;
}

function realizarCadastro() {
  limparMensagens();

  const erroValidacao = validarFormularioCadastro();
  if (erroValidacao) {
    mostrarMensagem('erro', erroValidacao);
    return;
  }

  const payload = {
    nome: document.getElementById('nome').value.trim(),
    sobrenome: document.getElementById('sobrenome').value.trim(),
    dataNascimento: document.getElementById('dataNascimento').value,
    cpf: document.getElementById('cpf').value.replace(/\D/g, ''),
    email: document.getElementById('email').value.trim(),
    senha: document.getElementById('senha').value,
  };

  apiClient
    .post('/usuarios/cadastro', payload)
    .then((response) => {
      mostrarMensagem(
        'sucesso',
        'Cadastro realizado com sucesso! Sua base pessoal foi criada. Redirecionando para o login...'
      );

      document.getElementById('cadastroForm').reset();

      setTimeout(() => {
        window.location.href = 'login.html';
      }, 2000);
    })
    .catch((erro) => {
      const mensagens = erro?.response?.data?.mensagens;

      if (Array.isArray(mensagens) && mensagens.length > 0) {
        mostrarMensagem('erro', mensagens.join(' '));
      } else {
        mostrarMensagem('erro', 'Não foi possível concluir o cadastro. Tente novamente.');
      }
    });
}

window.realizarCadastro = realizarCadastro;
