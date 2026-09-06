package com.example.gearrent.DTO;

import java.math.BigDecimal;

public record EquipamentoResponse(
        Long id,
        String nome,
        String numeroPatrimonio,
        String modelo,
        BigDecimal valorDiariaBase
) {}