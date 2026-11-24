package com.example.gearrent.infra.uptime;

import com.example.gearrent.presentation.UptimeHealthController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Agendador interno estilo UptimeRobot: executa a cada 30 segundos (antes do timeout de 50s do Render),
 * realiza um teste de conexão HTTP/interno e imprime "UP" no log interno do sistema.
 */
@Component
public class UptimeRobotScheduler {

    private static final Logger log = LoggerFactory.getLogger(UptimeRobotScheduler.class);

    private final UptimeHealthController healthController;
    private final RestClient restClient;
    private final Clock clock;

    @Value("${app.uptimerobot.habilitado:true}")
    private boolean habilitado;

    @Value("${app.uptimerobot.url:https://gearrent-eo3x.onrender.com/api/health}")
    private String pingUrl;

    public UptimeRobotScheduler(UptimeHealthController healthController, Clock clock) {
        this.healthController = healthController;
        this.clock = clock;
        this.restClient = RestClient.builder().build();
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
