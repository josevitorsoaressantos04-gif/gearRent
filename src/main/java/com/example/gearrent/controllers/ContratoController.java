package com.example.gearrent.controllers;

import com.example.gearrent.DTO.*;
import com.example.gearrent.entities.Contrato;
import com.example.gearrent.entities.Equipamento;
import com.example.gearrent.repository.ContratoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.core.support.RepositoryMethodInvocationListener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/contratos")
public class ContratoController {
    @Autowired
    private ContratoRepository contratoRepository;

    @GetMapping
    public ResponseEntity<List<Contrato>> listarContratos() {
        return ResponseEntity.ok(contratoRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Optional<Contrato>> BuscarContratoId(@PathVariable Long id){
            return ResponseEntity.ok(contratoRepository.findById(id));
    }

    @PostMapping
    public ResponseEntity<ContratoResponse> criarContrato(@RequestBody ContratoRequest request) {
        Contrato contrato = new Contrato();
        contrato.setCliente(request.clienteId());
        contrato.setEquipamento((List<Equipamento>) request.equipamentoId());
        contrato.setDataDevolucaoPrevista(request.dataDevolucaoPrevista());
        contrato.setDataDevolucaoReal(request.dataDevolucaoReal());
        contrato.setDataRetirada(request.dataRetirada());
        contrato.setStatusContrato(true);
        contratoRepository.save(contrato);
        return ResponseEntity.ok(new ContratoResponse(contrato.getId(), "Contrato cadastrado com sucesso"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ContratoResponse> cancelarContrato(@PathVariable Long id) {
        // TODO: Chamar o ContratoService para cancelar o contrato e liberar o equipamento no estoque
        return contratoRepository.findById(id).map(contrato -> {
            contrato.setStatusContrato(false);
            contratoRepository.save(contrato);
            return ResponseEntity.ok(new ContratoResponse(contrato.getId(), "Contrato Cancelado"));
        }).orElse(ResponseEntity.notFound().build());
    }
}