package com.example.gearrent.application.DTO;

import java.math.BigDecimal;

// Posição de hoje: não depende do período escolhido
public record ValoresEmAbertoResponse(
        long quantidadeEmAndamento,
        long quantidadeAtrasadas,
        BigDecimal valorEmAndamento,      // valor previsto
        BigDecimal valorAtrasadas,        // valor acumulado até hoje, já com multa
        BigDecimal valorMultasAtrasadas,  // parte do valor acima que é multa
        BigDecimal valorTotal) {
}