package com.example.gearrent.DTO;
// DTO para Saída de Dados (GET)
public record EmpresaResponse(
        Long id,
        String nome,
        String cnpj,
        String telefone,
        String email,
        Boolean ativo
) {}