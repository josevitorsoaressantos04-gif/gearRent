package com.example.gearrent.DTO;


import jakarta.validation.constraints.NotBlank;

public record UsuarioRequest(
        @NotBlank (message = "Campo Obrigatório")
        String nome,

        @NotBlank (message = "Campo Obrigatório")
        String cpf,

        @NotBlank (message = "Campo Obrigatório")
        String dataNascimento,

        @NotBlank (message = "Campo Obrigatório")
        String email,

        @NotBlank (message = "Campo Obrigatório")
        String login,

        @NotBlank (message = "Campo Obrigatório")
        String senha,

        String telefone) {
}
