package com.example.gearrent.application.DTO;

import com.example.gearrent.domain.entities.Equipamento;
import com.example.gearrent.domain.enums.CategoriaEquipamento;

import java.math.BigDecimal;

// Um equipamento dentro da locação (usado na resposta de detalhes/contrato)
public record ItemLocacaoResponse(
        Long equipamentoId,
        int codigoSequencial,
        String nome,
        String modelo,
        String numeroPatrimonio,
        CategoriaEquipamento categoria,
        BigDecimal valorDiaria) {   // valor atual da diária do equipamento

    public static ItemLocacaoResponse de(Equipamento e) {
        return new ItemLocacaoResponse(
                e.getId(),
                e.getCodigoSequencial(),
                e.getNome(),
                e.getModelo(),
                e.getNumeroPatrimonio(),
                e.getCategoria(),
                e.getValorDiaria());
    }
}