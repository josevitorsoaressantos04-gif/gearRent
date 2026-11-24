package com.example.gearrent;

import com.example.gearrent.application.DTO.*;
import com.example.gearrent.domain.enums.CategoriaEquipamento;
import com.example.gearrent.domain.enums.PapelUsuario;
import com.example.gearrent.domain.enums.StatusEquipamento;
import com.example.gearrent.domain.enums.TipoPagamento;
import com.example.gearrent.domain.enums.TipoPessoa;
import com.example.gearrent.presentation.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import com.example.gearrent.infra.security.TokenService;
import com.example.gearrent.infra.security.UsuarioAutenticado;
import com.example.gearrent.infra.uptime.UptimeRobotScheduler;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste End-to-End (E2E) Full-Stack da API GearRent.
 * Exercita todo o fluxo de ponta a ponta (UptimeRobot, Cadastro Multi-Tenant, Login JWT,
 * Locadora, Operadores, Clientes PF/PJ, Equipamentos, Locações, Visão Geral, Financeiro e Auditoria)
 * com rollback transacional ao final para manter o banco limpo.
 */
@SpringBootTest
@Transactional
class GearRentE2ETest {

    @Autowired
    private UptimeHealthController uptimeHealthController;

    @Autowired
    private UptimeRobotScheduler uptimeRobotScheduler;

    @Autowired
    private UsuarioController usuarioController;

    @Autowired
    private AuthController authController;

    @Autowired
    private LocadoraController locadoraController;

    @Autowired
    private ClienteController clienteController;

    @Autowired
    private EquipamentoController equipamentoController;

    @Autowired
    private LocacaoController locacaoController;

    @Autowired
    private VisaoGeralController visaoGeralController;

    @Autowired
    private ResumoFinanceiroController resumoFinanceiroController;

    @Autowired
    private AuditoriaController auditoriaController;

    @Autowired
    private TokenService tokenService;

    @Test
    @DisplayName("E2E 1: UptimeRobot Keep-Alive (30s) retorna status UP e conexão com banco UP")
    void deveRetornarUpNoHealthCheckEUptimeRobot() {
        ResponseEntity<Map<String, Object>> health = uptimeHealthController.verificarConexao();
        assertEquals(200, health.getStatusCode().value());
        assertNotNull(health.getBody());
        assertEquals("UP", health.getBody().get("status"));
        assertEquals("UP", health.getBody().get("database"));
        assertEquals(30, health.getBody().get("intervaloSegundos"));

        String statusAgendador = uptimeRobotScheduler.executarTesteConexao();
        assertEquals("UP", statusAgendador);
    }

    @Test
    @DisplayName("E2E 2: Fluxo Completo Multi-Tenant (Cadastro -> Login JWT -> Clientes -> Equipamentos -> Locações -> Financeiro -> Auditoria)")
    void fluxoCompletoEndToEndMultiTenant() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String emailAdmin = "e2e." + sufixo + "@gearrent.com";
        String senhaPadrao = "SenhaForte123";
        String cpfAdmin = gerarCpfValidoAleatorio();

        // 1. Cadastro público de Usuário + criação automática da Locadora (Tenant)
        ResponseEntity<UsuarioCadastroResponse> cadastroRes = usuarioController.cadastrar(
                new UsuarioCadastroRequest(
                        "Admin",
                        "E2E",
                        cpfAdmin,
                        LocalDate.of(1990, 5, 15),
                        emailAdmin,
                        senhaPadrao
                )
        );
        assertEquals(201, cadastroRes.getStatusCode().value());
        assertNotNull(cadastroRes.getBody());

        // 2. Login e geração do Token JWT contendo locadoraId e role=ADMIN
        ResponseEntity<LoginResponse> loginRes = authController.login(
                new LoginRequest(emailAdmin, senhaPadrao)
        );
        assertEquals(200, loginRes.getStatusCode().value());
        assertNotNull(loginRes.getBody());
        String jwt = loginRes.getBody().token();
        assertNotNull(jwt);

