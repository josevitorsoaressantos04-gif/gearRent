package com.example.gearrent.controllers;

import com.example.gearrent.DTO.*;
import com.example.gearrent.entities.Cliente;
import com.example.gearrent.repository.ClienteRepository;
import com.example.gearrent.repository.EnderecoRepository;
import com.example.gearrent.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/clientes")
public class ClienteController {
    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ClienteService clienteService;

    @GetMapping
    public ResponseEntity<List<Cliente>> listarClientes() {
        return ResponseEntity.ok(clienteRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<ClienteResponse> criarCliente(@RequestBody ClienteRequest request) {
        // TODO: Repassar 'request' para o ClienteService realizar a criação real
        Cliente cliente = new Cliente();
        cliente.setNome(request.nome());
        cliente.setEmail(request.email());
        cliente.setCpf(request.cpf());
        cliente.setAtivo(true);
        cliente.setDataAtualizacao(LocalDateTime.now());
        clienteRepository.save(cliente);
        return ResponseEntity.ok(new ClienteResponse(cliente.getId(), "Cliente criado com sucesso"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponse> atualizarCliente(@PathVariable Long id, @RequestBody ClienteRequest request) {
        return clienteRepository.findById(id)
                .map(cliente -> {
                    cliente.setNome(request.nome());
                    cliente.setEmail(request.email());
                    cliente.setCpf(request.cpf());
                    cliente.setDataAtualizacao(LocalDateTime.now());

                    clienteRepository.save(cliente);
                    return ResponseEntity.ok(new ClienteResponse(cliente.getId(), "Usuário atualizado com sucesso"));
                })
                .orElse(ResponseEntity.notFound().build()); // Se não encontrar o ID, já manda o 404
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ClienteResponse> excluirCliente(@PathVariable Long id) {
        clienteService.deleteLogico(id);
        return ResponseEntity.ok(new ClienteResponse(id, "Cliente desativado com sucesso"));
    }
}