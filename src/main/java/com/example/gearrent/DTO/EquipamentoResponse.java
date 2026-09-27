package com.example.gearrent.DTO;

import java.math.BigDecimal;

// DTO para Leitura (GET)
public record EquipamentoResponse(
        Long id,
        String nome,
        String numeroPatrimonio,
        String modelo,
        BigDecimal valorDiariaBase,
        String status,
        Boolean ativo
) {}