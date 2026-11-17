PENDÊNCIAS TÉCNICAS DO BACK-END PARA SUPORTAR A TELA DE GESTÃO DE CLIENTES

1. DTOs faltantes

1.1 ClienteListagemDto
Campos sugeridos:
- id
- codigoSequencial
- nome
- tipoPessoa
- documento
- telefone
- email
- status

1.2 ClienteDetalheDto
Campos sugeridos:
- id
- codigoSequencial
- nome
- tipoPessoa
- documento
- telefone
- email
- endereco
- status
- totalLocacoes
- locacoesEmAndamento
- equipamentosEmPosse
- valorAberto

1.3 ClienteHistoricoLocacaoDto
Campos sugeridos:
- idLocacao
- equipamento
- dataInicio
- dataFim
- statusLocacao

1.4 ClienteFiltroDto
Campos sugeridos:
- nome
- documento
- codigoSequencial
- tipoPessoa
- status
- dataInicio
- dataFim

1.5 ClienteCadastroRequest
Campos sugeridos:
- nome
- tipoPessoa
- documento
- telefone
- email
- endereco
- status (opcional, padrão ATIVO)

1.6 ClienteAtualizacaoRequest
Campos sugeridos:
- nome
- tipoPessoa
- documento
- telefone
- email
- endereco
- status

1.7 ClienteCadastroResponse / ClienteAtualizacaoResponse
Campos sugeridos:
- id
- nome
- mensagem

1.8 ClienteInativacaoResponse
Campos sugeridos:
- id
- status
- mensagem

2. Métodos faltantes no ClienteRepository

2.1 Busca por locadora + filtros combinados
- findByLocadoraIdAndStatus(...)
- findByLocadoraIdAndTipoPessoa(...)
- findByLocadoraIdAndStatusAndTipoPessoa(...)
- findByLocadoraIdAndDocumentoContaining(...)
- findByLocadoraIdAndCodigoSequencial(...)

2.2 Busca textual livre
- findByLocadoraIdAndNomeContainingIgnoreCase(...)
- findByLocadoraIdAndStatusNotAndNomeContainingIgnoreCase(...)
- combinações com nome, documento, código sequencial, tipoPessoa e status

2.3 Consultas para painel de cliente
- countByLocadoraIdAndStatus(...)
- countByLocadoraIdAndStatusAndClienteId(...)
- consultas customizadas para valor em aberto, locações ativas e equipamentos em posse

2.4 Histórico recente
- findTop10ByClienteIdAndLocadoraIdOrderByDataInicioDesc(...)
- ou equivalente em JPQL

2.5 Resumo analítico do cliente
- método customizado para retornar:
  - totalLocacoes
  - locacoesEmAndamento
  - equipamentosEmPosse
  - valorAberto

3. Validações faltantes no ClienteService

3.1 Validação de CPF/CNPJ
- CPF com 11 dígitos válidos
- CNPJ com 14 dígitos válidos
- consistência entre tipoPessoa e documento

3.2 Validação de tipo de pessoa
- PF => CPF
- PJ => CNPJ

3.3 Padronização de dados
- remover máscara de documento
- normalizar telefone
- normalizar email
- normalizar nome/razão social
- validar endereço básico

3.4 Regras de status
- ATIVO, INATIVO, CANCELADO
- impedir duplicidade de documento na mesma locadora
- permitir apenas um cliente ativo com mesmo documento na mesma base

3.5 Regras de inativação
- bloquear inativação se houver locação em andamento ou atrasada
- impedir exclusão física quando houver dependência de locação

3.6 Resumo do cliente
- total de locações realizadas
- locações em andamento
- equipamentos em posse
- valor total em aberto

3.7 Filtros por tenant (locadora)
- garantir que todas as consultas da tela usem locadoraId do JWT

4. Endpoints faltantes na ClienteController

4.1 Listagem com filtros
- GET /api/clientes
Parâmetros sugeridos:
- nome
- documento
- codigoSequencial
- tipoPessoa
- status

4.2 Detalhe do cliente
- GET /api/clientes/{id}

4.3 Histórico de locações
- GET /api/clientes/{id}/historico

4.4 Resumo do cliente
- GET /api/clientes/{id}/resumo

4.5 Cadastro
- POST /api/clientes

4.6 Atualização
- PUT /api/clientes/{id}

4.7 Inativação/Ativação
- PATCH /api/clientes/{id}/status
- ou PUT /api/clientes/{id}/inativar

4.8 Exclusão
- DELETE /api/clientes/{id}

5. Regras multi-tenant que faltam no back-end

- locadoraId deve ser extraído do JWT
- todas as consultas devem filtrar por locadoraId
- nunca retornar dados de outra base
- código sequencial deve ser único por locadora
- documento deve ser único por locadora
- cliente faltante ou inativo não pode aparecer em listagens da base ativa

6. Observações práticas

O backend atual cobre parcialmente:
- cadastro do cliente
- listagem básica
- busca por id
- atualização
- inativação lógica

Mas ainda não cobre de forma completa:
- filtros avançados da tela
- detalhe analítico para painel lateral
- histórico recente de locações
- resumo de consumo e dívidas
- suporte completo a PF/PJ com validação de documento
- endpoints para dashboard de cliente

7. Resultado esperado para a tela

Para que a interface de clientes funcione corretamente, o backend precisa entregar:
- lista filtrável de clientes por base multi-tenant
- painel de resumo por cliente
- histórico recente de locações
- dados de status e valores em aberto
- respostas em DTOs específicos para renderização do front-end

8. Status atual

- Front-end em desenvolvimento: OK
- Backend necessário para funcionalidade completa: PENDENTE
- Alterações no Java/Spring Boot: ainda não realizadas, conforme regra do projeto
