package com.example.gearrent.application.DTO;

import com.example.gearrent.domain.entities.Usuario;
import com.example.gearrent.domain.enums.PapelUsuario;
import com.example.gearrent.domain.enums.StatusCadastro;

public record OperadorCadastroResponse(
        Long id,
        String nome,
        String sobrenome,
        String email,
        PapelUsuario role,
        StatusCadastro status) {

    public static OperadorCadastroResponse de(Usuario u) {
        return new OperadorCadastroResponse(
                u.getId(), u.getNome(), u.getSobrenome(), u.getLogin(), u.getRole(), u.getStatus());
    }
}