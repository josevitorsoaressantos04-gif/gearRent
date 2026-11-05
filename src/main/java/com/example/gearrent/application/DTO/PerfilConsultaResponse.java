package com.example.gearrent.application.DTO;

import com.example.gearrent.domain.entities.Usuario;
import com.example.gearrent.domain.enums.PapelUsuario;
import com.example.gearrent.domain.enums.StatusCadastro;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PerfilConsultaResponse(
        Long id,
        String nome,
        String sobrenome,
        String email,
        String cpf,
        LocalDate dataNascimento,
        PapelUsuario role,
        StatusCadastro status,
        LocalDateTime avatarAtualizadoEm) {   // nulo = sem foto de perfil

    public static PerfilConsultaResponse de(Usuario u) {
        return new PerfilConsultaResponse(
                u.getId(), u.getNome(), u.getSobrenome(), u.getLogin(), u.getCpf(),
                u.getData_nascimento(), u.getRole(), u.getStatus(), u.getAvatarAtualizadoEm());
    }
}