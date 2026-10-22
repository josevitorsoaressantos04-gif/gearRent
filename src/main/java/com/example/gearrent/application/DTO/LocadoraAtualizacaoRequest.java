package com.example.gearrent.application.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.hibernate.validator.constraints.br.CNPJ;

public record LocadoraAtualizacaoRequest(

        @CNPJ(message = "número do registro de contribuinte individual brasileiro (CNPJ) inválido")
        String cnpj,

        @NotBlank(message = "Este campo é obrigatório")
        String nome,

        String nomeFantasia,

        @NotBlank(message = "Este campo é obrigatório")
        String logradouro,

        @NotBlank(message = "Este campo é obrigatório")
        String numero,

        @NotBlank(message = "Este campo é obrigatório")
        String complemento,

        @NotBlank(message = "Este campo é obrigatório")
        String bairro,

        @NotBlank(message = "Este campo é obrigatório")
        String cidade,

        @NotBlank(message = "Este campo é obrigatório")
        String estado,

        @NotBlank(message = "Este campo é obrigatório")
        String cep

) {
}