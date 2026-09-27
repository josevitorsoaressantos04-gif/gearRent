package com.example.gearrent.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ClienteRequest(
        @NotBlank(message = "O nome é obrigatório")
        String nome,

        @NotBlank(message = "O CPF é obrigatório")
        @Pattern(regexp = "^(\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}|\\d{11})$", message = "O CPF deve conter exatos 11 dígitos")
        String cpf,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @NotBlank(message = "O telefone é obrigatório")
        @Pattern(regexp = "^\\(?([1-9]{2})\\)?[-. ]?([2-9][0-9]{3,4})[-. ]?([0-9]{4})$", message = "Telefone inválido")
        String telefone
) {
        // Sanitização automática
        public String cpf() {
                return cpf != null ? cpf.replaceAll("\\D", "") : null;
        }

        public String email() {
                return email != null ? email.toLowerCase().trim() : null;
        }

        public String telefone() {
                return telefone != null ? telefone.replaceAll("\\D", "") : null;
        }
}