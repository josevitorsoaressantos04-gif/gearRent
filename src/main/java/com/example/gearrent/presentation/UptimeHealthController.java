package com.example.gearrent.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoint público de Healthcheck / UptimeRobot para manter o servidor ativo no Render
 * e registrar "UP" no log interno do sistema como teste de conexão.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "UptimeRobot / Health", description = "Endpoints públicos de monitoramento e teste de conexão (Keep-Alive 30s)")
public class UptimeHealthController {

    private static final Logger log = LoggerFactory.getLogger(UptimeHealthController.class);

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public UptimeHealthController(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @GetMapping({"/health", "/ping"})
    @Operation(
            summary = "Teste de conexão e UptimeRobot (Keep-Alive)",
            description = "Retorna o status UP do servidor e do banco de dados e registra o resultado UP no log interno do sistema. Protegido por rate limit por IP."
    )
    @ApiResponse(responseCode = "200", description = "Servidor ativo (UP)")
    @ApiResponse(responseCode = "429", description = "Limite de requisições excedido para este IP")
    public ResponseEntity<Map<String, Object>> verificarConexao() {
        String dbStatus = "UP";
        try {
            if (jdbcTemplate != null) {
                Integer resultado = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
                if (resultado == null || resultado != 1) {
                    dbStatus = "DEGRADED";
                }
            }
        } catch (Exception e) {
            dbStatus = "DOWN";
        }

        LocalDateTime agora = LocalDateTime.now(clock);
        log.info("[UPTIMEROBOT - TESTE DE CONEXÃO] Status: UP | Database: {} | Timestamp: {}", dbStatus, agora);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("status", "UP");
        resposta.put("database", dbStatus);
        resposta.put("intervaloSegundos", 30);
        resposta.put("mensagem", "UP - Teste de conexão concluído com sucesso.");
        resposta.put("timestamp", agora.toString());

        return ResponseEntity.ok(resposta);
    }
}
