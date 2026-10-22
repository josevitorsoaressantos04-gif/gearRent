package com.example.gearrent.domain.exceptions;

public class TokenRecuperacaoInvalidoException extends RuntimeException {
    public TokenRecuperacaoInvalidoException(String message) {
        super(message);
    }
}