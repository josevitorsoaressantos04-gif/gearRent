package com.example.gearrent.application.DTO;

import java.math.BigDecimal;

public record ReceitaMensalResponse(
        int ano,
        int mes,
        String rotulo,        // ex.: "11/2026", pronto para o eixo do gráfico
        long quantidade,      // locações concluídas no mês
        BigDecimal receita) {
}