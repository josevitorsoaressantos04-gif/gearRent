package com.example.gearrent.controllers;
import com.example.gearrent.DTO.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/contratos")
public class ContratoController {

    @GetMapping
    public ResponseEntity<List<ContratoResponse>> listarContratos() {
        return ResponseEntity.ok(Collections.emptyList());
    }

    @PostMapping
    public ResponseEntity<ContratoResponse> criarContrato(@RequestBody ContratoRequest request) {
        return ResponseEntity.ok(new ContratoResponse(1L, "Contrato gerado com sucesso"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ContratoResponse> cancelarContrato(@PathVariable Long id) {
        // TODO: Chamar o ContratoService para cancelar o contrato e liberar o equipamento no estoque
        return ResponseEntity.ok(new ContratoResponse(id, "Contrato cancelado com sucesso"));
    }
}