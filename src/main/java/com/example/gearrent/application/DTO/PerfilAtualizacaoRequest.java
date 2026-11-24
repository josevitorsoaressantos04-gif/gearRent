package com.example.gearrent.application.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PerfilAtualizacaoRequest(
        @NotBlank(message = "Informe o seu nome.")
        String nome,

        @NotBlank(message = "Informe o seu sobrenome.")
        String sobrenome,

        @NotBlank(message = "Informe o seu e-mail.")
        @Email(message = "Formato de e-mail inválido.")
        String email,

        String cpf) {
}