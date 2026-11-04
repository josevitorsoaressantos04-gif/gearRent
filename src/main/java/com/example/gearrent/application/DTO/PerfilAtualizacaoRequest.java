package com.example.gearrent.application.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.br.CPF;

// Sem campo de senha: a senha só muda em PATCH /api/usuarios/trocar-senha
public record PerfilAtualizacaoRequest(
        @NotBlank(message = "Informe o seu nome.")
        String nome,

        @NotBlank(message = "Informe o seu sobrenome.")
        String sobrenome,

        @NotBlank(message = "Informe o seu e-mail.")
        @Email(message = "Formato de e-mail inválido.")
        String email,

        @NotBlank(message = "Informe o seu CPF.")
        @CPF(message = "O CPF informado não parece correto. Confira os números.")
        String cpf) {
}