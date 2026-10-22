package com.example.gearrent.application.DTO;

import com.example.gearrent.domain.entities.Endereco;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record EnderecoDTO(

        @NotBlank(message = "Por favor, informe o nome da rua ou avenida.")
        String logradouro,

        @NotBlank(message = "Por favor, informe o número do endereço. Se não houver, escreva S/N.")
        String numero,

        String complemento,

        @NotBlank(message = "Por favor, informe o bairro.")
        String bairro,

        @NotBlank(message = "Por favor, informe a cidade.")
        String cidade,

        @NotNull(message = "Por favor, informe o estado com duas letras (exemplo: SC).")
        @Pattern(regexp = "^[A-Za-z]{2}$", message = "Por favor, informe o estado com duas letras (exemplo: SC).")
        String estado,

        @NotNull(message = "Por favor, informe o CEP com 8 números (exemplo: 88800-000).")
        @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "Por favor, informe o CEP com 8 números (exemplo: 88800-000).")
        String cep
) {
    public static EnderecoDTO de(Endereco endereco) {
        if (endereco == null) {
            return null;
        }
        return new EnderecoDTO(
                endereco.getLogradouro(),
                endereco.getNumero(),
                endereco.getComplemento(),
                endereco.getBairro(),
                endereco.getCidade(),
                endereco.getEstado(),
                endereco.getCep());
    }

    public Endereco paraEntidade() {
        return new Endereco(
                logradouro.trim(),
                numero.trim(),
                complemento == null || complemento.isBlank() ? null : complemento.trim(),
                bairro.trim(),
                cidade.trim(),
                estado.trim().toUpperCase(),
                cep.trim());
    }
}