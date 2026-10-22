package com.example.gearrent.application.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;

public record UsuarioAtualizacaoRequest(

        @NotBlank(message = "O nome é obrigatório.")
        String nome,

        @NotBlank(message = "O sobrenome é obrigatório.")
        String sobrenome,

        @NotBlank(message = "O CPF é obrigatório.")
        @CPF(message = "O CPF informado não parece correto. Confira os números.")
        String cpf,

        @NotNull(message = "A data de nascimento é obrigatória.")
        LocalDate dataNascimento,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Formato de e-mail inválido.")
        String email,

        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 6, message = "A senha deve conter no mínimo 6 caracteres.")
        String senha
) {
}
