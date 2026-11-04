package com.example.gearrent.domain.exception;

public class PatrimonioDuplicadoException extends ConflitoException {
    public PatrimonioDuplicadoException(String message) {
        super(message);
    }
}