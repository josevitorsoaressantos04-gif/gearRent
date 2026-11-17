package com.example.gearrent.domain.exception;

public class EmailDuplicadoException extends ConflitoException {
    public EmailDuplicadoException(String message) {
        super(message);
    }
}