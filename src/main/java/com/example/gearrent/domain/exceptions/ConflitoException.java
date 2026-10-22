package com.example.gearrent.domain.exceptions;

public class ConflitoException extends RegraDeNegocioException {
    public ConflitoException(String message) {
        super(message);
    }
}