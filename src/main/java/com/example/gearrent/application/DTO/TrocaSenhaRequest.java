package com.example.gearrent.application.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrocaSenhaRequest(
        @NotBlank(message = "Informe a sua senha atual.")
        String senhaAtual,

        @NotBlank(message = "Crie uma nova senha.")
        @Size(min = 6, message = "A nova senha precisa ter pelo menos 6 caracteres.")
        String novaSenha,

        @NotBlank(message = "Digite a nova senha novamente para confirmar.")
        String confirmacaoSenha) {
}