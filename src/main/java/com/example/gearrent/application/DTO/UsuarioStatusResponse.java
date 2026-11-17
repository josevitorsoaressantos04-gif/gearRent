package com.example.gearrent.application.DTO;

import com.example.gearrent.domain.enums.StatusCadastro;

// Resposta de desativação/exclusão de usuário: id, situação resultante e mensagem para exibir na tela
public record UsuarioStatusResponse(
        Long id,
        StatusCadastro status,
        String mensagem
) {}
