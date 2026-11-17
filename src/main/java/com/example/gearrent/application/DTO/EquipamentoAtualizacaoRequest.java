package com.example.gearrent.application.DTO;

import com.example.gearrent.domain.enums.CategoriaEquipamento;
import com.example.gearrent.domain.enums.StatusEquipamento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record EquipamentoAtualizacaoRequest(

        @NotBlank(message = "Por favor, informe o nome do equipamento.")
        @Size(max = 150, message = "O nome do equipamento pode ter no máximo 150 letras.")
        String nome,

        @Size(max = 100, message = "O modelo pode ter no máximo 100 letras.")
        String modelo,

        @Size(max = 50, message = "O número de patrimônio pode ter no máximo 50 caracteres.")
        String numeroPatrimonio,

        @NotNull(message = "Por favor, escolha a categoria do equipamento.")
        CategoriaEquipamento categoria,

        @NotNull(message = "Por favor, informe o valor da diária.")
        @Positive(message = "O valor da diária precisa ser maior que zero.")
        BigDecimal valorDiaria,

        // Opcional: só aceita DISPONIVEL ou MANUTENCAO. "Alugado" é controlado pelas locações.
        StatusEquipamento status
) {
}