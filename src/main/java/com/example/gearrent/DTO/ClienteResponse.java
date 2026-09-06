package com.example.gearrent.DTO;

public record ClienteResponse(
        Long id,
        String nome,
        String email,
        Boolean ativo
) {}