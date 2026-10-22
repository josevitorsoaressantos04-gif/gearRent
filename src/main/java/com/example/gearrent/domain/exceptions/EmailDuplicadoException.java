package com.example.gearrent.domain.exceptions;

public class EmailDuplicadoException extends ConflitoException {
    public EmailDuplicadoException(String message) {
        super(message);
    }
}