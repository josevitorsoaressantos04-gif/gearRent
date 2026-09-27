package com.example.gearrent.controllers;

import com.example.gearrent.DTO.*;
import com.example.gearrent.service.EquipamentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/equipamentos")
public class EquipamentoController {

    private final EquipamentoService equipamentoService;

    public EquipamentoController(EquipamentoService equipamentoService) {
        this.equipamentoService = equipamentoService;
    }

    @GetMapping
    public ResponseEntity<List<EquipamentoResponse>> listarEquipamentos() {
        return ResponseEntity.ok(equipamentoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipamentoResponse> buscarEquipamentoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(equipamentoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<MensagemResponse> criarEquipamento(@Valid @RequestBody EquipamentoRequest request) {
        MensagemResponse response = equipamentoService.criarEquipamento(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MensagemResponse> atualizarEquipamento(
            @PathVariable Long id,
            @Valid @RequestBody EquipamentoPutRequest request) {
        MensagemResponse response = equipamentoService.atualizarEquipamento(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MensagemResponse> excluirEquipamento(@PathVariable Long id) {
        equipamentoService.inativarEquipamento(id);
        return ResponseEntity.ok(new MensagemResponse(id, "Equipamento inativado com sucesso."));
    }
}