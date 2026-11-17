package com.example.gearrent.application.DTO;

public record ClienteAtualizacaoResponse(
        Long id,
        String nome,
        String mensagem
) {
}