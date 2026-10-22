package com.example.gearrent.domain.exceptions;

public class DocumentoDuplicadoException extends ConflitoException {
    public DocumentoDuplicadoException(String message) {
        super(message);
    }
}