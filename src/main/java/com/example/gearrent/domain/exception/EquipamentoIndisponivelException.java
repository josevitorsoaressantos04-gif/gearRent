package com.example.gearrent.domain.exception;

public class EquipamentoIndisponivelException extends ConflitoException {
    public EquipamentoIndisponivelException(String message) {
        super(message);
    }
}