        UsuarioAutenticado principal = tokenService.validar(jwt).orElseThrow();
        assertEquals(PapelUsuario.ADMIN, principal.role());
        assertNotNull(principal.locadoraId());

        // Configura o SecurityContext como o JwtFilter faz nas requisições autenticadas
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority(principal.role().autoridade()))
                )
        );

        try {
            // 3. Perfil do usuário e atualização cadastral da Locadora
            PerfilConsultaResponse perfil = usuarioController.meuPerfil(principal);
            assertEquals("Admin", perfil.nome());

            EnderecoDTO enderecoPadrao = new EnderecoDTO(
                    "Av. Centenário", "1000", "Sala 1", "Centro", "Criciúma", "SC", "88801-000"
            );
            LocadoraConsultaResponse locadoraAtualizada = locadoraController.atualizar(
                    principal,
                    new LocadoraAtualizacaoRequest(
                            null, "GearRent E2E Base", "GearRent E2E Base", "(48) 99999-0000", "dark", enderecoPadrao
                    )
            );
            assertEquals("GearRent E2E Base", locadoraAtualizada.nomeFantasia());

            // 4. Gestão de Operadores (Criação, Desativação e Reativação pelo ADMIN)
            ResponseEntity<OperadorCadastroResponse> opRes = usuarioController.criarOperador(
                    principal,
                    new OperadorCadastroRequest(
                            "Operador", "Teste", "op." + sufixo + "@gearrent.com", "Senha123", PapelUsuario.OPERADOR
                    )
            );
            assertEquals(201, opRes.getStatusCode().value());
            Long operadorId = opRes.getBody().id();

            ResponseEntity<UsuarioStatusResponse> desativado = usuarioController.desativar(principal, operadorId);
            assertEquals(200, desativado.getStatusCode().value());

            UsuarioReativacaoResponse reativado = usuarioController.reativar(principal, operadorId);
            assertEquals(opRes.getBody().email(), reativado.email());

            // 5. Cadastro e Edição de Cliente no Tenant
            ResponseEntity<ClienteCadastroResponse> clienteRes = clienteController.cadastrar(
                    principal,
                    new ClienteCadastroRequest(
                            "Construtora Horizonte",
                            "12.345.678/0001-99",
                            "contato@horizonte.com",
                            "(48) 98888-7777",
                            null,
                            enderecoPadrao
                    )
            );
            assertEquals(201, clienteRes.getStatusCode().value());
            Long clienteId = clienteRes.getBody().id();

            ResponseEntity<ClienteAtualizacaoResponse> clienteEditado = clienteController.atualizar(
                    principal,
                    clienteId,
                    new ClienteAtualizacaoRequest(
                            "Construtora Horizonte LTDA",
                            TipoPessoa.JURIDICA,
                            "12345678000199",
                            "financeiro@horizonte.com",
                            "(48) 98888-7777",
                            null,
                            enderecoPadrao
                    )
            );
            assertEquals(200, clienteEditado.getStatusCode().value());

            // 6. Cadastro e Listagem de Equipamentos no Tenant
            ResponseEntity<EquipamentoCadastroResponse> eqRes = equipamentoController.cadastrar(
                    principal,
                    new EquipamentoCadastroRequest(
                            "Martelete Rompedor 15kg",
                            "Bosch GSH 16",
                            "PAT-E2E-" + sufixo,
                            CategoriaEquipamento.FERRAMENTA_ELETRICA,
                            new BigDecimal("120.00")
                    )
            );
            assertEquals(201, eqRes.getStatusCode().value());
            Long equipamentoId = eqRes.getBody().id();

            ResponseEntity<List<EquipamentoDisponivelResponse>> disponiveis =
                    equipamentoController.listarDisponiveis(principal, "");
            assertEquals(1, disponiveis.getBody().size());

            // 7. Criação de Locação, Resumo do Cliente, Visão Geral e Devolução
            LocalDate hoje = LocalDate.now();
            ResponseEntity<LocacaoResponse> locacaoRes = locacaoController.cadastrar(
                    principal,
                    new LocacaoCadastroRequest(
                            clienteId,
                            List.of(equipamentoId),
                            hoje,
                            hoje.plusDays(3),
                            TipoPagamento.PIX,
                            new BigDecimal("10.00"),
                            "Carlos Mestre de Obras",
                            "11122233344",
                            enderecoPadrao
                    )
            );
            assertEquals(201, locacaoRes.getStatusCode().value());
            Long locacaoId = locacaoRes.getBody().id();
            assertEquals(new BigDecimal("350.00"), locacaoRes.getBody().valorTotal()); // (3 * 120) - 10

            // Verifica Resumo Analítico e Histórico do Cliente (Feature 3)
            ResponseEntity<ClienteResumoDTO> resumoCliente = clienteController.resumo(principal, clienteId);
            assertEquals(1L, resumoCliente.getBody().totalLocacoes());
            assertEquals(1L, resumoCliente.getBody().locacoesEmAndamento());
            assertEquals(1L, resumoCliente.getBody().equipamentosEmPosse());

            ResponseEntity<List<ClienteHistoricoLocacaoDTO>> historicoCliente =
                    clienteController.historico(principal, clienteId);
            assertEquals(1, historicoCliente.getBody().size());

            // Verifica Visão Geral e exposição de valorDiariaSoma (Feature 3)
            ResponseEntity<VisaoGeralResumoDTO> resumoDashboard = visaoGeralController.resumo(principal);
            assertEquals(1L, resumoDashboard.getBody().totalLocacoes());
            assertEquals(1L, resumoDashboard.getBody().equipamentosAlugados());

            ResponseEntity<Page<LocacaoListagemSinteticaDTO>> listagemSintetica =
                    visaoGeralController.locacoes(
                            principal,
                            new FiltroLocacaoDTO(null, null, null, null, null, null, null, null),
                            PageRequest.of(0, 10)
                    );
            assertEquals(1, listagemSintetica.getBody().getTotalElements());
            assertEquals(new BigDecimal("120.00"),
                    listagemSintetica.getBody().getContent().get(0).valorDiariaSoma());

            // 8. Registro de Devolução da Locação (Feature 2)
            LocacaoResponse devolvida = locacaoController.devolver(
                    principal,
                    locacaoId,
                    new LocacaoDevolucaoRequest(hoje)
            );
            assertEquals("CONCLUIDA", devolvida.status().name());

            // Equipamento volta a ficar DISPONIVEL após a devolução
            EquipamentoConsultaResponse eqAposDevolucao =
                    equipamentoController.buscarPorId(principal, equipamentoId).getBody();
            assertEquals(StatusEquipamento.DISPONIVEL, eqAposDevolucao.status());

            // 9. Resumo Financeiro e Logs de Auditoria
            ResumoFinanceiroResponse financeiro = resumoFinanceiroController.resumo(principal, null, null);
            assertEquals(1L, financeiro.quantidadeConcluidas());

            ResponseEntity<List<LogAuditoriaResponse>> logs = auditoriaController.listar(principal, null, false);
            assertFalse(logs.getBody().isEmpty());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static String gerarCpfValidoAleatorio() {
        int[] n = new int[11];
        java.util.concurrent.ThreadLocalRandom rnd = java.util.concurrent.ThreadLocalRandom.current();
        for (int i = 0; i < 9; i++) {
            n[i] = rnd.nextInt(0, 10);
        }
        // Evita todos os dígitos iguais
        if (n[0] == n[1] && n[1] == n[2]) {
            n[0] = (n[0] + 1) % 10;
        }
        int s1 = 0;
        for (int i = 0; i < 9; i++) s1 += n[i] * (10 - i);
        int d1 = 11 - (s1 % 11);
        n[9] = d1 >= 10 ? 0 : d1;

        int s2 = 0;
        for (int i = 0; i < 10; i++) s2 += n[i] * (11 - i);
        int d2 = 11 - (s2 % 11);
        n[10] = d2 >= 10 ? 0 : d2;

        StringBuilder sb = new StringBuilder(11);
        for (int digito : n) sb.append(digito);
        return sb.toString();
    }
}
