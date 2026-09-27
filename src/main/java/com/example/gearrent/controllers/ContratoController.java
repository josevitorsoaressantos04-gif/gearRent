package com.example.gearrent.controllers;

import com.example.gearrent.DTO.ContratoPutRequest;
import com.example.gearrent.DTO.ContratoRequest;
import com.example.gearrent.DTO.ContratoResponse;
import com.example.gearrent.DTO.MensagemResponse;
import com.example.gearrent.service.ContratoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RestController
@RequestMapping("/contratos")
public class ContratoController {

    private final ContratoService contratoService;

    public ContratoController(ContratoService contratoService) {
        this.contratoService = contratoService;
    }

    @GetMapping
    public ResponseEntity<List<ContratoResponse>> listarContratos() {
        return ResponseEntity.ok(contratoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContratoResponse> buscarContratoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(contratoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<MensagemResponse> criarContrato(@Valid @RequestBody ContratoRequest request) {
        MensagemResponse response = contratoService.criarContrato(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MensagemResponse> atualizarContrato(
            @PathVariable Long id,
            @Valid @RequestBody ContratoPutRequest request) {
        MensagemResponse response = contratoService.atualizarContrato(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MensagemResponse> cancelarContrato(@PathVariable Long id) {
        contratoService.deleteLogico(id);
        return ResponseEntity.ok(new MensagemResponse(id, "Contrato cancelado com sucesso."));
    }
}