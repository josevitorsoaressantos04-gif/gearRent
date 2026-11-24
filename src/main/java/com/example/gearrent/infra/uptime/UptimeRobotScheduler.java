package com.example.gearrent.infra.uptime;

import com.example.gearrent.presentation.UptimeHealthController;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.TimeMeter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Agendador interno estilo UptimeRobot: executa a cada 30 segundos (antes do timeout de 50s do Render),
 * realiza um teste de conexão HTTP/interno e imprime "UP" no log interno do sistema.
 * Conta com rate limit configurável via Bucket4j em memória para evitar rajadas por misconfiguração.
 */
@Component
public class UptimeRobotScheduler {

    private static final Logger log = LoggerFactory.getLogger(UptimeRobotScheduler.class);
    private static final int LIMITE_PADRAO_POR_MINUTO = 3;

    private final UptimeHealthController healthController;
    private final RestClient restClient;
    private final Clock clock;
    private final Bucket bucket;
    private final int limitePorMinuto;

    @Value("${app.uptimerobot.habilitado:true}")
    private boolean habilitado;

    @Value("${app.uptimerobot.url:https://gearrent-eo3x.onrender.com/api/health}")
    private String pingUrl;

    public UptimeRobotScheduler(UptimeHealthController healthController, Clock clock) {
        this(healthController, clock, LIMITE_PADRAO_POR_MINUTO);
    }

    @Autowired
    public UptimeRobotScheduler(
            UptimeHealthController healthController,
            Clock clock,
            @Value("${app.uptimerobot.rate-limit.requisicoes-por-minuto:3}") int requisicoesPorMinuto) {
        this.healthController = healthController;
        this.clock = clock;
        this.limitePorMinuto = Math.max(1, requisicoesPorMinuto);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();

        Bandwidth limite = Bandwidth.builder()
                .capacity(this.limitePorMinuto)
                .refillGreedy(this.limitePorMinuto, Duration.ofMinutes(1))
                .build();
        this.bucket = Bucket.builder()
                .withCustomTimePrecision(new TimeMeter() {
                    @Override
                    public long currentTimeNanos() {
                        return clock.millis() * 1_000_000L;
                    }

                    @Override
                    public boolean isWallClockBased() {
                        return true;
                    }
                })
                .addLimit(limite)
                .build();
    }

    /**
     * Executa automaticamente a cada 30 segundos (30.000 ms).
     */
    @Scheduled(
            fixedRateString = "${app.uptimerobot.intervalo-ms:30000}",
            initialDelayString = "${app.uptimerobot.atraso-inicial-ms:5000}"
    )
    public String executarTesteConexao() {
        if (!habilitado) {
            return "DISABLED";
        }

        // Tenta consumir 1 token. Caso ultrapasse o limite configurado por minuto, ignora a execução.
        if (!bucket.tryConsume(1)) {
            log.warn("[UPTIMEROBOT] Taxa limite atingida (máximo de {} requisições por minuto). Execução ignorada.",
                    limitePorMinuto);
            return "RATE_LIMITED";
        }

        LocalDateTime agora = LocalDateTime.now(clock);

        try {
            if (pingUrl != null && !pingUrl.isBlank()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> respostaRemota = restClient.get()
                        .uri(pingUrl.trim())
                        .retrieve()
                        .body(Map.class);

                String status = respostaRemota != null && respostaRemota.get("status") != null
                        ? String.valueOf(respostaRemota.get("status"))
                        : "UP";

                log.info("[UPTIMEROBOT 30s - KEEP-ALIVE] Teste de conexão externo ({}) -> Status: {} em {}",
                        pingUrl, status, agora);
                return status;
            }
        } catch (Exception ex) {
            // Caso o endpoint externo ainda esteja subindo ou rodando localmente sem internet,
            // executa o teste de conexão interno diretamente no UptimeHealthController.
            log.debug("Fallback do ping externo para verificação interna: {}", ex.getMessage());
        }

        Map<String, Object> local = healthController.verificarConexao().getBody();
        String statusLocal = local != null && local.get("status") != null
                ? String.valueOf(local.get("status"))
                : "UP";
        log.info("[UPTIMEROBOT 30s - KEEP-ALIVE] Teste de conexão interno -> Status: {} em {}", statusLocal, agora);
        return statusLocal;
    }
}
