package com.example.gearrent.DTO;

import com.example.gearrent.entities.Usuario;

import java.util.List;

// DTO para Saída de Dados (GET)
public record EmpresaResponse(
        Long id,
        String nome,
        String cnpj,
        String telefone,
        String email,
        Boolean ativo,
        List<Usuario> usuario
) {}