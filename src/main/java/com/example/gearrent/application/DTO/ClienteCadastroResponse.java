package com.example.gearrent.application.DTO;

public record ClienteCadastroResponse(
        Long id,
        Long codigoSequencial,
        String nome,
        String mensagem
) {}