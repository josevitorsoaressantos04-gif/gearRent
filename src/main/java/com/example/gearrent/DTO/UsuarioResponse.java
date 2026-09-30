package com.example.gearrent.DTO;

import com.example.gearrent.entities.Empresa;
import com.example.gearrent.entities.Usuario;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public record UsuarioResponse(
        Long id,
        String nome,
        String cpf,
        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate dataNascimento,
        String email,
        String telefone,
        Boolean ativo,
        Empresa empresa
) {
        // Construtor auxiliar para conversão direta da Entidade JPA para o DTO
        public UsuarioResponse(Usuario usuario) {
                this(
                        usuario.getId(),
                        usuario.getNome(),
                        usuario.getCpf(),
                        usuario.getDataNascimento(),
                        usuario.getEmail(),
                        usuario.getTelefone(),
                        usuario.getAtivo(),
                        usuario.getEmpresa()
                );
        }
}