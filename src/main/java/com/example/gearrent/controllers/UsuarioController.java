package com.example.gearrent.controllers;

import com.example.gearrent.DTO.MensagemResponse;
import com.example.gearrent.DTO.UsuarioConsultaResponse;
import com.example.gearrent.DTO.UsuarioRequest;
import com.example.gearrent.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://localhost:63342"})
@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // --- CONSULTAS (GET) ---

    @GetMapping
    public ResponseEntity<List<UsuarioConsultaResponse>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioConsultaResponse> buscarUsuarioPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @GetMapping("/buscar/nome")
    public ResponseEntity<List<UsuarioConsultaResponse>> buscarPorNome(@RequestParam String nome) {
        return ResponseEntity.ok(usuarioService.buscarPorNome(nome));
    }

    @GetMapping("/buscar/cpf/{cpf}")
    public ResponseEntity<UsuarioConsultaResponse> buscarPorCpf(@PathVariable String cpf) {
        return ResponseEntity.ok(usuarioService.buscarPorCpf(cpf));
    }

    @GetMapping("/buscar/login/{login}")
    public ResponseEntity<UsuarioConsultaResponse> buscarPorLogin(@PathVariable String login) {
        return ResponseEntity.ok(usuarioService.buscarPorLogin(login));
    }

    @GetMapping("/buscar/email")
    public ResponseEntity<UsuarioConsultaResponse> buscarPorEmail(@RequestParam String email) {
        return ResponseEntity.ok(usuarioService.buscarPorEmail(email));
    }

    @GetMapping("/buscar/data-cadastro")
    public ResponseEntity<List<UsuarioConsultaResponse>> buscarPorDataCadastro(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        return ResponseEntity.ok(usuarioService.buscarPorIntervaloDataCadastro(inicio, fim));
    }

    // --- OPERAÇÕES DE MUTAÇÃO (POST, PUT, DELETE) ---

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