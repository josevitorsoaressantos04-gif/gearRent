package com.example.gearrent;

import com.example.gearrent.infra.uptime.HealthRateLimitFilter;
import com.example.gearrent.infra.uptime.UptimeRobotScheduler;
import com.example.gearrent.presentation.UptimeHealthController;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class UptimeRateLimitUnitTest {

    private static final ZoneId ZONA_SP = ZoneId.of("America/Sao_Paulo");

    private static final class RelogioControlavel extends Clock {
        private final AtomicReference<Instant> instante;

        private RelogioControlavel(Instant inicial) {
            this.instante = new AtomicReference<>(inicial);
        }

        void avancar(Duration duracao) {
            instante.updateAndGet(atual -> atual.plus(duracao));
        }

        @Override
        public ZoneId getZone() {
            return ZONA_SP;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instante.get();
        }
    }

    @Test
    @DisplayName("UptimeRobotScheduler: bloqueia após exceder limite por minuto e libera após recarga gradual")
    void schedulerAplicaRateLimitERecarregaComClock() {
        RelogioControlavel clock = new RelogioControlavel(Instant.parse("2026-04-01T12:00:00Z"));
        UptimeHealthController controller = new UptimeHealthController(null, clock);
        UptimeRobotScheduler scheduler = new UptimeRobotScheduler(controller, clock, 3);
        ReflectionTestUtils.setField(scheduler, "habilitado", true);
        ReflectionTestUtils.setField(scheduler, "pingUrl", "");

        assertEquals("UP", scheduler.executarTesteConexao());
        assertEquals("UP", scheduler.executarTesteConexao());
        assertEquals("UP", scheduler.executarTesteConexao());
        assertEquals("RATE_LIMITED", scheduler.executarTesteConexao());

        // refillGreedy(3, 1 min) repõe 1 token a cada 20 segundos
        clock.avancar(Duration.ofSeconds(20));
        assertEquals("UP", scheduler.executarTesteConexao());
        assertEquals("RATE_LIMITED", scheduler.executarTesteConexao());
    }

    @Test
    @DisplayName("HealthRateLimitFilter: limita por IP com HTTP 429 + Retry-After sem bloquear outros IPs")
    void filtroLimitaPorIpSemAfetarOutrosChamadores() throws Exception {
        RelogioControlavel clock = new RelogioControlavel(Instant.parse("2026-04-01T12:00:00Z"));
        HealthRateLimitFilter filtro = new HealthRateLimitFilter(clock, 2, 100);

        AtomicInteger execucoesChain = new AtomicInteger();
        FilterChain chain = (req, res) -> execucoesChain.incrementAndGet();

        // 1ª e 2ª requisições do IP atacante passam
        MockHttpServletResponse res1 = executar(filtro, "GET", "/api/health", "203.0.113.10", chain);
        MockHttpServletResponse res2 = executar(filtro, "GET", "/api/ping", "203.0.113.10", chain);
        assertEquals(200, res1.getStatus());
        assertEquals("1", res1.getHeader("X-Rate-Limit-Remaining"));
        assertEquals(200, res2.getStatus());
        assertEquals("0", res2.getHeader("X-Rate-Limit-Remaining"));

        // 3ª requisição do mesmo IP recebe 429 e NÃO chega no controller/banco
        MockHttpServletResponse res3 = executar(filtro, "GET", "/api/health", "203.0.113.10", chain);
        assertEquals(429, res3.getStatus());
        assertEquals("0", res3.getHeader("X-Rate-Limit-Remaining"));
        assertNotNull(res3.getHeader("Retry-After"));
        assertTrue(Long.parseLong(res3.getHeader("Retry-After")) >= 1);
        assertTrue(res3.getContentAsString().contains("\"status\":429"));
        assertEquals(2, execucoesChain.get());

        // Outro IP (ex.: Docker HEALTHCHECK / Render) continua funcionando normalmente
        MockHttpServletResponse resOutroIp = executar(filtro, "GET", "/api/health", "127.0.0.1", chain);
        assertEquals(200, resOutroIp.getStatus());
        assertEquals(3, execucoesChain.get());

        // Após 30 segundos (refillGreedy(2, 1 min) -> 1 token a cada 30s), IP original volta a passar
        clock.avancar(Duration.ofSeconds(30));
        MockHttpServletResponse resPosRecarga = executar(filtro, "GET", "/api/health", "203.0.113.10", chain);
        assertEquals(200, resPosRecarga.getStatus());
        assertEquals(4, execucoesChain.get());
    }

    @Test
    @DisplayName("HealthRateLimitFilter: ignora rotas que não são de healthcheck e requisições OPTIONS")
    void filtroIgnoraOutrasRotasEOptions() throws Exception {
        RelogioControlavel clock = new RelogioControlavel(Instant.parse("2026-04-01T12:00:00Z"));
        HealthRateLimitFilter filtro = new HealthRateLimitFilter(clock, 1, 100);

        AtomicInteger execucoesChain = new AtomicInteger();
        FilterChain chain = (req, res) -> execucoesChain.incrementAndGet();

        MockHttpServletResponse resOutraRota = executar(filtro, "GET", "/api/clientes", "203.0.113.50", chain);
        MockHttpServletResponse resOptions = executar(filtro, "OPTIONS", "/api/health", "203.0.113.50", chain);
        assertEquals(200, resOutraRota.getStatus());
        assertNull(resOutraRota.getHeader("X-Rate-Limit-Remaining"));
        assertEquals(200, resOptions.getStatus());
        assertNull(resOptions.getHeader("X-Rate-Limit-Remaining"));
        assertEquals(2, execucoesChain.get());

        // O token do IP não foi consumido pelas requisições ignoradas
        MockHttpServletResponse resHealth = executar(filtro, "GET", "/api/health", "203.0.113.50", chain);
        assertEquals(200, resHealth.getStatus());
        assertEquals("0", resHealth.getHeader("X-Rate-Limit-Remaining"));
    }

    @Test
    @DisplayName("HealthRateLimitFilter: mantém cache de IPs limitado em memória e usa bucket de overflow sob flood")
    void filtroLimitaMemoriaDeIpsEEvitaOom() throws Exception {
        RelogioControlavel clock = new RelogioControlavel(Instant.parse("2026-04-01T12:00:00Z"));
        HealthRateLimitFilter filtro = new HealthRateLimitFilter(clock, 1, 2);

        MockFilterChain chain = new MockFilterChain();
        // Ocupa os 2 slots do cache de IPs
        assertEquals(200, executar(filtro, "GET", "/api/health", "198.51.100.1", (req, res) -> {}).getStatus());
        assertEquals(200, executar(filtro, "GET", "/api/health", "198.51.100.2", (req, res) -> {}).getStatus());

        // 3º e 4º IPs distintos caem no bucketOverflow (1 token compartilhado) sem crescer o mapa
        assertEquals(200, executar(filtro, "GET", "/api/health", "198.51.100.3", chain).getStatus());
        assertEquals(429, executar(filtro, "GET", "/api/health", "198.51.100.4", (req, res) -> {}).getStatus());

        // Após > 2 minutos de inatividade, entradas antigas são expiradas e novo IP entra normalmente
        clock.avancar(Duration.ofMinutes(3));
        assertEquals(200, executar(filtro, "GET", "/api/health", "198.51.100.5", (req, res) -> {}).getStatus());
    }

    private MockHttpServletResponse executar(HealthRateLimitFilter filtro,
                                             String metodo,
                                             String uri,
                                             String remoteAddr,
                                             FilterChain chain) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(metodo, uri);
        request.setRemoteAddr(remoteAddr);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filtro.doFilter(request, response, chain);
        return response;
    }
}

