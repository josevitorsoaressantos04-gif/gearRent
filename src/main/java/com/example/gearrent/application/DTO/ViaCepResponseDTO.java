package com.example.gearrent.application.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Representa o JSON retornado pela API pública do ViaCEP (https://viacep.com.br/ws/{cep}/json/).
 * O campo "localidade" do ViaCEP corresponde à cidade, e "uf" corresponde à sigla do estado (2 letras).
 * Quando o CEP não existe na base dos Correios, o ViaCEP retorna {"erro": true} ou {"erro": "true"}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ViaCepResponseDTO(
        String cep,
        String logradouro,
        String complemento,
        String bairro,
        String localidade,
        String uf,
        Object erro
) {
    /** Verifica se o ViaCEP sinalizou que o CEP informado não foi encontrado. */
    public boolean possuiErro() {
        return erro != null && "true".equalsIgnoreCase(String.valueOf(erro).trim());
    }
}
