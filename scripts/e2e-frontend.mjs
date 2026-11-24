import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';

const VIEW_DIR = path.resolve('src/main/resources/view');
const JS_DIR = path.join(VIEW_DIR, 'js');
const CSS_DIR = path.join(VIEW_DIR, 'css');

const HTML_PAGES = [
  'index.html',
  'login.html',
  'cadastro.html',
  'esqueci-senha.html',
  'redefinir-senha.html',
  'visao-geral.html',
  'clientes.html',
  'equipamentos.html',
  'locacoes.html',
  'configuracoes.html',
];

const JS_FILES = [
  'config.js',
  'api.js',
  'login.js',
  'cadastro.js',
  'esqueci-senha.js',
  'redefinir-senha.js',
  'visao-geral.js',
  'clientes.js',
  'equipamentos.js',
  'locacoes.js',
  'configuracoes.js',
];

test('E2E Front-end 1: Todas as 10 páginas HTML existem e carregam Axios 1.7.9 padronizado', () => {
  for (const page of HTML_PAGES) {
    const filePath = path.join(VIEW_DIR, page);
    assert.ok(fs.existsSync(filePath), `Página ${page} deve existir`);
    const content = fs.readFileSync(filePath, 'utf-8');
    assert.match(content, /<!DOCTYPE html>/i, `${page} deve possuir DOCTYPE html`);
    if (page !== 'index.html') {
      assert.match(
        content,
        /axios@1\.7\.9\/dist\/axios\.min\.js/,
        `${page} deve usar a versão padronizada axios@1.7.9`,
      );
      assert.match(content, /js\/config\.js/, `${page} deve carregar js/config.js`);
      assert.match(content, /js\/api\.js/, `${page} deve carregar js/api.js`);
    }
  }
});

test('E2E Front-end 2: Todos os 11 módulos JavaScript possuem sintaxe válida', () => {
  for (const jsFile of JS_FILES) {
    const filePath = path.join(JS_DIR, jsFile);
    assert.ok(fs.existsSync(filePath), `Script ${jsFile} deve existir`);
    const code = fs.readFileSync(filePath, 'utf-8');
    assert.doesNotThrow(() => new vm.Script(code, { filename: jsFile }), `Sintaxe válida em ${jsFile}`);
  }
});

test('E2E Front-end 3: UptimeRobot Keep-Alive (30s) configurado em api.js e retorna UP no log interno', async () => {
  const apiCode = fs.readFileSync(path.join(JS_DIR, 'api.js'), 'utf-8');
  assert.match(apiCode, /intervaloMs:\s*30000/, 'Intervalo do UptimeRobot deve ser de 30000ms (30s)');
  assert.match(apiCode, /apiClient\.get\('\/health'\)/, 'Deve consultar a rota /health');

  const logs = [];
  const sandbox = {
    window: { location: { hostname: 'localhost', pathname: '/visao-geral.html', replace: () => {} } },
    document: {
      readyState: 'loading',
      addEventListener: () => {},
      querySelector: () => null,
      querySelectorAll: () => [],
      getElementById: () => null,
    },
    sessionStorage: { getItem: () => null, setItem: () => {}, removeItem: () => {} },
    localStorage: { getItem: () => null, setItem: () => {} },
    console: {
      info: (...args) => logs.push(args.join(' ')),
      warn: (...args) => logs.push(args.join(' ')),
    },
    axios: {
      create: () => ({
        interceptors: { request: { use: () => {} } },
        get: async (url) => {
          assert.equal(url, '/health');
          return { data: { status: 'UP', intervaloSegundos: 30 } };
        },
      }),
    },
    setInterval: () => 123,
    clearInterval: () => {},
  };
  sandbox.window.GR = {};
  vm.createContext(sandbox);
  vm.runInContext(apiCode, sandbox);

  const status = await sandbox.window.GR.uptime.testarConexao();
  assert.equal(status, 'UP');
  assert.ok(
    logs.some((l) => l.includes('Status: UP')),
    'Deve registrar Status: UP no log interno do sistema',
  );
});

