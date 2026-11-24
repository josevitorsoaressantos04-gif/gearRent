package com.example.gearrent.application.service;

import com.example.gearrent.application.DTO.*;
import com.example.gearrent.domain.entities.Cliente;
import com.example.gearrent.domain.entities.Endereco;
import com.example.gearrent.domain.entities.Equipamento;
import com.example.gearrent.domain.enums.StatusCadastro;
import com.example.gearrent.domain.enums.StatusLocacao;
import com.example.gearrent.domain.enums.TipoPessoa;
import com.example.gearrent.domain.exception.DocumentoDuplicadoException;
import com.example.gearrent.domain.exception.RecursoNaoEncontradoException;
import com.example.gearrent.domain.exception.RegraDeNegocioException;
import com.example.gearrent.domain.repository.ClienteRepository;
import com.example.gearrent.domain.repository.LocacaoRepository;
import com.example.gearrent.domain.repository.LocadoraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private static final Set<StatusLocacao> LOCACOES_EM_ABERTO =
            Set.of(StatusLocacao.EM_ANDAMENTO, StatusLocacao.ATRASADA);

    private final ClienteRepository clienteRepository;
    private final LocadoraRepository locadoraRepository;
    private final LocacaoRepository locacaoRepository;
    private final AuditoriaService auditoriaService;

    // ---------------------------------------------------------------
    // CADASTRAR (Estratégia 1: reativação automática)
    // ---------------------------------------------------------------
    @Transactional
    public ClienteCadastroResponse cadastrar(ClienteCadastroRequest request, Long locadoraId) {
        String documento = limparDocumento(request.documento());

        // Busca SEM olhar o status (ATIVO, INATIVO ou CANCELADO) mas restrito À LOCADORA ATUAL
        return clienteRepository.findByLocadoraIdAndDocumento(locadoraId, documento)
                .map(existente -> reativar(existente, request))
                .orElseGet(() -> criar(request, locadoraId, documento));
    }

    private ClienteCadastroResponse reativar(Cliente existente, ClienteCadastroRequest request) {
        // Se já existir um cliente ATIVO na mesma locadora, barra com HTTP 409 Conflict
        if (existente.getStatus() == StatusCadastro.ATIVO) {
            throw new DocumentoDuplicadoException(
                    "Já existe um cliente ativo com este CPF ou CNPJ (cliente nº "
                            + existente.getCodigoSequencial() + ").");
        }

        // Se estiver CANCELADO/INATIVO na mesma locadora, reativa e atualiza os dados
        preencherDados(existente, request.nome(), request.email(), request.celular(),
                request.telefone(), request.endereco());
        existente.setStatus(StatusCadastro.ATIVO);
        existente.setTipoPessoa(TipoPessoa.deduzirDoDocumento(existente.getDocumento()));
        Cliente salvo = clienteRepository.save(existente);
        auditoriaService.registrar(
                salvo.getLocadora().getId(),
                "CLIENTE",
                "REATIVACAO",
                false,
                String.format("Cliente nº %d (%s) reativado.", salvo.getCodigoSequencial(), salvo.getNome()));

        return new ClienteCadastroResponse(salvo.getId(), salvo.getCodigoSequencial(), salvo.getNome(),
                "O cadastro de " + salvo.getNome() + " já existia e foi reativado com os dados novos.");
    }

    private ClienteCadastroResponse criar(ClienteCadastroRequest request, Long locadoraId, String documento) {
        Cliente cliente = new Cliente();
        cliente.setDocumento(documento);
        cliente.setTipoPessoa(TipoPessoa.deduzirDoDocumento(documento));
        cliente.setLocadora(locadoraRepository.getReferenceById(locadoraId));
        cliente.setStatus(StatusCadastro.ATIVO);
        preencherDados(cliente, request.nome(), request.email(), request.celular(),
                request.telefone(), request.endereco());

        // Número sequencial só para cliente NOVO, logo antes do save
        cliente.setCodigoSequencial(locadoraRepository.proximoCodigoCliente(locadoraId));

        Cliente salvo = clienteRepository.save(cliente);
        auditoriaService.registrar(
                locadoraId,
                "CLIENTE",
                "CADASTRO",
                false,
                String.format("Cliente nº %d (%s) cadastrado.", salvo.getCodigoSequencial(), salvo.getNome()));
        return new ClienteCadastroResponse(salvo.getId(), salvo.getCodigoSequencial(), salvo.getNome(),
                "Cliente " + salvo.getNome() + " cadastrado com sucesso!");
    }

    // ---------------------------------------------------------------
    // LISTAR / CONSULTAR
    // ---------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<ClienteListagemResponse> listar(Long locadoraId, String nome) {
        String filtro = nome == null ? "" : nome.trim();
        return clienteRepository
                .findByLocadoraIdAndStatusNotAndNomeContainingIgnoreCaseOrderByNome(
                        locadoraId, StatusCadastro.CANCELADO, filtro)
                .stream()
                .map(ClienteListagemResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClienteConsultaResponse consultar(Long locadoraId, Long id) {
        return ClienteConsultaResponse.de(buscarVisivel(locadoraId, id));
    }

    // ---------------------------------------------------------------
    // ATUALIZAR (tipo de pessoa e documento são opcionais: se vierem, são revalidados)
    // ---------------------------------------------------------------
    @Transactional
    public ClienteAtualizacaoResponse atualizar(Long locadoraId, Long id, ClienteAtualizacaoRequest request) {
        Cliente cliente = buscarVisivel(locadoraId, id);

        String documento = request.documento() == null
                ? cliente.getDocumento()
                : limparDocumento(request.documento());

        TipoPessoa tipo = request.tipoPessoa() != null
                ? request.tipoPessoa()
                : (request.documento() != null
                    ? TipoPessoa.deduzirDoDocumento(documento)
                    : (cliente.getTipoPessoa() != null
                        ? cliente.getTipoPessoa()
                        : TipoPessoa.deduzirDoDocumento(documento)));

        if (tipo == null || documento.length() != tipo.tamanhoDocumento()) {
            throw new RegraDeNegocioException(tipo == TipoPessoa.JURIDICA
                    ? "Para pessoa jurídica, informe um CNPJ com 14 números."
                    : "Para pessoa física, informe um CPF com 11 números.");
        }

        // Só consulta duplicidade se o documento realmente mudou (sempre dentro da locadora do token)
        if (!documento.equals(cliente.getDocumento())
                && clienteRepository.existsByLocadoraIdAndDocumentoAndIdNot(locadoraId, documento, id)) {
            throw new DocumentoDuplicadoException(
                    "Este CPF ou CNPJ já pertence a outro cliente cadastrado nesta locadora.");
        }

        cliente.setDocumento(documento);
        cliente.setTipoPessoa(tipo);
        preencherDados(cliente, request.nome(), request.email(), request.celular(),
                request.telefone(), request.endereco());
        clienteRepository.save(cliente);
        auditoriaService.registrar(
                locadoraId,
                "CLIENTE",
                "ATUALIZACAO",
                false,
                String.format("Dados do cliente nº %d (%s) atualizados.", cliente.getCodigoSequencial(), cliente.getNome()));

        return new ClienteAtualizacaoResponse(cliente.getId(), cliente.getNome(),
                "Dados de " + cliente.getNome() + " atualizados com sucesso!");
    }

    // ---------------------------------------------------------------
    // RESUMO E HISTÓRICO DE LOCAÇÕES DO CLIENTE
    // ---------------------------------------------------------------
    @Transactional(readOnly = true)
    public ClienteResumoDTO resumo(Long locadoraId, Long id) {
        buscarVisivel(locadoraId, id); // garante que o cliente é desta locadora

        long total = locacaoRepository.contarLocacoesDoCliente(id, locadoraId, StatusLocacao.CANCELADA);
        long emAndamento = locacaoRepository.contarLocacoesPorStatus(id, locadoraId, LOCACOES_EM_ABERTO);
        long equipamentos = locacaoRepository.contarEquipamentosEmPosse(id, locadoraId, LOCACOES_EM_ABERTO);
        BigDecimal emAberto = BigDecimal.valueOf(
                        locacaoRepository.somarValorEmAbertoDoCliente(id, locadoraId, LOCACOES_EM_ABERTO))
                .setScale(2, RoundingMode.HALF_UP);

        return new ClienteResumoDTO(total, emAndamento, equipamentos, emAberto);
    }

    @Transactional(readOnly = true)
    public List<ClienteHistoricoLocacaoDTO> historico(Long locadoraId, Long id) {
        buscarVisivel(locadoraId, id);

        return locacaoRepository.findByClienteIdAndLocadoraIdOrderByDataRetiradaDescIdDesc(id, locadoraId)
                .stream()
                .map(l -> new ClienteHistoricoLocacaoDTO(
                        l.getId(),
                        l.getCodigoSequencial(),
                        l.getEquipamentos().stream()
                                .sorted(Comparator.comparingInt(Equipamento::getCodigoSequencial))
                                .map(Equipamento::getNome)
                                .toList(),
                        l.getDataRetirada(),
                        l.getDataPrevista(),
                        l.getStatus()))
                .toList();
    }

    // ---------------------------------------------------------------
    // EXCLUIR (exclusão LÓGICA, bloqueada se houver locação em aberto)
    // ---------------------------------------------------------------
    @Transactional
    public ClienteExclusaoResponse excluir(Long locadoraId, Long id) {
        Cliente cliente = buscarVisivel(locadoraId, id);

        boolean temLocacaoEmAberto = locacaoRepository
                .existsByClienteIdAndLocadoraIdAndStatusIn(id, locadoraId, LOCACOES_EM_ABERTO);
        if (temLocacaoEmAberto) {
            throw new RegraDeNegocioException(
                    "Este cliente tem locações em andamento ou atrasadas. "
                            + "Finalize essas locações antes de remover o cadastro.");
        }

        cliente.setStatus(StatusCadastro.CANCELADO); // nada é apagado do banco
        auditoriaService.registrar(
                locadoraId,
                "CLIENTE",
                "INATIVACAO",
                false,
                String.format("Cliente nº %d (%s) inativado.", cliente.getCodigoSequencial(), cliente.getNome()));
        return new ClienteExclusaoResponse(cliente.getId(),
                "Cliente " + cliente.getNome() + " removido. O histórico de locações foi preservado.");
    }

    // ---------------------------------------------------------------
    // Auxiliares
    // ---------------------------------------------------------------
    private Cliente buscarVisivel(Long locadoraId, Long id) {
        Cliente cliente = clienteRepository.findByIdAndLocadoraId(id, locadoraId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado."));
        if (cliente.getStatus() == StatusCadastro.CANCELADO) {
            throw new RecursoNaoEncontradoException("Cliente não encontrado.");
        }
        return cliente;
    }

    private void preencherDados(Cliente c, String nome, String email, String celular,
                                String telefone, EnderecoDTO endereco) {
        c.setNome(nome);
        c.setEmail(email);
        c.setCelular(celular);
        c.setTelefone(telefone);
        if (endereco != null) {
            c.setEnderecoResidencial(paraEndereco(endereco));
        }
    }

    private Endereco paraEndereco(EnderecoDTO dto) {
        Endereco e = new Endereco();
        e.setCep(dto.cep());
        e.setLogradouro(dto.logradouro());
        e.setNumero(dto.numero());
        e.setComplemento(dto.complemento());
        e.setBairro(dto.bairro());
        e.setCidade(dto.cidade());
        e.setEstado(dto.estado());
        return e;
    }

    private String limparDocumento(String documento) {
        return documento == null ? null : documento.replaceAll("\\D", "");
    }
}