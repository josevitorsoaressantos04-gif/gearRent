package com.example.gearrent.application.service;

import com.example.gearrent.application.DTO.LocacaoAtualizacaoRequest;
import com.example.gearrent.application.DTO.LocacaoCadastroRequest;
import com.example.gearrent.application.DTO.LocacaoDevolucaoRequest;
import com.example.gearrent.application.DTO.LocacaoResponse;
import com.example.gearrent.domain.entities.Cliente;
import com.example.gearrent.domain.entities.Endereco;
import com.example.gearrent.domain.entities.Equipamento;
import com.example.gearrent.domain.entities.Locacao;
import com.example.gearrent.domain.enums.StatusCadastro;
import com.example.gearrent.domain.exception.RecursoNaoEncontradoException;
import com.example.gearrent.domain.exception.RegraDeNegocioException;
import com.example.gearrent.domain.repository.ClienteRepository;
import com.example.gearrent.domain.repository.EquipamentoRepository;
import com.example.gearrent.domain.repository.LocacaoRepository;
import com.example.gearrent.domain.repository.LocadoraRepository;
import com.example.gearrent.domain.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;

@Service
public class LocacaoService {

    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final LocacaoRepository locacaoRepository;
    private final ClienteRepository clienteRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final LocadoraRepository locadoraRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    public LocacaoService(LocacaoRepository locacaoRepository,
                          ClienteRepository clienteRepository,
                          EquipamentoRepository equipamentoRepository,
                          LocadoraRepository locadoraRepository,
                          UsuarioRepository usuarioRepository,
                          AuditoriaService auditoriaService,
                          Clock clock) {
        this.locacaoRepository = locacaoRepository;
        this.clienteRepository = clienteRepository;
        this.equipamentoRepository = equipamentoRepository;
        this.locadoraRepository = locadoraRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
        this.clock = clock;
    }

