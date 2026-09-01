package com.example.gearrent.controllers;

import com.example.gearrent.entities.Empresa;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Collections;

@RestController
@RequestMapping("/empresas")
public class EmpresaController {

    @GetMapping
    public ResponseEntity<List<Empresa>> listarEmpresas() {
        return ResponseEntity.ok(Collections.emptyList());
    }

    @PostMapping
    public ResponseEntity<Empresa> criarEmpresa(@RequestBody Empresa empresa) {
        return ResponseEntity.ok(empresa);
    }

    @PutMapping
    public ResponseEntity<Empresa> atualizarEmpresa(@RequestBody Empresa empresa) {
        return ResponseEntity.ok(empresa);
    }

    @DeleteMapping
    public ResponseEntity<Void> deletarEmpresa(@RequestBody Empresa empresa) {
        return ResponseEntity.ok().build();
    }

}