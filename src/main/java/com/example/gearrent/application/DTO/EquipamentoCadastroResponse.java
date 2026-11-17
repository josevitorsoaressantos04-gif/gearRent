package com.example.gearrent.application.DTO;

public record EquipamentoCadastroResponse(
        Long id,
        int codigoSequencial,
        String nome,
        String mensagem
) {
}