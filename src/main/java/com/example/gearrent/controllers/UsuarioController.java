package com.example.gearrent.controllers;

import com.example.gearrent.DTO.MensagemResponse;
import com.example.gearrent.DTO.UsuarioRequest;
import com.example.gearrent.DTO.UsuarioResponse;
import com.example.gearrent.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@CrossOrigin(origins = {"http://localhost:63342"})
@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> listarUsuariosPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<MensagemResponse> criarUsuario(@Valid @RequestBody UsuarioRequest usuarioRequest) {
        MensagemResponse response = usuarioService.criarUsuario(usuarioRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MensagemResponse> atualizarUsuario(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioRequest usuarioRequest) {
        MensagemResponse response = usuarioService.atualizarUsuario(id, usuarioRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MensagemResponse> excluirUsuario(@PathVariable Long id) {
        usuarioService.inativarUsuario(id);
        return ResponseEntity.ok(new MensagemResponse(id, "Usuário inativado com sucesso."));
    }
}