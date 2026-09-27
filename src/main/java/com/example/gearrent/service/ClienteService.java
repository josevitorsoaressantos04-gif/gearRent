package com.example.gearrent.service;

import com.example.gearrent.DTO.ClienteRequest;
import com.example.gearrent.DTO.ClienteResponse;
import com.example.gearrent.DTO.MensagemResponse;
import com.example.gearrent.entities.Cliente;
import com.example.gearrent.exception.RegraNegocioException;
import com.example.gearrent.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listarTodos() {
        return clienteRepository.findAll().stream()
                .map(this::mapearParaResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorId(Long id) {
        Cliente cliente = buscarEntidadePorId(id);
        return mapearParaResponse(cliente);
    }

    @Transactional
    public MensagemResponse criarCliente(ClienteRequest request) {
        if (clienteRepository.existsByCpf(request.cpf())) {
            throw new RegraNegocioException("O CPF informado já está cadastrado.");
        }
        if (clienteRepository.existsByEmail(request.email())) {
            throw new RegraNegocioException("O e-mail informado já está em uso.");
        }

        Cliente cliente = new Cliente();
        cliente.setNome(request.nome());
        cliente.setCpf(request.cpf());
        cliente.setEmail(request.email());
        cliente.setTelefone(request.telefone());
        cliente.setAtivo(true);

        clienteRepository.save(cliente);
        return new MensagemResponse(cliente.getId(), "Cliente cadastrado com sucesso.");
    }

    @Transactional
    public MensagemResponse atualizarCliente(Long id, ClienteRequest request) {
        Cliente cliente = buscarEntidadePorId(id);

        if (!cliente.getCpf().equals(request.cpf()) && clienteRepository.existsByCpf(request.cpf())) {
            throw new RegraNegocioException("O CPF informado já está cadastrado em outra conta.");
        }
        if (!cliente.getEmail().equalsIgnoreCase(request.email()) && clienteRepository.existsByEmail(request.email())) {
            throw new RegraNegocioException("O e-mail informado já está em uso.");
        }

        cliente.setNome(request.nome());
        cliente.setCpf(request.cpf());
        cliente.setEmail(request.email());
        cliente.setTelefone(request.telefone());

        clienteRepository.save(cliente);
        return new MensagemResponse(cliente.getId(), "Cliente atualizado com sucesso.");
    }

    @Transactional
    public void inativarCliente(Long id) {
        Cliente cliente = buscarEntidadePorId(id);

        if (Boolean.FALSE.equals(cliente.getAtivo())) {
            throw new RegraNegocioException("Este cliente já se encontra desativado.");
        }

        cliente.setAtivo(false);
        clienteRepository.save(cliente);
    }

    private Cliente buscarEntidadePorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Cliente não encontrado com o ID: " + id));
    }

    private ClienteResponse mapearParaResponse(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNome(),
                cliente.getCpf(),
                cliente.getEmail(),
                cliente.getTelefone(),
                cliente.getAtivo()
        );
    }
}