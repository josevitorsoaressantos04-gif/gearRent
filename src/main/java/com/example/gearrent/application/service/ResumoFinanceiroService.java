package com.example.gearrent.application.service;

import com.example.gearrent.application.DTO.ReceitaMensalResponse;
import com.example.gearrent.application.DTO.ResumoFinanceiroResponse;
import com.example.gearrent.application.DTO.ValoresEmAbertoResponse;
import com.example.gearrent.domain.entities.Locacao;
import com.example.gearrent.domain.enums.StatusLocacao;
import com.example.gearrent.domain.exception.RegraDeNegocioException;
import com.example.gearrent.domain.repository.LocacaoRepository;
import com.example.gearrent.domain.repository.LocacaoRepository.ReceitaMensalLinha;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResumoFinanceiroService {

    private static final int MAXIMO_MESES = 24;

    private final LocacaoRepository locacaoRepository;
    private final Clock clock;

    public ResumoFinanceiroService(LocacaoRepository locacaoRepository, Clock clock) {
        this.locacaoRepository = locacaoRepository;
        this.clock = clock;
    }

    @Transactional
    public ResumoFinanceiroResponse gerar(Long locadoraId, LocalDate dataInicio, LocalDate dataFim) {
        LocalDateTime agora = LocalDateTime.now(clock);

        // Período padrão: do dia 1 do mês atual até hoje
        LocalDate fim = dataFim != null ? dataFim : agora.toLocalDate();
        LocalDate inicio = dataInicio != null ? dataInicio : fim.withDayOfMonth(1);

        if (fim.isBefore(inicio)) {
            throw new RegraDeNegocioException("A data final precisa ser igual ou depois da data inicial.");
        }
        if (YearMonth.from(inicio).until(YearMonth.from(fim), ChronoUnit.MONTHS) >= MAXIMO_MESES) {
            throw new RegraDeNegocioException(
                    "Escolha um período de no máximo " + MAXIMO_MESES + " meses.");
        }

        // ---------- Receita do período (locações concluídas, por mês da devolução) ----------
        List<ReceitaMensalLinha> linhas = locacaoRepository.receitaMensal(
                locadoraId,
                StatusLocacao.CONCLUIDA,
                inicio.atStartOfDay(),
                fim.plusDays(1).atStartOfDay());   // fim exclusivo: inclui o dia final inteiro

        Map<YearMonth, ReceitaMensalLinha> porMes = new HashMap<>();
        for (ReceitaMensalLinha linha : linhas) {
            porMes.put(YearMonth.of(linha.getAno(), linha.getMes()), linha);
        }

        List<ReceitaMensalResponse> receitaMensal = new ArrayList<>();
        long quantidadeConcluidas = 0;
        BigDecimal receitaTotal = zero();
        BigDecimal totalMultas = zero();
        BigDecimal totalDescontos = zero();

        // Percorre todos os meses do período, inclusive os sem movimento (aparecem com zero)
        for (YearMonth mes = YearMonth.from(inicio); !mes.isAfter(YearMonth.from(fim)); mes = mes.plusMonths(1)) {
            ReceitaMensalLinha linha = porMes.get(mes);
            long quantidade = linha == null ? 0 : linha.getQuantidade();
            BigDecimal receita = linha == null ? zero() : dinheiro(linha.getReceita());

            quantidadeConcluidas += quantidade;
            receitaTotal = receitaTotal.add(receita);
            if (linha != null) {
                totalMultas = totalMultas.add(dinheiro(linha.getMultas()));
                totalDescontos = totalDescontos.add(dinheiro(linha.getDescontos()));
            }

            receitaMensal.add(new ReceitaMensalResponse(
                    mes.getYear(), mes.getMonthValue(),
                    String.format("%02d/%d", mes.getMonthValue(), mes.getYear()),
                    quantidade, receita));
        }

        return new ResumoFinanceiroResponse(
                inicio, fim, quantidadeConcluidas, receitaTotal, totalMultas, totalDescontos,
                receitaMensal, calcularEmAberto(locadoraId, agora));
    }

    // Posição de hoje: locações ainda não devolvidas (independe do período)
    private ValoresEmAbertoResponse calcularEmAberto(Long locadoraId, LocalDateTime agora) {
        List<Locacao> abertas = locacaoRepository.findByLocadoraIdAndStatusIn(
                locadoraId, List.of(StatusLocacao.EM_ANDAMENTO, StatusLocacao.ATRASADA));

        long qtdAndamento = 0;
        long qtdAtrasadas = 0;
        BigDecimal valorAndamento = zero();
        BigDecimal valorAtrasadas = zero();
        BigDecimal multasAtrasadas = zero();

        for (Locacao locacao : abertas) {
            // Marca ATRASADA e atualiza o valor acumulado até agora, se já passou do prazo
            locacao.sincronizarAtraso(agora);

            BigDecimal valor = dinheiro(locacao.getValorFinalCobrado());
            if (locacao.getStatus() == StatusLocacao.ATRASADA) {
                qtdAtrasadas++;
                valorAtrasadas = valorAtrasadas.add(valor);
                multasAtrasadas = multasAtrasadas.add(dinheiro(locacao.getValorMultaPorAtraso()));
            } else {
                qtdAndamento++;
                valorAndamento = valorAndamento.add(valor);
            }
        }

        return new ValoresEmAbertoResponse(
                qtdAndamento, qtdAtrasadas, valorAndamento, valorAtrasadas, multasAtrasadas,
                valorAndamento.add(valorAtrasadas));
    }

    private BigDecimal dinheiro(Double valor) {
        return valor == null ? zero() : BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2);
    }
}