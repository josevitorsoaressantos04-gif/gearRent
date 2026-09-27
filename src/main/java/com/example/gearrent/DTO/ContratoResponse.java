package com.example.gearrent.DTO;

import java.time.LocalDateTime;

// DTO de Retorno (GET)
public record ContratoResponse(
        Long id,
        Long clienteId,
        LocalDateTime dataRetirada,
        LocalDateTime dataDevolucaoPrevista,
        double valorAcordado,
        Boolean ativo
) {}