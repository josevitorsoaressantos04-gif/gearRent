package com.example.gearrent.application.DTO;

import com.example.gearrent.domain.entities.Equipamento;
import com.example.gearrent.domain.enums.CategoriaEquipamento;

import java.math.BigDecimal;

public record EquipamentoDisponivelResponse(
        Long id,
        int codigoSequencial,
        String nome,
        String modelo,
        CategoriaEquipamento categoria,
        BigDecimal valorDiaria
) {
    public static EquipamentoDisponivelResponse de(Equipamento equipamento) {
        return new EquipamentoDisponivelResponse(
                equipamento.getId(),
                equipamento.getCodigoSequencial(),
                equipamento.getNome(),
                equipamento.getModelo(),
                equipamento.getCategoria(),
                equipamento.getValorDiaria());
    }
}