package com.example.gearrent.application.DTO;

import com.example.gearrent.domain.entities.Locacao;
import com.example.gearrent.domain.enums.StatusLocacao;
import com.example.gearrent.domain.enums.TipoPagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record LocacaoResponse(
        Long id,
        Long codigoSequencial,
        StatusLocacao status,
        TipoPagamento tipoPagamento,
        Long clienteId,
        String clienteNome,
        List<ItemLocacaoResponse> itens,
        LocalDateTime dataRetirada,
        LocalDateTime dataPrevista,
        LocalDateTime dataDevolucao,
        long diasAtraso,
        BigDecimal valorDiarias,
        BigDecimal valorDesconto,
        BigDecimal valorMulta,
        BigDecimal valorTotal,        // previsto enquanto em andamento; definitivo após a devolução
        String responsavelRetirada,
        String documentoResponsavelRetirada,
        EnderecoDTO enderecoEntrega,
        LocalDateTime dataRegistro) {

    public static LocacaoResponse de(Locacao l, LocalDateTime agora) {
        BigDecimal desconto = dinheiro(l.getValorDesconto());
        BigDecimal multa = dinheiro(l.getValorMultaPorAtraso());
        BigDecimal total = dinheiro(l.getValorFinalCobrado());
        // total = diárias - desconto + multa  =>  diárias = total + desconto - multa
        BigDecimal diarias = total.add(desconto).subtract(multa);

        return new LocacaoResponse(
                l.getId(),
                l.getCodigoSequencial(),
                l.getStatus(),
                l.getTipoPagamento(),
                l.getCliente().getId(),
                l.getCliente().getNome(),
                l.getEquipamentos().stream()
                        .sorted(Comparator.comparingInt(e -> e.getCodigoSequencial()))
                        .map(ItemLocacaoResponse::de)
                        .toList(),
                l.getDataRetirada(),
                l.getDataPrevista(),
                l.getDataDevolucao(),
                l.calcularDiasAtraso(agora),
                diarias,
                desconto,
                multa,
                total,
                l.getResponsavelRetirada(),
                l.getDocumentoResponsavelRetirada(),
                EnderecoDTO.de(l.getEnderecoEntrega()),
                l.getDataRegistro());
    }

    private static BigDecimal dinheiro(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP);
    }
}