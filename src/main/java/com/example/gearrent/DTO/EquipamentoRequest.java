package com.example.gearrent.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record EquipamentoRequest(
        @NotBlank(message = "O nome do equipamento é obrigatório")
        String nome,

        @NotBlank(message = "O número de patrimônio é obrigatório")
        String numeroPatrimonio,

        String modelo,

        @NotNull(message = "O valor da diária base é obrigatório")
        @Positive(message = "O valor da diária deve ser maior que zero")
        double valorDiariaBase
) {}