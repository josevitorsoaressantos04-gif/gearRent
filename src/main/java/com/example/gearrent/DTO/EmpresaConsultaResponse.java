package com.example.gearrent.DTO;

import com.example.gearrent.entities.Empresa;

import java.util.Collections;
import java.util.List;

public record EmpresaConsultaResponse(
        Long id,
        String razaoSocial,
        String nomeFantasia,
        String inscricaoEstadual,
        String cnpj,
        String telefone,
        String email,
        Boolean status,
        List<UsuarioConsultaResponse> usuarios
) {
    // Construtor auxiliar para conversão da Entidade -> DTO
    public EmpresaConsultaResponse(Empresa empresa) {
        this(
                empresa.getId(),
                empresa.getRazaoSocial(),
                empresa.getNomeFantasia(),
                empresa.getInscricaoEstadual(),
                empresa.getCnpj(),
                empresa.getTelefone(),
                empresa.getEmail(),
                empresa.getStatus(),
                empresa.getUsuarios() != null ?
                        empresa.getUsuarios().stream()
                                .map(UsuarioConsultaResponse::new)
                                .toList()
                        : Collections.emptyList()
        );
    }
}