package com.example.gearrent.presentation;

import com.example.gearrent.application.DTO.ClienteAtualizacaoRequest;
import com.example.gearrent.application.DTO.ClienteAtualizacaoResponse;
import com.example.gearrent.application.DTO.ClienteCadastroRequest;
import com.example.gearrent.application.DTO.ClienteCadastroResponse;
import com.example.gearrent.application.DTO.ClienteConsultaResponse;
import com.example.gearrent.application.DTO.ClienteExclusaoResponse;
import com.example.gearrent.application.DTO.ClienteListagemResponse;
import com.example.gearrent.application.services.ClienteService;
import com.example.gearrent.infra.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    public ResponseEntity<ClienteCadastroResponse> cadastrar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ClienteCadastroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clienteService.cadastrar(usuario.locadoraId(), request));
    }

    @GetMapping
    public ResponseEntity<List<ClienteListagemResponse>> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false, defaultValue = "") String nome) {
        return ResponseEntity.ok(clienteService.listar(usuario.locadoraId(), nome));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteConsultaResponse> buscarPorId(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        return ResponseEntity.ok(clienteService.buscarPorId(usuario.locadoraId(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteAtualizacaoResponse> atualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody ClienteAtualizacaoRequest request) {
        return ResponseEntity.ok(clienteService.atualizar(usuario.locadoraId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ClienteExclusaoResponse> excluir(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        return ResponseEntity.ok(clienteService.excluir(usuario.locadoraId(), id));
    }
}