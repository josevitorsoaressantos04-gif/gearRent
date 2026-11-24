package com.example.gearrent.infra.uptime;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TimeMeter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtro de Rate Limit por IP (Bucket4j em memória) para os endpoints públicos
 * de healthcheck (/api/health e /api/ping).
 *
 * Impede abuso externo (DoS / flood de SELECT 1 no banco e logs) sem permitir
 * que um único cliente esgote o limite de outros chamadores legítimos
 * (como o HEALTHCHECK do Docker ou o monitoramento do Render).
 */
public class HealthRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HealthRateLimitFilter.class);
    private static final long EXPIRACAO_INATIVIDADE_MS = Duration.ofMinutes(2).toMillis();

    private final int requisicoesPorMinuto;
    private final int maxIpsEmMemoria;
    private final Clock clock;
    private final TimeMeter timeMeter;
    private final ConcurrentHashMap<String, BucketEntrada> bucketsPorIp = new ConcurrentHashMap<>();
    private final Bucket bucketOverflow;

    public HealthRateLimitFilter(Clock clock, int requisicoesPorMinuto, int maxIpsEmMemoria) {
        this.clock = clock;
        this.requisicoesPorMinuto = Math.max(1, requisicoesPorMinuto);
        this.maxIpsEmMemoria = Math.max(1, maxIpsEmMemoria);
        this.timeMeter = criarTimeMeter(clock);
        this.bucketOverflow = criarBucket();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        if (uri == null) {
            return true;
        }
        return !(uri.equals("/api/health")
                || uri.startsWith("/api/health/")
                || uri.equals("/api/ping"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ipCliente = resolverIpCliente(request);
        Bucket bucket = obterBucketParaIp(ipCliente);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
            return;
        }

        long segundosEspera = Math.max(1L, (probe.getNanosToWaitForRefill() + 999_999_999L) / 1_000_000_000L);
        log.warn("[HEALTH RATE LIMIT] IP {} excedeu o limite ({} req/min) em {} {}. Retry-After={}s",
                ipCliente, requisicoesPorMinuto, request.getMethod(), request.getRequestURI(), segundosEspera);

        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(segundosEspera));
        response.setHeader("X-Rate-Limit-Remaining", "0");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"status\":429,\"mensagens\":[\"Muitas requisições no endpoint de monitoramento. Tente novamente em "
                        + segundosEspera + " segundo(s).\"]}"
        );
    }

    private Bucket obterBucketParaIp(String ipCliente) {
        long agoraMs = clock.millis();
        BucketEntrada existente = bucketsPorIp.get(ipCliente);
        if (existente != null) {
            existente.ultimoAcessoMs = agoraMs;
            return existente.bucket;
        }

        if (bucketsPorIp.size() >= maxIpsEmMemoria) {
            limparEntradasExpiradas(agoraMs);
        }

        if (bucketsPorIp.size() >= maxIpsEmMemoria) {
            return bucketOverflow;
        }

        BucketEntrada entrada = bucketsPorIp.computeIfAbsent(
                ipCliente,
                chave -> new BucketEntrada(criarBucket(), agoraMs)
        );
        entrada.ultimoAcessoMs = agoraMs;
        return entrada.bucket;
    }

    private void limparEntradasExpiradas(long agoraMs) {
        bucketsPorIp.entrySet().removeIf(
                entry -> (agoraMs - entry.getValue().ultimoAcessoMs) > EXPIRACAO_INATIVIDADE_MS
        );
    }

    private Bucket criarBucket() {
        Bandwidth limite = Bandwidth.builder()
                .capacity(requisicoesPorMinuto)
                .refillGreedy(requisicoesPorMinuto, Duration.ofMinutes(1))
                .build();
        return Bucket.builder()
                .withCustomTimePrecision(timeMeter)
                .addLimit(limite)
                .build();
    }

    private String resolverIpCliente(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        return (ip == null || ip.isBlank()) ? "desconhecido" : ip.trim();
    }

    private static TimeMeter criarTimeMeter(Clock clock) {
        return new TimeMeter() {
            @Override
            public long currentTimeNanos() {
                return clock.millis() * 1_000_000L;
            }

            @Override
            public boolean isWallClockBased() {
                return true;
            }
        };
    }

    private static final class BucketEntrada {
        private final Bucket bucket;
        private volatile long ultimoAcessoMs;

        private BucketEntrada(Bucket bucket, long ultimoAcessoMs) {
            this.bucket = bucket;
            this.ultimoAcessoMs = ultimoAcessoMs;
        }
    }
}

