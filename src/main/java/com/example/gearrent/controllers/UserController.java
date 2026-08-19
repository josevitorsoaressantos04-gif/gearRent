package com.example.gearrent.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios")
public class UserController {

    @GetMapping("/todos")
    public String consultaTodosUsuarios() {
        return "Todos os usuarios";
    }

    @GetMapping("/PF/{pfId}")
    public String consultaUsuarioPfId(@PathVariable Long pfId) {
        return "Usuario PF ID "+ pfId;
    }

   @GetMapping("/empresa/{empresaId}")
   public String consultaEmpresa(@PathVariable Long empresaId) {
        return "Usuario PJ ID:" + empresaId;
   }
}
