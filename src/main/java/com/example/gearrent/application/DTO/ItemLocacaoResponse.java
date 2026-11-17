package com.example.gearrent.application.DTO;

import com.example.gearrent.domain.entities.Equipamento;
import com.example.gearrent.domain.enums.CategoriaEquipamento;

import java.math.BigDecimal;
import java.math.RoundingMode;

// Um equipamento dentro da locação, com o preço combinado e o subtotal dos dias cobrados
public record ItemLocacaoResponse(
        Long equipamentoId,
        int codigoSequencial,
        String nome,
        String modelo,
        String numeroPatrimonio,
        CategoriaEquipamento categoria,
        BigDecimal valorDiaria,   // preço da diária no momento da locação
        BigDecimal subtotal) {    // valorDiaria x dias cobrados (antes de desconto e multa)

    public static ItemLocacaoResponse de(Equipamento e, BigDecimal valorDiaria, long diasCobrados) {
        BigDecimal subtotal = valorDiaria
                .multiply(BigDecimal.valueOf(diasCobrados))
                .setScale(2, RoundingMode.HALF_UP);

        return new ItemLocacaoResponse(
                e.getId(),
                e.getCodigoSequencial(),
                e.getNome(),
                e.getModelo(),
                e.getNumeroPatrimonio(),
                e.getCategoria(),
                valorDiaria,
                subtotal);
    }
}