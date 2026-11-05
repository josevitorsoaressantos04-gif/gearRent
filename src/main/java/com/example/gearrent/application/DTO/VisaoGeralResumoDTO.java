package com.example.gearrent.application.DTO;

import java.math.BigDecimal;

public record VisaoGeralResumoDTO(
        long totalLocacoes,
        long emAndamento,
        long atrasadas,
        BigDecimal faturamentoTotal
) {}