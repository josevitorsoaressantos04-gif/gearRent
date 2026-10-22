package com.example.gearrent.application.services;

import com.example.gearrent.application.DTO.ClienteAtualizacaoRequest;
import com.example.gearrent.application.DTO.ClienteAtualizacaoResponse;
import com.example.gearrent.application.DTO.ClienteCadastroRequest;
import com.example.gearrent.application.DTO.ClienteCadastroResponse;
import com.example.gearrent.application.DTO.ClienteConsultaResponse;
import com.example.gearrent.application.DTO.ClienteExclusaoResponse;
import com.example.gearrent.application.DTO.ClienteListagemResponse;
import com.example.gearrent.domain.entities.Cliente;
import com.example.gearrent.domain.enums.StatusCadastro;
import com.example.gearrent.domain.enums.StatusLocacao;
import com.example.gearrent.domain.exceptions.DocumentoDuplicadoException;
import com.example.gearrent.domain.exceptions.RecursoNaoEncontradoException;
import com.example.gearrent.domain.exceptions.RegraDeNegocioException;
import com.example.gearrent.domain.repository.ClienteRepository;
import com.example.gearrent.domain.repository.LocacaoRepository;
import com.example.gearrent.domain.repository.LocadoraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final LocadoraRepository locadoraRepository;
    private final LocacaoRepository locacaoRepository;

    public ClienteService(ClienteRepository clienteRepository,
                          LocadoraRepository locadoraRepository,
                          LocacaoRepository locacaoRepository) {
        this.clienteRepository = clienteRepository;
        this.locadoraRepository = locadoraRepository;
        this.locacaoRepository = locacaoRepository;
    }

    @Transactional
    public ClienteCadastroResponse cadastrar(Long locadoraId, ClienteCadastroRequest request) {
        String documento = request.documento().replaceAll("\\D", "");

        if (clienteRepository.existsByLocadoraIdAndDocumento(locadoraId, documento)) {
            throw new DocumentoDuplicadoException(
                    "Já existe um cliente cadastrado com este CPF ou CNPJ.");
        }

        Cliente cliente = new Cliente();
        cliente.setNome(request.nome().trim());
        cliente.setDocumento(documento);
        cliente.setEmail(limpar(request.email()));
        cliente.setCelular(limpar(request.celular()));
        cliente.setTelefone(limpar(request.telefone()));
        cliente.setEnderecoResidencial(request.endereco() == null ? null : request.endereco().paraEntidade());
        cliente.setStatus(StatusCadastro.ATIVO);
        cliente.setDataCadastro(LocalDateTime.now());
        cliente.setLocadora(locadoraRepository.getReferenceById(locadoraId));

        Cliente salvo = clienteRepository.save(cliente);

        return new ClienteCadastroResponse(salvo.getId(), salvo.getNome(), "Cliente cadastrado com sucesso!");
    }

    @Transactional(readOnly = true)
    public List<ClienteListagemResponse> listar(Long locadoraId, String nome) {
        String termo = nome == null ? "" : nome.trim();
        return clienteRepository
                .findByLocadoraIdAndStatusNotAndNomeContainingIgnoreCaseOrderByNomeAsc(
                        locadoraId, StatusCadastro.CANCELADO, termo)
                .stream()
                .map(ClienteListagemResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClienteConsultaResponse buscarPorId(Long locadoraId, Long id) {
        return ClienteConsultaResponse.de(buscarEntidade(locadoraId, id));
    }

    @Transactional
    public ClienteAtualizacaoResponse atualizar(Long locadoraId, Long id, ClienteAtualizacaoRequest request) {
        Cliente cliente = buscarEntidade(locadoraId, id);

        cliente.setNome(request.nome().trim());
        cliente.setEmail(limpar(request.email()));
        cliente.setCelular(limpar(request.celular()));
        cliente.setTelefone(limpar(request.telefone()));
        cliente.setEnderecoResidencial(request.endereco() == null ? null : request.endereco().paraEntidade());
        cliente.setDataAtualizacao(LocalDateTime.now());

        return new ClienteAtualizacaoResponse(cliente.getId(), cliente.getNome(), "Cliente atualizado com sucesso!");
    }

    @Transactional
    public ClienteExclusaoResponse excluir(Long locadoraId, Long id) {
        Cliente cliente = buscarEntidade(locadoraId, id);

        boolean temLocacaoAberta = locacaoRepository.existsByClienteIdAndLocadoraIdAndStatusIn(
                id, locadoraId, List.of(StatusLocacao.EM_ANDAMENTO, StatusLocacao.ATRASADA));
        if (temLocacaoAberta) {
            throw new RegraDeNegocioException(
                    "Este cliente possui uma locação em andamento. Registre a devolução antes de excluí-lo.");
        }

        cliente.setStatus(StatusCadastro.CANCELADO);
        cliente.setDataAtualizacao(LocalDateTime.now());

        return new ClienteExclusaoResponse(cliente.getId(), "Cliente excluído com sucesso!");
    }

    private Cliente buscarEntidade(Long locadoraId, Long id) {
        return clienteRepository.findByIdAndLocadoraId(id, locadoraId)
                .filter(c -> c.getStatus() != StatusCadastro.CANCELADO)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Não encontramos este cliente. Confira os dados e tente novamente."));
    }

    private String limpar(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}