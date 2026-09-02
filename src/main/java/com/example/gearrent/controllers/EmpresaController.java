package com.example.gearrent.controllers;
import com.example.gearrent.DTO.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/empresas")
public class EmpresaController {

    @GetMapping
    public ResponseEntity<List<EmpresaResponse>> listarEmpresas() {
        return ResponseEntity.ok(Collections.emptyList());
    }

    @PostMapping
    public ResponseEntity<EmpresaResponse> criarEmpresa(@RequestBody EmpresaRequest request) {
        return ResponseEntity.ok(new EmpresaResponse(1L, "Empresa registrada"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<EmpresaResponse> atualizarStatusEmpresa(
            @PathVariable Long id,
            @RequestBody AtualizarStatusEmpresaRequest request) {
        // TODO: Delegar para EmpresaService alterar o status ativo/inativo
        return ResponseEntity.ok(new EmpresaResponse(id, "Status da empresa atualizado com sucesso"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<EmpresaResponse> excluirEmpresa(@PathVariable Long id) {
        // TODO: Chamar a EmpresaService para alterar o status para inativo
        return ResponseEntity.ok(new EmpresaResponse(id, "Empresa inativada com sucesso"));
    }

}