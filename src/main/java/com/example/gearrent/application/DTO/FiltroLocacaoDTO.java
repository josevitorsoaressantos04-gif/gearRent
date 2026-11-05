package com.example.gearrent.application.DTO;

import com.example.gearrent.domain.enums.CategoriaEquipamento;
import com.example.gearrent.domain.enums.StatusLocacao;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

/** Filtros vindos dos query params. Note: NÃO existe locadoraId aqui, de propósito. */
public record FiltroLocacaoDTO(
        StatusLocacao status,
        Long clienteId,
        CategoriaEquipamento categoria,
        String modelo,
        String numeroPatrimonio,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim
) {}