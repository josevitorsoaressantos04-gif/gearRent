package com.example.gearrent.application.DTO;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

// Corpo opcional: se vier vazio, a devolução é registrada agora.
// Aceita "dataDevolucao" (oficial) ou "dataReal" (alias usado por versões antigas da interface).
public record LocacaoDevolucaoRequest(
        @JsonAlias("dataReal")
        @PastOrPresent(message = "A data de devolução não pode ser no futuro.")
        LocalDate dataDevolucao) {
}