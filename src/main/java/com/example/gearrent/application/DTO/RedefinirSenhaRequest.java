package com.example.gearrent.application.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaRequest(

        @NotBlank(message = "O link para criar a nova senha não foi identificado. Abra o link do e-mail novamente.")
        String token,

        @NotNull(message = "Por favor, informe a nova senha.")
        @Size(min = 6, message = "A nova senha deve conter no mínimo 6 caracteres.")
        String novaSenha,

        @NotBlank(message = "Por favor, digite a nova senha mais uma vez para confirmar.")
        String confirmacaoSenha
) {
}