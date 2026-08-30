package com.example.gearrent.controllers;

import com.example.gearrent.DTO.CadastroUsuarioRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class CadastroUsuarioController {

    @PostMapping
    public ResponseEntity<CadastroUsuarioRequest> cadastroUsuarioRequest(CadastroUsuarioRequest cadastroUsuarioRequest) {
        if(cadastroUsuarioRequest.nome().isEmpty() || cadastroUsuarioRequest.cpf().isEmpty()
        || cadastroUsuarioRequest.dataNascimento().isEmpty() || cadastroUsuarioRequest.email().isEmpty() ||
                cadastroUsuarioRequest.senha().isEmpty() || cadastroUsuarioRequest.telefone().isEmpty()){
            return  ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(cadastroUsuarioRequest);
    }
}
