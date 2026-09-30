package com.example.gearrent.DTO;

import com.example.gearrent.entities.Usuario;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public record UsuarioConsultaResponse(
        Long empresa_id,
        String razaoSocial,
        Long id,
        String nome,
        String login,
        String cpf,
        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate dataNascimento
) {
    // Construtor auxiliar para conversão da Entidade -> DTO
    public UsuarioConsultaResponse(Usuario usuario) {
        this(
                usuario.getEmpresa() != null ? usuario.getEmpresa().getId() : null,
                usuario.getEmpresa() != null ? usuario.getEmpresa().getNomeFantasia() : null,
                usuario.getId(),
                usuario.getNome(),
                usuario.getLogin(),
                usuario.getCpf(),
                usuario.getDataNascimento()
        );
    }
}