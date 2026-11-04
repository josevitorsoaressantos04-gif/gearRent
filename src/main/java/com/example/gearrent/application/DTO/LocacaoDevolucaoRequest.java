package com.example.gearrent.application.DTO;

import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

// Corpo opcional: se vier vazio, a devolução é registrada agora.
public record LocacaoDevolucaoRequest(
        @PastOrPresent(message = "A data de devolução não pode ser no futuro.")
        LocalDate dataDevolucao) {
}