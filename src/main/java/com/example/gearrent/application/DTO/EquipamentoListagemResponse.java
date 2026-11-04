package com.example.gearrent.application.DTO;

import com.example.gearrent.domain.entities.Equipamento;
import com.example.gearrent.domain.enums.CategoriaEquipamento;
import com.example.gearrent.domain.enums.StatusEquipamento;

import java.math.BigDecimal;

// Se você já tem um EquipamentoListagemResponse, mantenha os campos que a tela usa.
public record EquipamentoListagemResponse(
        Long id,
        int codigoSequencial,
        String nome,
        String modelo,
        String numeroPatrimonio,
        CategoriaEquipamento categoria,
        StatusEquipamento status,
        BigDecimal valorDiaria) {

    public static EquipamentoListagemResponse de(Equipamento e) {
        return new EquipamentoListagemResponse(
                e.getId(), e.getCodigoSequencial(), e.getNome(), e.getModelo(),
                e.getNumeroPatrimonio(), e.getCategoria(), e.getStatus(), e.getValorDiaria());
    }
}