    @Transactional
    public LocacaoResponse cadastrar(Long locadoraId, Long usuarioId, LocacaoCadastroRequest req) {
        if (!req.dataPrevista().isAfter(req.dataRetirada())) {
            throw new RegraDeNegocioException(
                    "A data de devolução prevista precisa ser depois da data de retirada.");
        }
        LocalDateTime agora = LocalDateTime.now(clock);

        // Cliente: precisa ser desta locadora e estar ativo
        Cliente cliente = clienteRepository.findByIdAndLocadoraId(req.clienteId(), locadoraId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado."));
        if (cliente.getStatus() != StatusCadastro.ATIVO) {
            throw new RegraDeNegocioException(
                    "Este cliente está inativo. Reative o cadastro antes de criar uma locação.");
        }

        // Equipamentos: sem repetidos, ordenados e com bloqueio pessimista
        List<Long> ids = req.equipamentosIds().stream().distinct().sorted().toList();
        List<Equipamento> equipamentos = equipamentoRepository.buscarParaLocacao(ids, locadoraId);
        if (equipamentos.size() != ids.size()) {
            throw new RecursoNaoEncontradoException(
                    "Um ou mais equipamentos escolhidos não foram encontrados.");
        }

        // Código sequencial por locadora (UPDATE atômico, sem pular nem repetir)
        locadoraRepository.incrementarUltimoCodigoLocacao(locadoraId);
        Long codigo = locadoraRepository.buscarUltimoCodigoLocacao(locadoraId);

        BigDecimal desconto = req.valorDesconto() == null ? BigDecimal.ZERO : req.valorDesconto();
        Endereco endereco = req.enderecoEntrega() == null ? null : req.enderecoEntrega().paraEntidade();

        Locacao locacao = Locacao.abrir(
                locadoraRepository.getReferenceById(locadoraId),
                codigo,
                cliente,
                usuarioRepository.getReferenceById(usuarioId),
                new HashSet<>(equipamentos),
                req.dataRetirada().atStartOfDay(),
                req.dataPrevista().atStartOfDay(),
                req.tipoPagamento(),
                desconto,
                req.responsavelRetirada(),
                req.documentoResponsavelRetirada(),
                endereco,
                agora);

        Locacao salva = locacaoRepository.save(locacao);
        boolean retroativa = req.dataRetirada().isBefore(agora.toLocalDate());
        String acao = retroativa ? "CADASTRO_RETROATIVO" : "CADASTRO";
        String textoStatus = salva.getStatus().name();
        String detalhes = String.format(
                "%s da locação nº %d para %s (retirada: %s, prevista: %s, status inicial: %s).",
                retroativa ? "Cadastro retroativo" : "Cadastro",
                salva.getCodigoSequencial(),
                cliente.getNome(),
                req.dataRetirada().format(DATA_BR),
                req.dataPrevista().format(DATA_BR),
                textoStatus);
        auditoriaService.registrar(locadoraId, "LOCACAO", acao, retroativa, detalhes);

        return LocacaoResponse.de(salva, agora);
    }

    // Não é readOnly porque pode gravar a mudança para ATRASADA
    @Transactional
    public LocacaoResponse consultar(Long locadoraId, Long id) {
        LocalDateTime agora = LocalDateTime.now(clock);
        Locacao locacao = locacaoRepository.findByIdAndLocadoraId(id, locadoraId)
                .orElseThrow(this::naoEncontrada);

        locacao.sincronizarAtraso(agora);
        return LocacaoResponse.de(locacao, agora);
    }

    @Transactional
    public LocacaoResponse devolver(Long locadoraId, Long id, LocacaoDevolucaoRequest req) {
        LocalDateTime agora = LocalDateTime.now(clock);
        Locacao locacao = locacaoRepository.buscarParaAtualizacao(id, locadoraId)
                .orElseThrow(this::naoEncontrada);

        LocalDateTime dataDevolucao = (req == null || req.dataDevolucao() == null)
                ? agora : req.dataDevolucao().atStartOfDay();

        locacao.sincronizarAtraso(agora);
        locacao.devolver(dataDevolucao, agora);
        auditoriaService.registrar(
                locadoraId,
                "LOCACAO",
                "DEVOLUCAO",
                false,
                String.format("Devolução da locação nº %d (%s) em %s. Multa: R$ %.2f · Total: R$ %.2f.",
                        locacao.getCodigoSequencial(),
                        locacao.getCliente().getNome(),
                        dataDevolucao.toLocalDate().format(DATA_BR),
                        locacao.getValorMultaPorAtraso(),
                        locacao.getValorFinalCobrado()));
        return LocacaoResponse.de(locacao, agora);
    }

    @Transactional
    public LocacaoResponse atualizar(Long locadoraId, Long id, LocacaoAtualizacaoRequest req) {
        LocalDateTime agora = LocalDateTime.now(clock);
        Locacao locacao = locacaoRepository.buscarParaAtualizacao(id, locadoraId)
                .orElseThrow(this::naoEncontrada);

        // Se já passou da data prevista, vira ATRASADA aqui e a edição é recusada (só EM_ANDAMENTO edita)
        locacao.sincronizarAtraso(agora);

        Endereco endereco = req.enderecoEntrega() == null ? null : req.enderecoEntrega().paraEntidade();
        locacao.atualizar(
                req.dataPrevista() == null ? null : req.dataPrevista().atStartOfDay(),
                req.valorDesconto(),
                req.responsavelRetirada(),
                req.documentoResponsavelRetirada(),
                endereco,
                agora);
        auditoriaService.registrar(
                locadoraId,
                "LOCACAO",
                "ATUALIZACAO",
                false,
                String.format("Atualização da locação nº %d (%s). Nova previsão: %s.",
                        locacao.getCodigoSequencial(),
                        locacao.getCliente().getNome(),
                        locacao.getDataPrevista().toLocalDate().format(DATA_BR)));
        return LocacaoResponse.de(locacao, agora);
    }

    @Transactional
    public LocacaoResponse cancelar(Long locadoraId, Long id) {
        LocalDateTime agora = LocalDateTime.now(clock);
        Locacao locacao = locacaoRepository.buscarParaAtualizacao(id, locadoraId)
                .orElseThrow(this::naoEncontrada);

        locacao.sincronizarAtraso(agora);
        locacao.cancelar(agora);
        auditoriaService.registrar(
                locadoraId,
                "LOCACAO",
                "CANCELAMENTO",
                false,
                String.format("Cancelamento da locação nº %d (%s).",
                        locacao.getCodigoSequencial(),
                        locacao.getCliente().getNome()));
        return LocacaoResponse.de(locacao, agora);
    }

    private RecursoNaoEncontradoException naoEncontrada() {
        return new RecursoNaoEncontradoException("Locação não encontrada.");
    }
}