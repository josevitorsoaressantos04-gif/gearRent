package com.example.gearrent.application.services;

import com.example.gearrent.application.DTO.*;
import com.example.gearrent.domain.entities.Locacao;
import com.example.gearrent.domain.enums.StatusLocacao;
import com.example.gearrent.domain.exceptions.RegraDeNegocioException;
import com.example.gearrent.domain.repository.*;
// Ajuste este import conforme o pacote onde você colocou a LocacaoSpecifications
// (se estiver em domain.repository, ele já é coberto pelo import acima e esta linha pode sair)
import com.example.gearrent.domain.repository.LocacaoSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VisaoGeralQueryService {

    private static final int TAMANHO_MAXIMO_PAGINA = 50;

    // Campos reais da entidade Locacao
    private static final Set<String> ORDENACOES_PERMITIDAS =
            Set.of("dataRetirada", "dataPrevista", "status", "valorFinalCobrado");

    private final LocacaoRepository locacaoRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final ClienteRepository clienteRepository;

    public VisaoGeralResumoDTO buscarResumo(Long locadoraId) {
        Map<StatusLocacao, Long> porStatus = locacaoRepository.contarPorStatus(locadoraId).stream()
                .collect(Collectors.toMap(ContagemPorStatus::status, ContagemPorStatus::total));

        long total = porStatus.values().stream().mapToLong(Long::longValue).sum();

        // A query devolve Double (valorFinalCobrado é double na entidade)
        BigDecimal faturamento = BigDecimal.valueOf(
                locacaoRepository.somarFaturamento(locadoraId, StatusLocacao.CANCELADA));

        return new VisaoGeralResumoDTO(
                total,
                porStatus.getOrDefault(StatusLocacao.EM_ANDAMENTO, 0L),
                porStatus.getOrDefault(StatusLocacao.ATRASADA, 0L),
                faturamento
        );
    }

    public OpcoesFiltrosDTO buscarOpcoesFiltros(Long locadoraId) {
        var filtros = new FiltrosDinamicosDTO(
                equipamentoRepository.listarCategorias(locadoraId),
                equipamentoRepository.listarModelos(locadoraId)
        );
        return new OpcoesFiltrosDTO(filtros, clienteRepository.listarParaDropdown(locadoraId));
    }

    public Page<LocacaoListagemSinteticaDTO> listarLocacoes(Long locadoraId,
                                                            FiltroLocacaoDTO filtro,
                                                            Pageable pageable) {
        validarPeriodo(filtro);

        // Query 1 (+ count): locações da página com cliente já carregado
        Page<Locacao> pagina = locacaoRepository.findAll(
                LocacaoSpecifications.comFiltros(locadoraId, filtro),
                sanitizar(pageable));

        // Query 2: itens de todas as locações da página, de uma vez
        List<Long> ids = pagina.getContent().stream().map(Locacao::getId).toList();
        Map<Long, List<LocacaoListagemSinteticaDTO.ItemResumo>> itensPorLocacao = ids.isEmpty()
                ? Map.of()
                : locacaoRepository.buscarItensDasLocacoes(locadoraId, ids).stream()
                .collect(Collectors.groupingBy(
                        ItemLocacaoProjecao::locacaoId,
                        Collectors.mapping(i -> new LocacaoListagemSinteticaDTO.ItemResumo(
                                i.equipamentoId(), i.nome(), i.modelo(),
                                i.categoria(), i.numeroPatrimonio()), Collectors.toList())));

        return pagina.map(l -> paraDTO(l, itensPorLocacao.getOrDefault(l.getId(), List.of())));
    }

    private LocacaoListagemSinteticaDTO paraDTO(Locacao l,
                                                List<LocacaoListagemSinteticaDTO.ItemResumo> itens) {
        // Diárias: retirada -> devolução real; se ainda não devolveu, usa a prevista
        LocalDateTime fim = l.getDataReal() != null ? l.getDataReal() : l.getDataPrevista();
        long diarias = (l.getDataRetirada() == null || fim == null)
                ? 1
                : Math.max(1, ChronoUnit.DAYS.between(l.getDataRetirada(), fim));

        return new LocacaoListagemSinteticaDTO(
                l.getId(),
                new LocacaoListagemSinteticaDTO.ClienteResumo(
                        l.getCliente().getCodigoSequencial(), l.getCliente().getNome()),
                itens,
                diarias,
                l.getStatus(),
                l.getDataRetirada(),
                l.getDataPrevista(),
                l.getDataReal(),
                l.getValorFinalCobrado()
        );
    }

    private void validarPeriodo(FiltroLocacaoDTO f) {
        if (f.dataInicio() != null && f.dataFim() != null && f.dataInicio().isAfter(f.dataFim())) {
            throw new RegraDeNegocioException("A data inicial não pode ser maior que a data final.");
        }
    }

    // Limita o tamanho da página e aceita só ordenações conhecidas (evita sort por campo arbitrário)
    private Pageable sanitizar(Pageable p) {
        int tamanho = Math.min(p.getPageSize(), TAMANHO_MAXIMO_PAGINA);
        List<Sort.Order> ordens = p.getSort().stream()
                .filter(o -> ORDENACOES_PERMITIDAS.contains(o.getProperty()))
                .toList();
        Sort sort = ordens.isEmpty()
                ? Sort.by(Sort.Direction.DESC, "dataRetirada")
                : Sort.by(ordens);
        return PageRequest.of(p.getPageNumber(), tamanho, sort);
    }
}