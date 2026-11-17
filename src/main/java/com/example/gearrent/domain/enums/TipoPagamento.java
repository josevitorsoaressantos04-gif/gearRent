package com.example.gearrent.domain.enums;

public enum TipoPagamento {
    DINHEIRO,
    PIX,
    CARTAO_CREDITO,
    CARTAO_DEBITO,
    FATURADO // Útil caso clientes com "TipoPessoa.JURIDICA" paguem mensalmente
}
