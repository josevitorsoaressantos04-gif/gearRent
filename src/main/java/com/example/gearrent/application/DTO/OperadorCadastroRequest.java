package com.example.gearrent.application.DTO;

import com.example.gearrent.domain.enums.PapelUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OperadorCadastroRequest(
        @NotBlank(message = "Informe o nome do operador.")
        String nome,

        @NotBlank(message = "Informe o sobrenome do operador.")
        String sobrenome,

        @NotBlank(message = "Informe o e-mail do operador.")
        @Email(message = "Informe um e-mail válido, por exemplo nome@email.com.")
        String email,

        @NotBlank(message = "Crie uma senha para o operador.")
        @Size(min = 6, message = "A senha precisa ter pelo menos 6 caracteres.")
        String senha,

        @NotNull(message = "Escolha o papel do usuário: ADMIN ou OPERADOR.")
        PapelUsuario role) {
}