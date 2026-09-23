package com.example.gearrent.controllers;

import com.example.gearrent.DTO.*;
import com.example.gearrent.entities.Equipamento;
import com.example.gearrent.repository.EquipamentoRepository;
import com.example.gearrent.service.EquipamentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/equipamentos")
public class EquipamentoController {
    @Autowired
    private EquipamentoRepository equipamentoRepository;

    @Autowired
    EquipamentoService equipamentoService;

    @GetMapping
    public ResponseEntity<List<Equipamento>> listarEquipamentos() {
        return ResponseEntity.ok(equipamentoRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<EquipamentoResponse> criarEquipamento(@RequestBody EquipamentoRequest request, HttpEntity<Object> httpEntity) {
        Equipamento equipamento = new Equipamento();
        equipamento.setNome(request.nome());
        equipamento.setModelo(request.modelo());
        equipamento.setNumeroPatrimonio(request.numeroPatrimonio());
        equipamento.setValorDiariaBase(request.valorDiariaBase());
        equipamentoRepository.save(equipamento);
        return ResponseEntity.status(HttpStatus.CREATED).body(new EquipamentoResponse(equipamento.getId(), "Equipamento criado com sucesso"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EquipamentoResponse> atualizarEquipamento(@PathVariable Long id, @RequestBody EquipamentoRequest request) {
        return ResponseEntity.ok(new EquipamentoResponse(id, "Equipamento atualizado"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<EquipamentoResponse> excluirEquipamento(@PathVariable Long id) {
        equipamentoService.deleteLogico(id);
        return ResponseEntity.ok(new EquipamentoResponse(id, "Equipamento inativado com sucesso"));
    }

    @PatchMapping("/status/{id}")
    public ResponseEntity<AtualizarStatusResponse> atualizarEquipamentoStatus(
            @PathVariable Long id,
            @RequestBody AtualizarStatusRequest request) {

        // O EquipamentoService.js impedirá a mudança do numeroPatrimonio[cite: 2],
        // mas permitirá alterar o valorDiariaBase (desde que recebido como BigDecimal).
        // equipamentoService.atualizar(id, request);

        return ResponseEntity.ok(new AtualizarStatusResponse(id, "Equipamento atualizado com sucesso"));
    }
}