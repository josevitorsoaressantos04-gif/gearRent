package com.example.gearrent.application.DTO;

import com.example.gearrent.domain.enums.StatusCadastro;

public record UsuarioReativacaoResponse(
        Long id,
        String email,
        StatusCadastro status,
        String mensagem) {
}