package com.example.gearrent.presentation;

import com.example.gearrent.application.DTO.UsuarioAtualizacaoRequest;
import com.example.gearrent.application.DTO.UsuarioAtualizacaoResponse;
import com.example.gearrent.application.DTO.UsuarioCadastroRequest;
import com.example.gearrent.application.DTO.UsuarioCadastroResponse;
import com.example.gearrent.application.DTO.UsuarioConsultaResponse;
import com.example.gearrent.application.DTO.UsuarioStatusResponse;
import com.example.gearrent.application.services.UsuarioService;
import com.example.gearrent.infra.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // Rota pública: cria o usuário e a base dele
    @PostMapping("/cadastro")
    public ResponseEntity<UsuarioCadastroResponse> cadastrar(
            @Valid @RequestBody UsuarioCadastroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(usuarioService.cadastroDeUsuarioInicial(request));
    }

    @GetMapping
    public ResponseEntity<List<UsuarioConsultaResponse>> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false, defaultValue = "") String nome) {
        return ResponseEntity.ok(usuarioService.listarUsuariosDaMinhaBase(usuario.locadoraId(), nome));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioAtualizacaoResponse> atualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody UsuarioAtualizacaoRequest request) {
        return ResponseEntity.ok(usuarioService.atualizar(usuario.locadoraId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<UsuarioStatusResponse> excluir(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.deletar(usuario.locadoraId(), id));
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<UsuarioStatusResponse> desativar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.desativar(usuario.locadoraId(), id));
    }
}