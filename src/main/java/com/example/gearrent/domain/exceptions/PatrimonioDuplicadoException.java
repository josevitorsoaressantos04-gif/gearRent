package com.example.gearrent.domain.exceptions;

public class PatrimonioDuplicadoException extends ConflitoException {
    public PatrimonioDuplicadoException(String message) {
        super(message);
    }
}