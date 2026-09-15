package com.example.gearrent.DTO;

public record EnderecoResponse(
        Long id,
        String logradouro,
        String numero,
        String cep,
        String bairro,
        String nomeCidade,
        String uf
) {}