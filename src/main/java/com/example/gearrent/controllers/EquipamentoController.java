package com.example.gearrent.controllers;

import com.example.gearrent.DTO.*;
import com.example.gearrent.entities.Equipamento;
import com.example.gearrent.repository.EquipamentoRepository;
import com.example.gearrent.service.EquipamentoService;
import jakarta.validation.Valid;
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
    public ResponseEntity<EquipamentoResponse> criarEquipamento(@RequestBody EquipamentoRequest request) {
        Equipamento equipamento = new Equipamento();
        equipamento.setNome(request.nome());
        equipamento.setModelo(request.modelo());
        equipamento.setNumeroPatrimonio(request.numeroPatrimonio());
        equipamento.setValorDiariaBase(request.valorDiariaBase());
        equipamentoRepository.save(equipamento);
        return ResponseEntity.status(HttpStatus.CREATED).body(new EquipamentoResponse(equipamento.getId(), "Equipamento criado com sucesso"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AtualizarStatusResponse> atualizarEquipamento(
            @PathVariable Long id,
            @Valid @RequestBody EquipamentoRequest request) {
        equipamentoService.atualizarEquipamento(id, request);
        return ResponseEntity.ok(new AtualizarStatusResponse(id, "Equipamento atualizado com sucesso."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<AtualizarStatusResponse> excluirEquipamento(@PathVariable Long id) {
        return equipamentoRepository.findById(id)
                .map(equipamento -> {
                    equipamentoService.deleteLogico(id);
                    return ResponseEntity.ok(new AtualizarStatusResponse(id, "Equipamento desativado com sucesso"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

}