package com.example.gearrent.controllers;

import com.example.gearrent.entities.Usuario;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Collections;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    @GetMapping
    public ResponseEntity<List<Usuario>> listarUsuarios() {
        // Retorno mockado: futuramente será substituído por Page<Usuario>
        return ResponseEntity.ok(Collections.emptyList());
    }

    @PostMapping
    public ResponseEntity<Usuario> criarUsuario(@RequestBody Usuario usuario) {
        // TODO: A senha do usuário deve ser "hashada" (ex: BCrypt) antes de salvar no banco
        return ResponseEntity.ok(usuario);
    }
}