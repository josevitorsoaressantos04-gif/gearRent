package com.example.gearrent.application.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Campo Obrigatório")
        @Email
        String email,

        @NotBlank(message = "Campo Obrigatório")
        String senha) {
}
