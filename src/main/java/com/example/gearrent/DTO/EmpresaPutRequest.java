package com.example.gearrent.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// DTO para Atualização Segura (PUT) - O CNPJ foi removido intencionalmente
public record EmpresaPutRequest(
        @NotBlank(message = "A razão social/nome é obrigatória")
        String nome,

        @NotBlank(message = "O telefone é obrigatório")
        String telefone,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email
) {
    public String telefoneLimpo() {
        return telefone != null ? telefone.replaceAll("\\D", "") : null;
    }
}
