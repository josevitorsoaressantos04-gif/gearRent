package com.example.gearrent.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

// DTO para Atualização Parcial (PUT)
public record EquipamentoPutRequest(
        @NotBlank(message = "O nome é obrigatório")
        String nome,

        String modelo,

        @NotNull(message = "O valor da diária é obrigatório")
        @Positive(message = "O valor da diária deve ser maior que zero")
        BigDecimal valorDiariaBase
) {}