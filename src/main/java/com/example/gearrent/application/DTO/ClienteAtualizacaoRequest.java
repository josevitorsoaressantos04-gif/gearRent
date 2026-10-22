package com.example.gearrent.application.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClienteAtualizacaoRequest(

        @NotBlank(message = "Por favor, informe o nome do cliente.")
        @Size(max = 150, message = "O nome do cliente pode ter no máximo 150 letras.")
        String nome,

        @Email(message = "O e-mail informado não parece correto. Exemplo: nome@email.com")
        String email,

        @Size(max = 20, message = "O número de celular está muito longo. Confira e tente novamente.")
        String celular,

        @Size(max = 20, message = "O número de telefone está muito longo. Confira e tente novamente.")
        String telefone,

        @Valid
        EnderecoDTO endereco
) {
}