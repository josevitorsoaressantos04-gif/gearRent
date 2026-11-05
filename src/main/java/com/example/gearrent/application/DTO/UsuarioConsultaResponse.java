package com.example.gearrent.application.DTO;

import com.example.gearrent.domain.entities.Usuario;
import com.example.gearrent.domain.enums.PapelUsuario;
import com.example.gearrent.domain.enums.StatusCadastro;

import java.time.LocalDate;

public record UsuarioConsultaResponse(
        Long id,
        String login,
        String nome,
        String sobrenome,
        LocalDate dataNascimento,
        PapelUsuario role,
        StatusCadastro status
) {
    public static UsuarioConsultaResponse de(Usuario usuario) {
        return new UsuarioConsultaResponse(
                usuario.getId(),
                usuario.getLogin(),
                usuario.getNome(),
                usuario.getSobrenome(),
                usuario.getData_nascimento(),
                usuario.getRole(),
                usuario.getStatus()
        );
    }
}