package com.example.gearrent.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

// DTO de Aditivo Contratual (PUT) - Restrito por Auditoria
public record ContratoPutRequest(
        @NotNull(message = "A nova data de devolução é obrigatória")
        @Future(message = "A data de devolução deve ser no futuro")
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
        LocalDateTime dataDevolucaoPrevista
) {}
