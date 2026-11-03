package com.example.gearrent.application.services;

import com.example.gearrent.application.DTO.*;
import com.example.gearrent.domain.entities.Cliente;
import com.example.gearrent.domain.entities.Endereco;
import com.example.gearrent.domain.enums.StatusCadastro;
import com.example.gearrent.domain.enums.StatusLocacao;
import com.example.gearrent.domain.exceptions.DocumentoDuplicadoException;
import com.example.gearrent.domain.exceptions.RegraDeNegocioException;
import com.example.gearrent.domain.repository.ClienteRepository;
import com.example.gearrent.domain.repository.LocacaoRepository;
import com.example.gearrent.domain.repository.LocadoraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private static final Set<StatusLocacao> LOCACOES_EM_ABERTO =
            Set.of(StatusLocacao.EM_ANDAMENTO, StatusLocacao.ATRASADA);

    private final ClienteRepository clienteRepository;
    private final LocadoraRepository locadoraRepository;
    private final LocacaoRepository locacaoRepository;

    // ---------------------------------------------------------------
    // CADASTRAR (Estratégia 1: reativação automática)
    // ---------------------------------------------------------------
    @Transactional
    public ClienteCadastroResponse cadastrar(ClienteCadastroRequest request, Long locadoraId) {
        String documento = limparDocumento(request.documento());

        // Busca SEM olhar o status: ativo, inativo ou cancelado
        return clienteRepository.findByLocadoraIdAndDocumento(locadoraId, documento)
                .map(existente -> reativar(existente, request))
                .orElseGet(() -> criar(request, locadoraId, documento));
    }

    private ClienteCadastroResponse reativar(Cliente existente, ClienteCadastroRequest request) {
        if (existente.getStatus() == StatusCadastro.ATIVO) {
            throw new DocumentoDuplicadoException(
                    "Já existe um cliente ativo com este CPF ou CNPJ (cliente nº "
                            + existente.getCodigoSequencial() + ").");
        }
        // Mesmo id e mesmo codigoSequencial; só os dados e o status mudam
        preencherDados(existente, request.nome(), request.email(), request.celular(),
                request.telefone(), request.endereco());
        existente.setStatus(StatusCadastro.ATIVO);
        Cliente salvo = clienteRepository.save(existente);

        return new ClienteCadastroResponse(salvo.getId(), salvo.getCodigoSequencial(), salvo.getNome(),
                "O cadastro de " + salvo.getNome() + " já existia e foi reativado com os dados novos.");
    }

    private ClienteCadastroResponse criar(ClienteCadastroRequest request, Long locadoraId, String documento) {
        Cliente cliente = new Cliente();
        cliente.setDocumento(documento);
        cliente.setLocadora(locadoraRepository.getReferenceById(locadoraId));
        cliente.setStatus(StatusCadastro.ATIVO);
        preencherDados(cliente, request.nome(), request.email(), request.celular(),
                request.telefone(), request.endereco());

        // Número sequencial só para cliente NOVO, logo antes do save
        cliente.setCodigoSequencial(locadoraRepository.proximoCodigoCliente(locadoraId));

        Cliente salvo = clienteRepository.save(cliente);
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
    // ATUALIZAR (o documento não muda: o request não o recebe)
    // ---------------------------------------------------------------
    @Transactional
    public ClienteAtualizacaoResponse atualizar(Long locadoraId, Long id, ClienteAtualizacaoRequest request) {
        Cliente cliente = buscarVisivel(locadoraId, id);
        preencherDados(cliente, request.nome(), request.email(), request.celular(),
                request.telefone(), request.endereco());
        clienteRepository.save(cliente);

        return new ClienteAtualizacaoResponse(cliente.getId(), cliente.getNome(),
                "Dados de " + cliente.getNome() + " atualizados com sucesso!");
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
        return new ClienteExclusaoResponse(cliente.getId(),
                "Cliente " + cliente.getNome() + " removido. O histórico de locações foi preservado.");
    }

    // ---------------------------------------------------------------
    // Auxiliares
    // ---------------------------------------------------------------
    // Cliente da locadora logada que ainda não foi cancelado
    private Cliente buscarVisivel(Long locadoraId, Long id) {
        Cliente cliente = clienteRepository.findByIdAndLocadoraId(id, locadoraId)
                .orElseThrow(() -> new RegraDeNegocioException("Cliente não encontrado."));
        if (cliente.getStatus() == StatusCadastro.CANCELADO) {
            throw new RegraDeNegocioException("Cliente não encontrado.");
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

    // ⚠️ Ajuste aos nomes reais dos campos de Endereco/EnderecoDTO
    // (se o EnderecoDTO já tiver um método de conversão, use-o e apague este)
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