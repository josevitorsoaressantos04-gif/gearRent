package com.example.gearrent.application.DTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ResumoFinanceiroResponse(
        LocalDate dataInicio,
        LocalDate dataFim,
        long quantidadeConcluidas,
        BigDecimal receitaTotal,
        BigDecimal totalMultas,
        BigDecimal totalDescontos,
        List<ReceitaMensalResponse> receitaMensal,
        ValoresEmAbertoResponse valoresEmAberto) {
}