package com.example.gearrent.controllers;

import com.example.gearrent.entities.Contrato;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Collections;

@RestController
@RequestMapping("/contratos")
public class ContratoController {

    @GetMapping
    public ResponseEntity<List<Contrato>> listarContratos() {
        return ResponseEntity.ok(Collections.emptyList());
    }

    @PostMapping
    public ResponseEntity<Contrato> criarContrato(@RequestBody Contrato contrato) {
        return ResponseEntity.ok(contrato);
    }
}