test('E2E Front-end 4: Máscara de CPF no cadastro e contratos REST de locação (dataReal, DELETE e ?clienteId)', () => {
  const cadastroCode = fs.readFileSync(path.join(JS_DIR, 'cadastro.js'), 'utf-8');
  const sandbox = {
    window: {},
    document: { getElementById: () => null, querySelector: () => null },
  };
  vm.createContext(sandbox);
  vm.runInContext(cadastroCode, sandbox);

  const input = { value: '12345678901' };
  sandbox.window.mascararCPF(input);
  assert.equal(input.value, '123.456.789-01', 'Máscara de CPF deve formatar 11 dígitos como 000.000.000-00');

  const locacoesCode = fs.readFileSync(path.join(JS_DIR, 'locacoes.js'), 'utf-8');
  assert.match(locacoesCode, /dataReal:\s*refs\.returnDate\.value/, 'Devolução deve enviar dataReal');
  assert.match(locacoesCode, /apiClient\.delete\(`\/locacoes\//, 'Cancelamento deve usar DELETE /locacoes/{id}');
  assert.match(locacoesCode, /params\.get\('clienteId'\)/, 'Deve ler o parâmetro ?clienteId da URL');
});

test('E2E Front-end 5: Formulários públicos suportam envio com Enter (type="submit") e variáveis CSS estão íntegras', () => {
  for (const authPage of ['login.html', 'cadastro.html', 'esqueci-senha.html', 'redefinir-senha.html']) {
    const html = fs.readFileSync(path.join(VIEW_DIR, authPage), 'utf-8');
    assert.match(html, /type="submit"/, `${authPage} deve ter botão principal com type="submit"`);
  }

  const redefinirHtml = fs.readFileSync(path.join(VIEW_DIR, 'redefinir-senha.html'), 'utf-8');
  const formCloseIdx = redefinirHtml.indexOf('</form>');
  const linkNovoIdx = redefinirHtml.indexOf('id="areaLinkNovo"');
  assert.ok(linkNovoIdx > formCloseIdx, '#areaLinkNovo deve estar fora do <form> em redefinir-senha.html');

  const css = fs.readFileSync(path.join(CSS_DIR, 'recuperacao-senha.css'), 'utf-8');
  assert.match(css, /--success-border:/, 'recuperacao-senha.css deve declarar --success-border');
  assert.match(css, /--error-border:/, 'recuperacao-senha.css deve declarar --error-border');
});

test('E2E Docker & Infra 6: Dockerfile, Dockerfile-frontend e docker-compose.yml configuram UptimeRobot (30s)', () => {
  const compose = fs.readFileSync(path.resolve('docker-compose.yml'), 'utf-8');
  assert.match(compose, /UPTIMEROBOT_INTERVALO_MS=30000/, 'docker-compose.yml deve definir intervalo de 30000ms');
  assert.match(compose, /interval:\s*30s/, 'docker-compose.yml deve ter healthcheck com interval: 30s');
  assert.match(compose, /container_name:\s*gearrent-uptimerobot/, 'docker-compose.yml deve ter o serviço gearrent-uptimerobot');

  const dockerfileBackend = fs.readFileSync(path.resolve('Dockerfile'), 'utf-8');
  assert.match(dockerfileBackend, /HEALTHCHECK --interval=30s/, 'Dockerfile deve ter HEALTHCHECK --interval=30s');
  assert.match(dockerfileBackend, /\/api\/health/, 'Dockerfile deve testar /api/health');

  const dockerfileFrontend = fs.readFileSync(path.resolve('Dockerfile-frontend'), 'utf-8');
  assert.match(dockerfileFrontend, /HEALTHCHECK --interval=30s/, 'Dockerfile-frontend deve ter HEALTHCHECK --interval=30s');
});
