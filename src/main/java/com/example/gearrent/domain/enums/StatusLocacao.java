package com.example.gearrent.domain.enums;

public enum StatusLocacao {
    EM_ANDAMENTO, ATRASADA, CONCLUIDA, CANCELADA;

    public boolean podeIrPara(StatusLocacao destino) {
        return switch (this) {
            case EM_ANDAMENTO -> destino == ATRASADA
                    || destino == CONCLUIDA
                    || destino == CANCELADA;
            case ATRASADA -> destino == CONCLUIDA;
            default -> false;
        };
    }
}