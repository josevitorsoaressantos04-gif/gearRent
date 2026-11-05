package com.example.gearrent.application.DTO;

import com.example.gearrent.domain.entities.Locacao;
import com.example.gearrent.domain.enums.StatusLocacao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public record LocacaoResumoResponse(
        Long id,
        Long codigoSequencial,
        StatusLocacao status,
        String clienteNome,
        int quantidadeItens,
        LocalDateTime dataRetirada,
        LocalDateTime dataPrevista,
        LocalDateTime dataDevolucao,
        long diasAtraso,
        BigDecimal valorTotal) {

    public static LocacaoResumoResponse de(Locacao l, LocalDateTime agora) {
        return new LocacaoResumoResponse(
                l.getId(),
                l.getCodigoSequencial(),
                l.getStatus(),
                l.getCliente().getNome(),
                l.getEquipamentos().size(),
                l.getDataRetirada(),
                l.getDataPrevista(),
                l.getDataDevolucao(),
                l.calcularDiasAtraso(agora),
                BigDecimal.valueOf(l.getValorFinalCobrado()).setScale(2, RoundingMode.HALF_UP));
    }
}