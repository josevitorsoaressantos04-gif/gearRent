package com.example.gearrent.application.DTO;

import com.example.gearrent.domain.entities.Equipamento;
import com.example.gearrent.domain.enums.CategoriaEquipamento;
import com.example.gearrent.domain.enums.StatusEquipamento;

import java.math.BigDecimal;

public record EquipamentoListagemResponse(
        Long id,
        int codigoSequencial,
        String nome,
        CategoriaEquipamento categoria,
        StatusEquipamento status,
        BigDecimal valorDiaria
) {
    public static EquipamentoListagemResponse de(Equipamento equipamento) {
        return new EquipamentoListagemResponse(
                equipamento.getId(),
                equipamento.getCodigoSequencial(),
                equipamento.getNome(),
                equipamento.getCategoria(),
                equipamento.getStatus(),
                equipamento.getValorDiaria());
    }
}