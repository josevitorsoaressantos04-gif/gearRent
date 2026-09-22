package com.example.gearrent.controllers;

import com.example.gearrent.DTO.*;
import com.example.gearrent.entities.Empresa;
import com.example.gearrent.repository.EmpresaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/empresas")
public class EmpresaController {
    @Autowired
    private EmpresaRepository empresaRepository;

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


    @PutMapping("/{id}/atualizar")
    public ResponseEntity<EmpresaResponse> atualizarDadosEmpresa(@PathVariable String cnpj, @RequestBody EmpresaRequest request) {
        return empresaRepository.findByCnpj(cnpj).map(empresa ->  {
            empresa.setNome(request.nome());
            empresa.setCnpj(request.cnpj());
            empresa.setEmail(request.email());
            empresa.setTelefone(request.telefone());
            empresaRepository.save(empresa);
        });
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<EmpresaResponse> excluirEmpresa(@PathVariable Long id) {
        // TODO: Chamar a EmpresaService para alterar o status para inativo
        return ResponseEntity.ok(new EmpresaResponse(id, "Empresa inativada com sucesso"));
    }

}