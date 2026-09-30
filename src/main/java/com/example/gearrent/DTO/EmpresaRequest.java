package com.example.gearrent.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.br.CNPJ;

// DTO para Cadastro Inicial (POST)
public record EmpresaRequest(
        @NotBlank(message = "A razão social/nome é obrigatória")
        String nome,

        @NotBlank(message = "O CNPJ é obrigatório")
        @CNPJ(message = "O formato do CNPJ é inválido")
        String cnpj,

        @NotBlank(message = "O CNPJ é obrigatório")
        String razaoSocial,

        @NotBlank(message = "O CNPJ é obrigatório")
        String nomeFantasia,

        @NotBlank(message = "O telefone é obrigatório")
        @Pattern(regexp = "^\\(?([1-9]{2})\\)?[-. ]?([2-9][0-9]{3,4})[-. ]?([0-9]{4})$", message = "Telefone em formato inválido")
        String telefone,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email
) {
    public String cnpjLimpo() {
        return cnpj != null ? cnpj.replaceAll("\\D", "") : null;
    }

    public String telefoneLimpo() {
        return telefone != null ? telefone.replaceAll("\\D", "") : null;
    }
}
