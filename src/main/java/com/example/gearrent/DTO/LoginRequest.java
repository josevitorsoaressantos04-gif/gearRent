package com.example.gearrent.DTO;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank (message = "Campo Obrigatório")
        String login ,

        @NotBlank (message = "Campo Obrigatório")
        String senha) {
}
