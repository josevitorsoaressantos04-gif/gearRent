package com.example.gearrent.controllers;

import com.example.gearrent.entities.Equipamento;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Collections;

@RestController
@RequestMapping("/equipamentos")
public class EquipamentoController {

    @GetMapping
    public ResponseEntity<List<Equipamento>> listarEquipamentos() {
        return ResponseEntity.ok(Collections.emptyList());
    }

    @PostMapping
    public ResponseEntity<Equipamento> criarEquipamento(@RequestBody Equipamento equipamento) {
        return ResponseEntity.ok(equipamento);
    }

    @PutMapping
    public ResponseEntity<Void> atualizarEquipamento(@RequestBody Equipamento equipamento) {
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deletarEquipamento(@RequestBody Equipamento equipamento) {
        return ResponseEntity.ok().build();
    }
}