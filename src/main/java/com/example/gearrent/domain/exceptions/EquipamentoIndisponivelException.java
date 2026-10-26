package com.example.gearrent.domain.exceptions;

public class EquipamentoIndisponivelException extends ConflitoException {
    public EquipamentoIndisponivelException(String message) {
        super(message);
    }
}