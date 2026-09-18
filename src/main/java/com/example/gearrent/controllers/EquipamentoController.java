package com.example.gearrent.controllers;
import com.example.gearrent.DTO.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/equipamentos")
public class EquipamentoController {

    @GetMapping
    public ResponseEntity<List<EquipamentoResponse>> listarEquipamentos() {
        return ResponseEntity.ok(Collections.emptyList());
    }

    @PostMapping
    public ResponseEntity<EquipamentoResponse> criarEquipamento(@RequestBody EquipamentoRequest request) {
        return ResponseEntity.ok(new EquipamentoResponse(1L, "Equipamento cadastrado"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EquipamentoResponse> atualizarEquipamento(@PathVariable Long id, @RequestBody EquipamentoRequest request) {
        return ResponseEntity.ok(new EquipamentoResponse(id, "Equipamento atualizado"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<EquipamentoResponse> excluirEquipamento(@PathVariable Long id) {
        // TODO: Chamar o EquipamentoService.js para inativar o registro na base de dados
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