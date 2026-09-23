package com.example.gearrent.controllers;

import com.example.gearrent.DTO.*;
import com.example.gearrent.entities.Empresa;
import com.example.gearrent.repository.EmpresaRepository;
import com.example.gearrent.service.EmpresaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/empresas")
public class EmpresaController {
    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private EmpresaService empresaService;

    @GetMapping
    public ResponseEntity<List<Empresa>> listarEmpresas() {
        return ResponseEntity.ok(empresaRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<EmpresaResponse> criarEmpresa(@RequestBody EmpresaRequest request) {
        Empresa empresa = new Empresa();
        empresa.setNome(request.nome());
        empresa.setCnpj(request.cnpj());
        empresa.setEmail(request.email());
        empresa.setTelefone(request.telefone());
        empresa.setStatus(true);
        empresaRepository.save(empresa);

        return ResponseEntity.ok(new EmpresaResponse(empresa.getId(), "Empresa cadastrada com sucesso"));
    }


    @PutMapping("/{cnpj}/atualizar")
    public ResponseEntity<EmpresaResponse> atualizarDadosEmpresa(
            @PathVariable String cnpj,
            @RequestBody EmpresaRequest request) {

        // A Service devolve um Optional. O map() converte para 200 OK se existir, o orElse manda 404 se não achar.
        return empresaService.atualizarPorCnpj(cnpj, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<EmpresaResponse> excluirEmpresa(@PathVariable Long id) {
        empresaService.deleteLogico(id);
        return ResponseEntity.ok(new EmpresaResponse(id, "Empresa inativada com sucesso"));
    }

}