package com.example.gearrent.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EnderecoRequest(
        @NotBlank(message = "O logradouro é obrigatório")
        String logradouro,

        String numero,

        @NotBlank(message = "O CEP é obrigatório")
        String cep,

        String bairro,

        @NotNull(message = "O ID da cidade é obrigatório")
        Long cidadeId
) {}