package com.example.gearrent.controllers;

import com.example.gearrent.DTO.*;
import com.example.gearrent.service.EmpresaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RestController
@RequestMapping("/empresas")
public class EmpresaController {

    private final EmpresaService empresaService;

    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @GetMapping
    public ResponseEntity<List<EmpresaConsultaResponse>> listarEmpresas() {
        return ResponseEntity.ok(empresaService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmpresaConsultaResponse> buscarEmpresaPorId(@PathVariable Long id) {
        return ResponseEntity.ok(empresaService.buscarPorId(id));
    }

    @GetMapping("/{cnpj}")
    public ResponseEntity<EmpresaConsultaResponse> buscarEmpresaPorCnpj(@PathVariable String cnpj) {
        return ResponseEntity.ok(empresaService.buscarPorCnpjComUsuarios(cnpj));
    }


    @PostMapping
    public ResponseEntity<MensagemResponse> criarEmpresa(@Valid @RequestBody EmpresaRequest request) {
        MensagemResponse response = empresaService.criarEmpresa(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MensagemResponse> atualizarEmpresa(
            @PathVariable Long id,
            @Valid @RequestBody EmpresaPutRequest request) {
        MensagemResponse response = empresaService.atualizarEmpresa(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MensagemResponse> excluirEmpresa(@PathVariable Long id) {
        empresaService.inativarEmpresa(id);
        return ResponseEntity.ok(new MensagemResponse(id, "Empresa inativada com sucesso."));
    }
}