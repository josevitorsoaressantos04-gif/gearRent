package com.example.gearrent.application.DTO;

import com.example.gearrent.domain.entities.Equipamento;
import com.example.gearrent.domain.enums.CategoriaEquipamento;
import com.example.gearrent.domain.enums.StatusEquipamento;

import java.math.BigDecimal;

public record EquipamentoConsultaResponse(
        Long id,
        int codigoSequencial,
        String nome,
        String modelo,
        String numeroPatrimonio,
        CategoriaEquipamento categoria,
        StatusEquipamento status,
        BigDecimal valorDiaria
) {
    public static EquipamentoConsultaResponse de(Equipamento equipamento) {
        return new EquipamentoConsultaResponse(
                equipamento.getId(),
                equipamento.getCodigoSequencial(),
                equipamento.getNome(),
                equipamento.getModelo(),
                equipamento.getNumeroPatrimonio(),
                equipamento.getCategoria(),
                equipamento.getStatus(),
                equipamento.getValorDiaria());
    }
}