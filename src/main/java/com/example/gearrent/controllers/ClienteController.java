package com.example.gearrent.controllers;

import com.example.gearrent.DTO.ClienteRequest;
import com.example.gearrent.DTO.ClienteResponse;
import com.example.gearrent.DTO.MensagemResponse;
import com.example.gearrent.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RestController
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public ResponseEntity<List<ClienteResponse>> listarClientes() {
        return ResponseEntity.ok(clienteService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> buscarClientePorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<MensagemResponse> criarCliente(@Valid @RequestBody ClienteRequest request) {
        MensagemResponse response = clienteService.criarCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MensagemResponse> atualizarCliente(
            @PathVariable Long id,
            @Valid @RequestBody ClienteRequest request) {
        MensagemResponse response = clienteService.atualizarCliente(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MensagemResponse> excluirCliente(@PathVariable Long id) {
        clienteService.inativarCliente(id);
        return ResponseEntity.ok(new MensagemResponse(id, "Cliente desativado com sucesso."));
    }
}