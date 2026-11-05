package com.example.gearrent.application.DTO;

import com.example.gearrent.domain.enums.TipoPagamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record LocacaoCadastroRequest(
        @NotNull(message = "Escolha o cliente da locação.")
        Long clienteId,

        @NotEmpty(message = "Escolha pelo menos um equipamento.")
        List<@NotNull(message = "Equipamento inválido na lista.") Long> equipamentosIds,

        @NotNull(message = "Informe a data de retirada.")
        @FutureOrPresent(message = "A data de retirada não pode ser no passado.")
        LocalDate dataRetirada,

        @NotNull(message = "Informe até quando o equipamento ficará alugado.")
        LocalDate dataPrevista,

        TipoPagamento tipoPagamento,

        @DecimalMin(value = "0.0", message = "O desconto não pode ser negativo.")
        BigDecimal valorDesconto,   // em reais; vazio = sem desconto

        @Size(max = 150, message = "O nome do responsável pode ter no máximo 150 letras.")
        String responsavelRetirada,

        @Size(max = 20, message = "O documento do responsável pode ter no máximo 20 caracteres.")
        String documentoResponsavelRetirada,

        @Valid
        EnderecoDTO enderecoEntrega) {   // opcional: se vier, precisa estar completo
}