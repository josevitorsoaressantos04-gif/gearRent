package com.example.gearrent.controllers;

import com.example.gearrent.DTO.EmpresaPutRequest;
import com.example.gearrent.DTO.EmpresaRequest;
import com.example.gearrent.DTO.EmpresaResponse;
import com.example.gearrent.DTO.MensagemResponse;
import com.example.gearrent.entities.Empresa;
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

    // Injeção de dependência via construtor
    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @GetMapping
    public ResponseEntity<List<EmpresaResponse>> listarEmpresas() {
        return ResponseEntity.ok(empresaService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmpresaResponse> buscarEmpresaPorId(@PathVariable Long id) {
        return ResponseEntity.ok(empresaService.buscarPorId(id));
    }

    @GetMapping("/cnpj/{cnpj}")
    public ResponseEntity<UsuarioConsultaResponse> buscarUsuarioEmpresaPorCpnj(@PathVariable String cnpj) {
        return ResponseEntity.ok(empresaService.buscarPorCnpj(cnpj));
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