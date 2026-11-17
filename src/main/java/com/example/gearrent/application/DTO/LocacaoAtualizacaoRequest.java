package com.example.gearrent.application.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

// Edição de uma locação EM_ANDAMENTO. Campo não enviado (null) = mantém o valor atual.
public record LocacaoAtualizacaoRequest(
        @FutureOrPresent(message = "A nova data de devolução prevista não pode estar no passado.")
        LocalDate dataPrevista,

        @DecimalMin(value = "0.0", message = "O desconto não pode ser negativo.")
        BigDecimal valorDesconto,

        @Size(max = 150, message = "O nome do responsável pode ter no máximo 150 letras.")
        String responsavelRetirada,

        @Size(max = 20, message = "O documento do responsável pode ter no máximo 20 caracteres.")
        String documentoResponsavelRetirada,

        @Valid
        EnderecoDTO enderecoEntrega) {
}
