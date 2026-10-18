package com.example.gearrent.domain.enums;

public enum StatusLocacao {
    EM_ANDAMENTO, // Equipamento com o cliente dentro do prazo
    CONCLUIDA,    // Equipamento devolvido com sucesso
    ATRASADA,     // Passou da data prevista e aciona a regra de cálculo de multa
    CANCELADA     // Operação abortada antes da retirada
}
