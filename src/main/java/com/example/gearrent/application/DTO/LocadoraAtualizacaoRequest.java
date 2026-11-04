package com.example.gearrent.application.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CNPJ;

public record LocadoraAtualizacaoRequest(

        @CNPJ(message = "número do registro de contribuinte individual brasileiro (CNPJ) inválido")
        String cnpj,

        @NotBlank(message = "Este campo é obrigatório")
        String nome,

        String nomeFantasia,

        @NotBlank(message = "Informe o telefone da sua empresa.")
        @Size(max = 20, message = "O telefone pode ter no máximo 20 caracteres.")
        String telefone,

        @NotNull(message = "Informe o endereço da sua empresa.")
        @Valid
        EnderecoDTO endereco) {
}