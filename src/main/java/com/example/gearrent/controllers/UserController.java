package com.example.gearrent.controllers;


import com.example.gearrent.entities.Usuario;
import com.example.gearrent.entities.UsuarioLocador;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
public class UserController {



    @GetMapping("/locador/{pfId}")
    public UsuarioLocador consultaUsuarioLocador(UsuarioLocador usuarioLocador) {
        return usuarioLocador;
    }

    @GetMapping("/cliente/{pfId}")
    public String consultaUsuarioCliente(@PathVariable Long pfId) {
        return "Usuario PF ID " + pfId;
    }

    @GetMapping("/empresa/{empresaId}")
    public String consultaEmpresa(@PathVariable Long empresaId) {
        return "Usuario PJ ID:" + empresaId;
    }

    @PostMapping
    public ResponseEntity<UsuarioLocador> adicionaUsuarioLocador(@RequestBody UsuarioLocador usuarioLocador) {
        return ResponseEntity.ok(usuarioLocador);
    }
}
