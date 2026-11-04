package com.example.gearrent.domain.exception;

public class TokenRecuperacaoInvalidoException extends RuntimeException {
    public TokenRecuperacaoInvalidoException(String message) {
        super(message);
    }
}