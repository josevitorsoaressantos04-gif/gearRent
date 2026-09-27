package com.example.gearrent.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public record UsuarioResponse(
        Long id,
        String nome,
        String cpf,
        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate dataNascimento,
        String email,
        String telefone,
        Boolean ativo
) {}