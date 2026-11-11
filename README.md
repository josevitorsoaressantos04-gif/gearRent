# GearRent — Sistema de Locação Inteligente de Equipamentos

> Plataforma web multi-tenant (SaaS) para locação de equipamentos de construção civil, com foco em uma experiência desktop clara e acessível para operadores de diferentes idades.

[![Java 17](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://www.java.com/)
[![Spring Boot 4.1.1](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-database-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![JWT](https://img.shields.io/badge/Security-JWT-000000?logo=jsonwebtokens&logoColor=white)](https://jwt.io/)
[![Cucumber BDD](https://img.shields.io/badge/BDD-Cucumber%20planned-23D96C?logo=cucumber&logoColor=white)](https://cucumber.io/)
[![Vanilla JS](https://img.shields.io/badge/Frontend-Vanilla%20JS-F7DF1E?logo=javascript&logoColor=black)](https://developer.mozilla.org/docs/Web/JavaScript)

## Sobre o projeto

O GearRent simplifica o ciclo de locação: cadastro de clientes e equipamentos, criação de contratos, acompanhamento de diárias e registro de devoluções. O sistema ajuda locadoras a controlar disponibilidade, reduzir custos operacionais e apresentar valores e regras de cobrança com transparência.

### Experiência e acessibilidade

A aplicação web foi pensada prioritariamente para uso em desktop. A interface privilegia contraste entre fundos claros e texto escuro, hierarquia visual evidente, controles espaçosos e linguagem direta em português. O estilo base usa tipografia de 18 px; alguns componentes específicos do painel podem ter dimensões diferentes.

Erros de validação e regras de negócio são apresentados por mensagens compreensíveis, sem depender de alertas nativos do navegador. O projeto também utiliza rótulos e estados de foco visíveis para apoiar a navegação por teclado.

## Arquitetura e decisões técnicas

### DDD simplificado

O código é organizado por responsabilidades, inspirado em Domain-Driven Design:

- **`domain`**: entidades JPA, enums, regras de domínio, exceções de negócio e contratos de repositório.
- **`application`**: serviços que implementam casos de uso, transações com `@Transactional` e cálculos de locação e valores financeiros.
- **`infra`**: autenticação e autorização JWT, tratamento global de exceções, e-mail e configurações técnicas.
- **`presentation`**: controllers REST, validação dos contratos e documentação OpenAPI.

### Isolamento multi-tenant

Cada usuário pertence a uma `Locadora`. O `locadoraId` é obtido a partir do JWT validado e fornecido ao contexto autenticado; os endpoints não recebem o tenant como dado confiável de formulário ou URL. As consultas e operações de domínio usam esse identificador para restringir o acesso aos dados da base autenticada.

### Histórico e reativação

Registros relevantes são preservados por exclusão lógica, com estados como `INATIVO` ou `CANCELADO`, em vez de remoção física. No cadastro de clientes, o sistema procura um documento já existente na mesma locadora e pode reativar o cadastro correspondente, mantendo seu histórico.

## Stack tecnológica

| Área | Tecnologias |
|---|---|
| Back-end | Java 17, Spring Boot 4.1.1, Spring Web MVC, Spring Security, Spring Data JPA, Hibernate, Jakarta Validation e Lombok |
| Persistência | PostgreSQL |
| Autenticação | Auth0 Java JWT 4.4.0, BCrypt e autorização por papéis |
| Documentação da API | Springdoc OpenAPI / Swagger UI |
| Front-end | HTML5, CSS3 e JavaScript Vanilla (ES6+); Axios carregado por CDN |
| Testes atuais | JUnit 5 e testes de contexto Spring Boot |

> **BDD / Cucumber:** Gherkin e Cucumber são objetivos de evolução, mas ainda não estão configurados nas dependências ou nos testes do repositório. Os cenários abaixo são exemplos de especificação, não testes executáveis. A suíte atual é executada com `mvn test` ou com o Maven Wrapper.

## Estrutura do projeto

```text
gearRent/
├── pom.xml
├── mvnw
├── mvnw.cmd
└── src/
    ├── main/
    │   ├── java/com/example/gearrent/
    │   │   ├── application/
    │   │   │   ├── DTO/
    │   │   │   └── service/
    │   │   ├── domain/
    │   │   │   ├── entities/
    │   │   │   ├── enums/
    │   │   │   ├── exception/
    │   │   │   ├── repository/
    │   │   │   └── service/
    │   │   ├── infra/
    │   │   │   ├── config/
    │   │   │   ├── email/
    │   │   │   ├── exception/
    │   │   │   └── security/
    │   │   └── presentation/
    │   └── resources/
    │       ├── application.properties
    │       └── view/
    │           ├── css/
    │           ├── js/
    │           ├── index.html
    │           └── ... páginas da aplicação
    └── test/
        └── java/com/example/gearrent/
            ├── GearRentApplicationTests.java
            └── domain/entities/LocacaoCancellationTest.java
```

## API REST

Todas as rotas protegidas exigem `Authorization: Bearer <token>`. O tenant é resolvido pelo JWT.

| Acesso | Método e rota | Finalidade |
|---|---|---|
| Público | `POST /api/usuarios/cadastro` | Cadastro inicial de usuário administrador e criação da locadora |
| Público | `POST /api/auth/login` | Autenticação e emissão do JWT |
| Público | `POST /api/auth/esqueci-senha` | Solicitação de recuperação de senha |
| Público | `POST /api/auth/redefinir-senha` | Redefinição usando token temporário |
| JWT | `/api/locacoes` | Criação, consulta, devolução e cancelamento de locações |
| JWT | `/api/clientes` | Gestão de clientes da locadora autenticada |
| JWT | `/api/equipamentos` | Catálogo com filtros e paginação; o tenant vem do token |
| JWT | `/api/usuarios` | Gestão de usuários, perfil, operadores e senhas |
| JWT | `/api/locadora` | Consulta e atualização dos dados da base |
| JWT | `/api/financeiro` | Resumo de receitas e valores em aberto |
| JWT | `/api/visao-geral` | Indicadores e consultas para o painel |

O cancelamento de locação por `DELETE /api/locacoes/{id}` é lógico: preserva o contrato como `CANCELADA` e libera os equipamentos quando as regras permitem.

A documentação interativa fica disponível em [`/swagger-ui/index.html`](http://localhost:8080/swagger-ui/index.html) após iniciar a API.

## Testes

Execute os testes disponíveis com Maven:

```bash
mvn test
```

Ou, sem Maven instalado globalmente:

```bash
# Windows PowerShell
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```

Os testes existentes incluem o carregamento do contexto Spring e as regras de cancelamento de locação. Cucumber, MockMvc e REST Assured ainda não fazem parte da configuração atual.

### Exemplo de cenário Gherkin proposto

O exemplo ilustra uma possível especificação futura em português. Ele não corresponde a um arquivo `.feature` presente no projeto.

```gherkin
# Exemplo futuro: src/test/resources/features/locacao.feature
Funcionalidade: Cancelamento de locação

  Cenário: Cancelar uma locação antes da retirada
    Dado que existe uma locação em andamento para a minha base
    E a data de retirada ainda não chegou
    Quando solicito o cancelamento da locação
    Então a locação deve ficar com status "CANCELADA"
    E os equipamentos devem ficar disponíveis novamente
```

## Como executar localmente

### Pré-requisitos

- JDK 17.
- PostgreSQL em execução.
- Maven 3.8+ ou o Maven Wrapper incluído no repositório.

### Banco de dados e configurações

Crie o banco configurado pela aplicação:

```sql
CREATE DATABASE "gearRent-db";
```

Configure as propriedades de conexão em `src/main/resources/application.properties` de acordo com o seu ambiente:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/gearRent-db
spring.datasource.username=SEU_USUARIO
spring.datasource.password=SUA_SENHA
```

Defina um segredo forte para o JWT usando a variável `JWT_SECRET`. Não use o segredo de desenvolvimento em produção nem versione senhas ou credenciais. Para envio de e-mail, configure também as variáveis SMTP disponíveis no arquivo de propriedades, como `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME` e `MAIL_PASSWORD`.

### Iniciar o back-end

```bash
# Maven instalado
mvn spring-boot:run

# Windows PowerShell
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

Por padrão, a API fica disponível em `http://localhost:8080`.

### Abrir o front-end

Sirva `src/main/resources/view/` com um servidor HTTP estático, como a extensão Live Server do VS Code. O ponto de entrada público é `index.html`; por exemplo:

```text
http://127.0.0.1:5500/src/main/resources/view/index.html
```

O cliente HTTP do front-end está configurado para acessar `http://localhost:8080/api`. Ajuste essa origem em `src/main/resources/view/js/api.js` caso a API use outro endereço.

### Swagger UI

Com o back-end em execução, acesse:

```text
http://localhost:8080/swagger-ui/index.html
```
