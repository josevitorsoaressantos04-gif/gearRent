package com.example.gearrent.presentation;

import com.example.gearrent.application.DTO.EquipamentoAtualizacaoRequest;
import com.example.gearrent.application.DTO.EquipamentoAtualizacaoResponse;
import com.example.gearrent.application.DTO.EquipamentoCadastroRequest;
import com.example.gearrent.application.DTO.EquipamentoCadastroResponse;
import com.example.gearrent.application.DTO.EquipamentoConsultaResponse;
import com.example.gearrent.application.DTO.EquipamentoDisponivelResponse;
import com.example.gearrent.application.DTO.EquipamentoExclusaoResponse;
import com.example.gearrent.application.DTO.EquipamentoListagemResponse;
import com.example.gearrent.application.services.EquipamentoService;
import com.example.gearrent.infra.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipamentos")
public class EquipamentoController {

    private final EquipamentoService equipamentoService;

    public EquipamentoController(EquipamentoService equipamentoService) {
        this.equipamentoService = equipamentoService;
    }

    @PostMapping
    public ResponseEntity<EquipamentoCadastroResponse> cadastrar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody EquipamentoCadastroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(equipamentoService.cadastrar(usuario.locadoraId(), request));
    }

    @GetMapping
    public ResponseEntity<List<EquipamentoListagemResponse>> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false, defaultValue = "") String nome) {
        return ResponseEntity.ok(equipamentoService.listar(usuario.locadoraId(), nome));
    }

    @GetMapping("/disponiveis")
    public ResponseEntity<List<EquipamentoDisponivelResponse>> listarDisponiveis(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false, defaultValue = "") String nome) {
        return ResponseEntity.ok(equipamentoService.listarDisponiveis(usuario.locadoraId(), nome));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipamentoConsultaResponse> buscarPorId(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        return ResponseEntity.ok(equipamentoService.buscarPorId(usuario.locadoraId(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EquipamentoAtualizacaoResponse> atualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody EquipamentoAtualizacaoRequest request) {
        return ResponseEntity.ok(equipamentoService.atualizar(usuario.locadoraId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<EquipamentoExclusaoResponse> excluir(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        return ResponseEntity.ok(equipamentoService.excluir(usuario.locadoraId(), id));
    }
}