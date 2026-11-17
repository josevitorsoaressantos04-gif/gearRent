package com.example.gearrent.domain.exception;

public class TransicaoStatusInvalidaException extends RegraDeNegocioException {
    public TransicaoStatusInvalidaException(String mensagem) {
        super(mensagem);
    }
}