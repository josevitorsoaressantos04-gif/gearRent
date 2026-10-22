package com.example.gearrent.presentation;

import com.example.gearrent.application.DTO.*;
import com.example.gearrent.application.services.UsuarioService;
import com.example.gearrent.domain.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuario")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/cadastro")
    public ResponseEntity<UsuarioCadastroResponse> cadastroUsuario(
            @Valid @RequestBody UsuarioCadastroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(usuarioService.cadastroDeUsuarioInicial(request));
    }

    @PutMapping("/{idBase}/{idUsuario}")
    public ResponseEntity<UsuarioAtualizacaoResponse> atualizarUsuario(
            @PathVariable Long idBase,
            @PathVariable Long idUsuario,
            @Valid @RequestBody UsuarioAtualizacaoRequest request) {
        return ResponseEntity.ok(usuarioService.atualizar(idBase, idUsuario, request));
    }

    @DeleteMapping("/{idBase}/{idUsuario}")
    public ResponseEntity<UsuarioStatusResponse> deletarUsuario(
            @PathVariable Long idBase,
            @PathVariable Long idUsuario) {
        return ResponseEntity.ok(usuarioService.deletar(idBase, idUsuario));
    }

    @PatchMapping("/{idBase}/{idUsuario}/desativar")
    public ResponseEntity<UsuarioStatusResponse> desativarUsuario(
            @PathVariable Long idBase,
            @PathVariable Long idUsuario) {
        return ResponseEntity.ok(usuarioService.desativar(idBase, idUsuario));
    }

    @GetMapping("/{idBase}/{idUsuario}")
    public ResponseEntity<List<UsuarioConsultaResponse>> listarUsuariosDaMinhaBase(
            @PathVariable Long idBase,
            @PathVariable Long idUsuario) {
        return ResponseEntity.ok(usuarioService.listarUsuariosDaMinhaBase(idUsuario, idBase));
    }
}