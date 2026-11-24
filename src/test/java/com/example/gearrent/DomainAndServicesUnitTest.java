package com.example.gearrent;

import com.example.gearrent.domain.entities.Cliente;
import com.example.gearrent.domain.entities.Endereco;
import com.example.gearrent.domain.entities.Equipamento;
import com.example.gearrent.domain.entities.Locacao;
import com.example.gearrent.domain.entities.Locadora;
import com.example.gearrent.domain.entities.Usuario;
import com.example.gearrent.domain.enums.CategoriaEquipamento;
import com.example.gearrent.domain.enums.PapelUsuario;
import com.example.gearrent.domain.enums.StatusCadastro;
import com.example.gearrent.domain.enums.StatusEquipamento;
import com.example.gearrent.domain.enums.StatusLocacao;
import com.example.gearrent.domain.enums.TipoPagamento;
import com.example.gearrent.domain.enums.TipoPessoa;
import com.example.gearrent.domain.exception.EquipamentoIndisponivelException;
import com.example.gearrent.domain.exception.RegraDeNegocioException;
import com.example.gearrent.domain.exception.TransicaoStatusInvalidaException;
import com.example.gearrent.domain.service.CalculadoraLocacao;
import com.example.gearrent.infra.security.TokenService;
import com.example.gearrent.infra.security.UsuarioAutenticado;
import com.example.gearrent.infra.uptime.UptimeRobotScheduler;
import com.example.gearrent.presentation.UptimeHealthController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DomainAndServicesUnitTest {

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-11-24T12:00:00Z"),
            ZoneId.of("America/Sao_Paulo")
    );

    @Test
    @DisplayName("CalculadoraLocacao: calcula diárias normais, desconto e multa de 10% em atraso")
    void calculaDiariasDescontoEMultaCorretamente() {
        LocalDate retirada = LocalDate.of(2026, 11, 1);
        LocalDate prevista = LocalDate.of(2026, 11, 5);   // 4 diárias normais
        LocalDate devolucao = LocalDate.of(2026, 11, 7);  // 2 diárias de atraso (total 6 dias)

        CalculadoraLocacao.Resultado resultado = CalculadoraLocacao.calcular(
                retirada,
                prevista,
                devolucao,
                new BigDecimal("100.00"),
                new BigDecimal("50.00")
        );

        assertEquals(6L, resultado.diasCobrados());
        assertEquals(2L, resultado.diasAtraso());
        assertEquals(new BigDecimal("600.00"), resultado.valorDiarias());
        assertEquals(new BigDecimal("50.00"), resultado.valorDesconto());
        assertEquals(new BigDecimal("20.00"), resultado.valorMulta()); // 10% sobre 200.00
        assertEquals(new BigDecimal("570.00"), resultado.valorTotal()); // 600 - 50 + 20
    }

    @Test
    @DisplayName("Locacao e Equipamento: ciclo completo de abertura, congelamento de preço, edição, atraso e devolução")
    void cicloCompletoDeLocacaoNoDominio() {
        Locadora locadora = new Locadora();
        locadora.setId(10L);
        locadora.setNomeFantasia("Locadora Teste");

        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Construtora Alfa");
        cliente.setStatus(StatusCadastro.ATIVO);
        cliente.setLocadora(locadora);

        Usuario operador = new Usuario();
        operador.setId(2L);
        operador.setLocadora(locadora);

        Equipamento betoneira = new Equipamento();
        betoneira.setId(100L);
        betoneira.setNome("Betoneira 400L");
        betoneira.setCategoria(CategoriaEquipamento.MAQUINARIO_PESADO);
        betoneira.setStatus(StatusEquipamento.DISPONIVEL);
        betoneira.setValorDiaria(new BigDecimal("150.00"));
        betoneira.setLocadora(locadora);

        LocalDateTime retirada = LocalDateTime.of(2026, 11, 20, 0, 0);
        LocalDateTime prevista = LocalDateTime.of(2026, 11, 22, 0, 0);
        LocalDateTime agora = LocalDateTime.of(2026, 11, 20, 10, 0);

        Locacao locacao = Locacao.abrir(
                locadora,
                1L,
                cliente,
                operador,
                Set.of(betoneira),
                retirada,
                prevista,
                TipoPagamento.PIX,
                new BigDecimal("20.00"),
                "João Responsável",
                "12345678901",
                new Endereco("Rua A", "10", null, "Centro", "Criciúma", "SC", "88800-000"),
                agora
        );

        assertEquals(StatusLocacao.EM_ANDAMENTO, locacao.getStatus());
        assertEquals(StatusEquipamento.ALUGADO, betoneira.getStatus());
        assertEquals(280.0, locacao.getValorFinalCobrado(), 0.001); // (2 * 150) - 20

        // Mesmo que o preço do equipamento suba no catálogo, a locação mantém o preço congelado
        betoneira.setValorDiaria(new BigDecimal("999.00"));
        assertEquals(new BigDecimal("150.00"), locacao.valorDiariaDe(betoneira));

        // Não permite alugar novamente um equipamento já alugado
        assertThrows(EquipamentoIndisponivelException.class, betoneira::marcarComoAlugado);

        // Atualiza data prevista enquanto está EM_ANDAMENTO
        locacao.atualizar(
                LocalDateTime.of(2026, 11, 23, 0, 0),
                new BigDecimal("30.00"),
                "Maria Responsável",
                null,
                null,
                agora
        );
        assertEquals(420.0, locacao.getValorFinalCobrado(), 0.001); // (3 * 150) - 30

        // Simula passagem do tempo além da data prevista -> vira ATRASADA
        LocalDateTime aposPrazo = LocalDateTime.of(2026, 11, 25, 9, 0);
        locacao.sincronizarAtraso(aposPrazo);
        assertEquals(StatusLocacao.ATRASADA, locacao.getStatus());

        // Devolve equipamento em 25/11 (2 dias de atraso) -> libera equipamento e conclui contrato
        locacao.devolver(LocalDateTime.of(2026, 11, 25, 0, 0), aposPrazo);
        assertEquals(StatusLocacao.CONCLUIDA, locacao.getStatus());
        assertEquals(StatusEquipamento.DISPONIVEL, betoneira.getStatus());
        assertEquals(30.0, locacao.getValorMultaPorAtraso(), 0.001); // 10% de (2 * 150)

        // Locação concluída não pode ser devolvida novamente
        assertThrows(TransicaoStatusInvalidaException.class,
                () -> locacao.devolver(LocalDateTime.of(2026, 11, 26, 0, 0), aposPrazo));
    }

    @Test
    @DisplayName("Locacao: rejeita desconto maior que o valor das diárias")
    void rejeitaDescontoMaiorQueDiarias() {
        Locadora locadora = new Locadora();
        locadora.setId(1L);

        Equipamento eq = new Equipamento();
        eq.setId(1L);
        eq.setNome("Furadeira");
        eq.setStatus(StatusEquipamento.DISPONIVEL);
        eq.setValorDiaria(new BigDecimal("50.00"));

        assertThrows(RegraDeNegocioException.class, () -> Locacao.abrir(
                locadora,
                1L,
                new Cliente(),
                new Usuario(),
                Set.of(eq),
                LocalDateTime.of(2026, 11, 20, 0, 0),
                LocalDateTime.of(2026, 11, 21, 0, 0),
                TipoPagamento.DINHEIRO,
                new BigDecimal("100.00"),
                null,
                null,
                null,
                LocalDateTime.of(2026, 11, 20, 8, 0)
        ));
    }

    @Test
    @DisplayName("TipoPessoa: deduz corretamente Pessoa Física (11 dígitos) e Jurídica (14 dígitos)")
    void deduzTipoPessoaPeloDocumento() {
        assertEquals(TipoPessoa.FISICA, TipoPessoa.deduzirDoDocumento("12345678901"));
        assertEquals(TipoPessoa.JURIDICA, TipoPessoa.deduzirDoDocumento("12345678000199"));
        assertNull(TipoPessoa.deduzirDoDocumento("123"));
    }

    @Test
    @DisplayName("TokenService (Multi-Tenant JWT): gera e valida token isolando locadoraId e role")
    void geraEValidaTokenJwtMultiTenant() {
        TokenService tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", "chave-secreta-de-teste-unitario-gearrent-2026");
        ReflectionTestUtils.setField(tokenService, "emissor", "gearrent");
        ReflectionTestUtils.setField(tokenService, "expiracaoMinutos", 120L);

        Locadora locadora = new Locadora();
        locadora.setId(77L);

        Usuario admin = new Usuario();
        admin.setId(15L);
        admin.setRole(PapelUsuario.ADMIN);
        admin.setLocadora(locadora);

        String jwt = tokenService.gerarToken(admin);
        assertNotNull(jwt);

        Optional<UsuarioAutenticado> validado = tokenService.validar(jwt);
        assertTrue(validado.isPresent());
        assertEquals(15L, validado.get().id());
        assertEquals(77L, validado.get().locadoraId());
        assertEquals(PapelUsuario.ADMIN, validado.get().role());

        // Token adulterado deve ser rejeitado
        assertTrue(tokenService.validar(jwt + "adulterado").isEmpty());
    }

    @Test
    @DisplayName("UptimeRobot (30s Keep-Alive): endpoint /api/health e scheduler retornam UP no log interno")
    void uptimeRobotRetornaUpNoTesteDeConexao() {
        UptimeHealthController controller = new UptimeHealthController(null, clock);
        ResponseEntity<Map<String, Object>> response = controller.verificarConexao();

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals(30, response.getBody().get("intervaloSegundos"));

        UptimeRobotScheduler scheduler = new UptimeRobotScheduler(controller, clock);
        ReflectionTestUtils.setField(scheduler, "habilitado", true);
        ReflectionTestUtils.setField(scheduler, "pingUrl", ""); // força verificação local imediata

        String status = scheduler.executarTesteConexao();
        assertEquals("UP", status);
    }
}
