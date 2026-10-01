package com.example.gearrent.DTO;

import com.example.gearrent.entities.Usuario;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UsuarioConsultaResponse(
        Long id,
        Boolean ativo,
        String nome,
        String cpf,
        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate dataNascimento,
        String email,
        String login,
        String telefone,
        Long empresaId,
        String empresaRazaoSocial,
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
        LocalDateTime dataCadastro,
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
        LocalDateTime dataAtualizacao
) {
    // Construtor auxiliar para conversão da Entidade -> DTO
    public UsuarioConsultaResponse(Usuario usuario) {
        this(
                usuario.getId(),
                usuario.getAtivo(),
                usuario.getNome(),
                usuario.getCpf(),
                usuario.getDataNascimento(),
                usuario.getEmail(),
                usuario.getLogin(),
                usuario.getTelefone(),
                usuario.getEmpresa() != null ? usuario.getEmpresa().getId() : null,
                usuario.getEmpresa() != null ? usuario.getEmpresa().getRazaoSocial() : null,
                usuario.getDataCadastro(),
                usuario.getDataAtualizacao()
        );
    }
}