package com.example.gearrent.controllers;
import com.example.gearrent.DTO.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    @GetMapping
    public ResponseEntity<List<ClienteResponse>> listarClientes() {
        return ResponseEntity.ok(Collections.emptyList());
    }

    @PostMapping
    public ResponseEntity<ClienteResponse> criarCliente(@RequestBody ClienteRequest request) {
        // TODO: Repassar 'request' para o ClienteService realizar a criação real
        return ResponseEntity.ok(new ClienteResponse(1L, "Cliente criado com sucesso"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponse> atualizarCliente(@PathVariable Long id, @RequestBody ClienteRequest request) {
        return ResponseEntity.ok(new ClienteResponse(id, "Cliente atualizado com sucesso"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ClienteResponse> excluirCliente(@PathVariable Long id) {
        return ResponseEntity.ok(new ClienteResponse(id, "Cliente desativado com sucesso"));
    }
}