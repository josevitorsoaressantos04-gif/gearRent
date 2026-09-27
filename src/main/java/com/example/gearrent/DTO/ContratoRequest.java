package com.example.gearrent.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// DTO de Criação Inicial (POST)
public record ContratoRequest(
        @NotNull(message = "O ID do cliente é obrigatório")
        Long clienteId,

        @NotEmpty(message = "É necessário informar ao menos um equipamento")
        List<Long> equipamentoIds,

        @NotNull(message = "A data de retirada é obrigatória")
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
        LocalDateTime dataRetirada,

        @NotNull(message = "A data de devolução prevista é obrigatória")
        @Future(message = "A data de devolução deve ser no futuro")
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
        LocalDateTime dataDevolucaoPrevista,

        @NotNull(message = "O valor acordado é obrigatório")
        @Positive(message = "O valor deve ser maior que zero")
        double valorAcordado
) {}
