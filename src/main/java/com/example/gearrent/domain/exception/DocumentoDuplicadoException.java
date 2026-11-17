package com.example.gearrent.domain.exception;

public class DocumentoDuplicadoException extends ConflitoException {
    public DocumentoDuplicadoException(String message) {
        super(message);
    }
}