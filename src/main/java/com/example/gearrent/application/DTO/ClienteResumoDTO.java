package com.example.gearrent.application.DTO;

import java.math.BigDecimal;

// Resumo sintético do cliente.
// totalLocacoes: todas as locações exceto as canceladas.
// locacoesEmAndamento: EM_ANDAMENTO + ATRASADA.
public record ClienteResumoDTO(
        long totalLocacoes,
        long locacoesEmAndamento,
        long equipamentosEmPosse,
        BigDecimal valorTotalEmAberto) {
}
