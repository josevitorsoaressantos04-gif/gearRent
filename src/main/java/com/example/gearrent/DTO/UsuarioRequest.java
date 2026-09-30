package com.example.gearrent.DTO;

import com.example.gearrent.entities.Empresa;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;


public record UsuarioRequest(
        @NotBlank(message = "O nome é obrigatório")
        String nome,

        @NotBlank( message = "O login é obrigatório")
        String login,

        @NotBlank(message = "O CPF é obrigatório")
        @Pattern(
                regexp = "^(\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}|\\d{11})$",
                message = "O CPF deve conter exatos 11 dígitos no formato 000.000.000-00 ou apenas números"
        )
        @CPF(message = "O CPF informado é inválido pelos dígitos verificadores")
        String cpf,

        @NotNull(message = "A data de nascimento é obrigatória")
        @Past(message = "A data de nascimento deve ser uma data passada")
        @JsonFormat(pattern = "dd-MM-yyyy")
        LocalDate dataNascimento,

        @NotBlank(message = "O e-mail/login é obrigatório")
        @Email(message = "Formato de e-mail/login inválido")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres")
        String senha,

        @NotBlank(message = "O telefone é obrigatório")
        @Pattern(
                regexp = "^\\(?([1-9]{2})\\)?[-. ]?([2-9][0-9]{3,4})[-. ]?([0-9]{4})$",
                message = "Telefone inválido. Formato esperado: (00)000000000 ou (00)00000000"
        )
        String telefone,

        Long empresa_id
) {
        // Sanitização: limpa caracteres especiais e força e-mail/login em minúsculo
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