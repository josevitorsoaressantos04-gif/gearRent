package com.example.gearrent.application.service;

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
import java.util.HashSet;
import java.util.List;

@Service
public class LocacaoService {

    private final LocacaoRepository locacaoRepository;
    private final ClienteRepository clienteRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final LocadoraRepository locadoraRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    public LocacaoService(LocacaoRepository locacaoRepository,
                          ClienteRepository clienteRepository,
                          EquipamentoRepository equipamentoRepository,
                          LocadoraRepository locadoraRepository,
                          UsuarioRepository usuarioRepository,
                          Clock clock) {
        this.locacaoRepository = locacaoRepository;
        this.clienteRepository = clienteRepository;
        this.equipamentoRepository = equipamentoRepository;
        this.locadoraRepository = locadoraRepository;
        this.usuarioRepository = usuarioRepository;
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

        return LocacaoResponse.de(locacaoRepository.save(locacao), agora);
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
        return LocacaoResponse.de(locacao, agora);
    }

    @Transactional
    public LocacaoResponse cancelar(Long locadoraId, Long id) {
        LocalDateTime agora = LocalDateTime.now(clock);
        Locacao locacao = locacaoRepository.buscarParaAtualizacao(id, locadoraId)
                .orElseThrow(this::naoEncontrada);

        locacao.sincronizarAtraso(agora);
        locacao.cancelar(agora);
        return LocacaoResponse.de(locacao, agora);
    }

    private RecursoNaoEncontradoException naoEncontrada() {
        return new RecursoNaoEncontradoException("Locação não encontrada.");
    }
}