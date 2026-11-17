package com.example.gearrent.application.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EsqueciSenhaRequest(

        @NotBlank(message = "Por favor, informe o seu e-mail.")
        @Email(message = "O e-mail informado não parece correto. Exemplo: nome@email.com")
        String email
) {
}