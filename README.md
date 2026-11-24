# GearRent — Sistema de Locação Inteligente de Equipamentos

> Plataforma Web Multi-Tenant (SaaS) para locação de equipamentos de construção civil, com foco em usabilidade acessível para operadores de mais idade, interface desktop clara e regras de cobrança transparentes.

[![Java 17](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://www.java.com/)
[![Spring Boot 4.1.1](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Springdoc OpenAPI 3.1.1](https://img.shields.io/badge/OpenAPI-3.1.1-85EA2D?logo=swagger&logoColor=black)](https://springdoc.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-database-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![JWT](https://img.shields.io/badge/Security-JWT-000000?logo=jsonwebtokens&logoColor=white)](https://jwt.io/)
[![Vanilla JS](https://img.shields.io/badge/Frontend-Vanilla%20JS-F7DF1E?logo=javascript&logoColor=black)](https://developer.mozilla.org/docs/Web/JavaScript)

---

## 1. Visão Geral

O **GearRent** simplifica todo o ciclo operacional de uma locadora de equipamentos: cadastro de clientes (PF/PJ), catálogo e disponibilidade de equipamentos, abertura de contratos (inclusive retroativos), cálculo automático de diárias/multas, registro de devoluções, cancelamentos e acompanhamento gerencial em tempo real.

### Experiência e Acessibilidade
- **Foco em operadores de mais idade:** tipografia confortável, alto contraste entre fundos claros e texto escuro, botões amplos, rótulos explícitos e navegação por teclado com foco visível.
- **Tema claro e tema escuro:** alternância salva localmente no navegador sem afetar dados ou regras de negócio.
- **Feedback claro:** mensagens de validação e erro em português direto, sem dependência de alertas nativos do navegador.
- **Front-end MPA em Vanilla JS:** páginas HTML independentes em `src/main/resources/view/` com CSS puro (`layout.css`) e requisições HTTP via Axios.

### Arquitetura (DDD Simplificado)
- **`domain`**: entidades JPA, enums, regras de domínio, exceções de negócio e contratos de repositório.
- **`application`**: serviços de caso de uso (`@Transactional`), DTOs e orquestração de regras/cálculos.
- **`infra`**: segurança JWT, tratamento global de exceções (`GlobalExceptionHandler`), e-mail, OpenAPI e keep-alive.
- **`presentation`**: controllers REST com validação (`@Valid`) e documentação Springdoc OpenAPI.

---

## 2. Acesso ao Swagger / OpenAPI

A API expõe documentação interativa via **Springdoc OpenAPI 3.1.1** (`bearerAuth` configurado para testes diretos com token JWT):

| Ambiente | Swagger UI | Especificação OpenAPI (JSON) |
|---|---|---|
| **Produção (Render)** | https://gearrent-eo3x.onrender.com/swagger-ui/index.html | https://gearrent-eo3x.onrender.com/v3/api-docs |
| **Local** | `http://localhost:8080/swagger-ui/index.html` (ou `/swagger-ui.html`) | `http://localhost:8080/v3/api-docs` |

> **Dica para testar rotas protegidas no Swagger UI:** autentique-se em `POST /api/auth/login`, copie o campo `token` da resposta, clique em **Authorize** no topo da página do Swagger e cole apenas o token JWT.

---

## 3. Autenticação & Multi-Tenancy SaaS

### Fluxo de Autenticação
1. **Onboarding de nova locadora (`POST /api/usuarios/cadastro`):** rota pública que cria simultaneamente a nova `Locadora` (tenant) e o primeiro usuário com papel `ROLE_ADMIN`.
2. **Login (`POST /api/auth/login`):** rota pública que valida e-mail e senha (BCrypt) e emite um **JWT** assinado contendo a identidade do usuário, seu papel (`ROLE_ADMIN` ou `ROLE_OPERADOR`) e o identificador da locadora.
3. **Recuperação de senha (`POST /api/auth/esqueci-senha` e `POST /api/auth/redefinir-senha`):** rotas públicas com token temporário de 30 minutos.
4. **Rotas protegidas:** todas as demais rotas da API exigem o cabeçalho HTTP:
   ```http
   Authorization: Bearer <token>
   ```

### Isolamento Multi-Tenant Invisível (`locadoraId`)
- **Nunca envie `locadoraId` no corpo, query string ou URL.**
- Em toda requisição autenticada, o `JwtFilter` valida o token e popula o `UsuarioAutenticado` no contexto do Spring Security (`@AuthenticationPrincipal`).
- Todas as consultas, cadastros, atualizações, exclusões e cálculos recebem `usuario.locadoraId()` internamente na camada de apresentação e restringem os dados estritamente à base autenticada, impedindo acesso cruzado entre locadoras (IDOR).

---

## 4. Catálogo Resumido de Endpoints da API

### Módulo: Auth (`/api/auth`)
| Método | Path / Rota | Descrição Sintética | Autenticação |
|---|---|---|---|
| `POST` | `/api/auth/login` | Autentica usuário e emite token JWT | Pública |
| `POST` | `/api/auth/esqueci-senha` | Solicita envio de token de recuperação de senha (30 min) | Pública |
| `POST` | `/api/auth/redefinir-senha` | Redefine a senha usando o token temporário recebido | Pública |

### Módulo: Usuários (`/api/usuarios`)
| Método | Path / Rota | Descrição Sintética | Autenticação |
|---|---|---|---|
| `POST` | `/api/usuarios/cadastro` | Cadastra administrador inicial e cria nova locadora (tenant) | Pública |
| `GET` | `/api/usuarios` | Lista usuários da locadora autenticada (filtro opcional `nome`) | JWT |
| `POST` | `/api/usuarios` | Cria novo operador na locadora autenticada | JWT (`ROLE_ADMIN`) |
| `PUT` | `/api/usuarios/{id}` | Atualiza dados cadastrais de usuário da locadora | JWT (`ROLE_ADMIN` ou próprio usuário) |
| `DELETE` | `/api/usuarios/{id}` | Exclui logicamente um usuário da locadora | JWT (`ROLE_ADMIN`) |
| `PATCH` | `/api/usuarios/{id}/desativar` | Desativa acesso de usuário (`INATIVO`), exceto a si mesmo | JWT (`ROLE_ADMIN`) |
| `PATCH` | `/api/usuarios/{id}/reativar` | Reativa usuário inativo da locadora | JWT (`ROLE_ADMIN`) |
| `GET` | `/api/usuarios/me` | Consulta o perfil do usuário autenticado | JWT |
| `PUT` | `/api/usuarios/me` | Atualiza nome, sobrenome, e-mail e CPF do próprio perfil | JWT |
| `PATCH` | `/api/usuarios/trocar-senha` | Altera a senha do usuário autenticado | JWT |
| `POST` | `/api/usuarios/avatar` | Envia/substitui foto de perfil (`multipart/form-data`, até 2 MB) | JWT |
| `GET` | `/api/usuarios/me/avatar` | Obtém os bytes da foto de perfil do usuário autenticado | JWT |
| `GET` | `/api/usuarios/{id}/avatar` | Obtém os bytes da foto de perfil de usuário da mesma locadora | JWT |
| `DELETE` | `/api/usuarios/avatar` | Remove a foto de perfil do usuário autenticado | JWT |

### Módulo: Clientes (`/api/clientes`)
| Método | Path / Rota | Descrição Sintética | Autenticação |
|---|---|---|---|
| `POST` | `/api/clientes` | Cadastra cliente (PF/PJ) ou reativa cadastro removido pelo documento | JWT |
| `GET` | `/api/clientes` | Lista clientes ativos da locadora (filtro opcional `nome`) | JWT |
| `GET` | `/api/clientes/{id}` | Consulta detalhes completos de um cliente da locadora | JWT |
| `PUT` | `/api/clientes/{id}` | Atualiza dados cadastrais, contatos e endereço do cliente | JWT |
| `GET` | `/api/clientes/{id}/resumo` | Retorna resumo de locações, equipamentos em posse e valor em aberto | JWT |
| `GET` | `/api/clientes/{id}/historico` | Lista histórico de locações do cliente (mais recentes primeiro) | JWT |
| `DELETE` | `/api/clientes/{id}` | Exclui logicamente o cliente (bloqueado se houver locação ativa/atrasada) | JWT |

### Módulo: Equipamentos (`/api/equipamentos`)
| Método | Path / Rota | Descrição Sintética | Autenticação |
|---|---|---|---|
| `POST` | `/api/equipamentos` | Cadastra equipamento no estoque da locadora | JWT |
| `GET` | `/api/equipamentos` | Lista equipamentos paginados com filtros (`nome`, `categoria`, `status`) | JWT |
| `GET` | `/api/equipamentos/disponiveis` | Lista apenas equipamentos com status `DISPONIVEL` | JWT |
| `GET` | `/api/equipamentos/{id}` | Consulta detalhes de um equipamento da locadora | JWT |
| `PUT` | `/api/equipamentos/{id}` | Atualiza dados do equipamento (bloqueado se `ALUGADO`) | JWT |
| `DELETE` | `/api/equipamentos/{id}` | Exclui logicamente o equipamento (bloqueado se `ALUGADO`) | JWT |

### Módulo: Locações (`/api/locacoes`)
| Método | Path / Rota | Descrição Sintética | Autenticação |
|---|---|---|---|
| `POST` | `/api/locacoes` | Abre contrato (inclusive retroativo), aplica lock nos itens e calcula diárias/multa | JWT |
| `GET` | `/api/locacoes/{id}` | Consulta detalhes do contrato (calcula acumulado até hoje se atrasada) | JWT |
| `PUT` | `/api/locacoes/{id}` | Edita locação `EM_ANDAMENTO` e recalcula diárias/multas | JWT |
| `PATCH` | `/api/locacoes/{id}/devolucao` | Registra devolução, libera equipamentos e fixa valor final | JWT |
| `PATCH` | `/api/locacoes/{id}/cancelamento` | Cancela locação antes da retirada (`CANCELADA`) e libera itens | JWT |
| `DELETE` | `/api/locacoes/{id}` | Alias legado para cancelamento (prefira o `PATCH`) | JWT |

### Módulo: Visão Geral / BFF (`/api/visao-geral`)
| Método | Path / Rota | Descrição Sintética | Autenticação |
|---|---|---|---|
| `GET` | `/api/visao-geral/resumo` | Retorna KPIs consolidados do dashboard (locações, receitas e estoque) | JWT |
| `GET` | `/api/visao-geral/filtros-opcoes` | Retorna categorias, modelos e clientes para popular filtros do painel | JWT |
| `GET` | `/api/visao-geral/locacoes` | Lista locações paginadas com múltiplos filtros combinados (máx. 50/página) | JWT |

### Módulo: Locadora (`/api/locadora`)
| Método | Path / Rota | Descrição Sintética | Autenticação |
|---|---|---|---|
| `GET` | `/api/locadora` | Consulta os dados cadastrais da locadora autenticada | JWT |
| `PUT` | `/api/locadora` | Atualiza nome fantasia, telefone e endereço (CNPJ é imutável) | JWT (`ROLE_ADMIN`) |

### Módulos Auxiliares (`/api/financeiro`, `/api/auditoria`, `/api/cep`, `/api/health`)
| Método | Path / Rota | Descrição Sintética | Autenticação |
|---|---|---|---|
| `GET` | `/api/financeiro/resumo` | Resumo de receita e valores em aberto por período (máx. 24 meses) | JWT |
| `GET` | `/api/auditoria` | Lista últimos 200 logs de auditoria da locadora | JWT (`ROLE_ADMIN`) |
| `GET` | `/api/cep/{cep}` | Consulta endereço via ViaCEP para autopreenchimento | Pública |
| `GET` | `/api/health` / `/api/ping` | Healthcheck do servidor e banco de dados (Keep-Alive) | Pública |

---

## 5. Padrão de Resposta de Erros (`ApiErroResposta`)

Todas as exceções de validação, autenticação, autorização, conflito e regras de negócio são interceptadas pelo `GlobalExceptionHandler` (e pelos handlers do `SecurityConfig` para 401/403) e retornadas no formato padronizado `ApiErroResposta`, facilitando a exibição direta no front-end Vanilla JS:

```json
{
  "status": 422,
  "dataHora": "2026-11-24T15:10:00.123456",
  "mensagens": [
    "Este cliente está inativo. Reative o cadastro antes de criar uma locação."
  ]
}
```

### Principais Códigos HTTP Retornados
- **`200 OK` / `201 Created` / `204 No Content`**: operação concluída com sucesso.
- **`400 Bad Request`**: falha de validação de campos (`@Valid`), JSON malformado ou token de recuperação inválido.
- **`401 Unauthorized`**: token JWT ausente, inválido ou expirado, ou credenciais inválidas no login.
- **`403 Forbidden`**: usuário autenticado sem permissão para a ação (ex.: operador tentando acessar rota exclusiva de `ROLE_ADMIN`).
- **`404 Not Found`**: recurso inexistente ou pertencente a outra locadora (isolamento multi-tenant).
- **`409 Conflict`**: duplicidade de CPF/CNPJ, e-mail ou patrimônio na locadora, ou equipamento indisponível.
- **`413 Payload Too Large`**: upload de avatar acima de 2 MB.
- **`422 Unprocessable Entity`**: violação de regra de negócio (ex.: data prevista inválida, cliente com locação ativa, equipamento alugado).

---

## 6. Como Executar Localmente

### Pré-requisitos
- **JDK 17**
- **PostgreSQL** em execução
- **Maven 3.8+** ou o Maven Wrapper (`mvnw` / `mvnw.cmd`)

### Configuração e Inicialização
1. Crie o banco de dados:
   ```sql
   CREATE DATABASE gearrent_db;
   ```
2. Ajuste as variáveis de ambiente ou `src/main/resources/application.properties` (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET`).
3. Inicie o back-end:
   ```bash
   # Windows PowerShell
   .\mvnw.cmd spring-boot:run

   # Linux / macOS
   ./mvnw spring-boot:run
   ```
4. Execute os testes:
   ```bash
   .\mvnw.cmd test
   ```
5. Para abrir o front-end localmente, sirva `src/main/resources/view/` com um servidor HTTP estático (ex.: Live Server) e ajuste `API_URL` em `src/main/resources/view/js/config.js` para `http://localhost:8080` caso queira apontar para a API local.
