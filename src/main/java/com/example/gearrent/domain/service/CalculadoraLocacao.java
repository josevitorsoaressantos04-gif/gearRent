package com.example.gearrent.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class CalculadoraLocacao {

    private static final BigDecimal PERCENTUAL_MULTA = new BigDecimal("0.10");

    private CalculadoraLocacao() {
    }

    public record Resultado(
            long diasCobrados,
            long diasAtraso,
            BigDecimal valorDiarias,   // diárias normais + diárias de atraso
            BigDecimal valorDesconto,  // já limitado ao valor das diárias normais
            BigDecimal valorMulta,
            BigDecimal valorTotal) {
    }

    public static Resultado calcular(LocalDate retirada,
                                     LocalDate prevista,
                                     LocalDate devolucao,
                                     BigDecimal somaDiariasEquipamentos,
                                     BigDecimal valorDescontoSolicitado) {

        long diasPosse = Math.max(1, ChronoUnit.DAYS.between(retirada, devolucao));
        long diasAtraso = Math.max(0, ChronoUnit.DAYS.between(prevista, devolucao));
        long diasNormais = diasPosse - diasAtraso;

        BigDecimal diariasNormais = somaDiariasEquipamentos.multiply(BigDecimal.valueOf(diasNormais));
        BigDecimal diariasAtraso = somaDiariasEquipamentos.multiply(BigDecimal.valueOf(diasAtraso));

        BigDecimal desconto = valorDescontoSolicitado.min(diariasNormais).setScale(2, RoundingMode.HALF_UP);
        BigDecimal multa = diariasAtraso.multiply(PERCENTUAL_MULTA).setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorDiarias = diariasNormais.add(diariasAtraso).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = valorDiarias.subtract(desconto).add(multa);

        return new Resultado(diasPosse, diasAtraso, valorDiarias, desconto, multa, total);
    }
}