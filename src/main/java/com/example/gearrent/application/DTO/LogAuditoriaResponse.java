package com.example.gearrent.application.DTO;

import com.example.gearrent.domain.entities.LogAuditoria;

import java.time.LocalDateTime;

public record LogAuditoriaResponse(
        Long id,
        LocalDateTime dataHora,
        Long usuarioId,
        String usuarioNome,
        String modulo,
        String acao,
        boolean retroativo,
        String detalhes
) {
    public static LogAuditoriaResponse de(LogAuditoria log) {
        return new LogAuditoriaResponse(
                log.getId(),
                log.getDataHora(),
                log.getUsuarioId(),
                log.getUsuarioNome(),
                log.getModulo(),
                log.getAcao(),
                log.isRetroativo(),
                log.getDetalhes()
        );
    }
}
