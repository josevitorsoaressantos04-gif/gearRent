package com.example.gearrent.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/Clientes")
public class ClienteController {

    @GetMapping
    public String consultaTodosClientes() {
        return "Todos os Clientes";
    }

    @GetMapping("/{id}")
    public String consultaClientePorID(@PathVariable Long id) {
        return "Clientes ID: " + id;
    }

    @GetMapping("/PF")
    public String consultaClientesPF() {
        return "Usuarios PF";
    }

    @GetMapping("/PF/{clientePfId}")
    public String consultaClientesPFID(@PathVariable Long clientePfId) {
        return "Cliente PF ID: " + clientePfId;
    }

    @GetMapping("/PJ")
    public String consultaClientesPJ() {
        return "Clientes PJ";
    }

    @GetMapping("/PJ/{clientePjId}")
    public String consultaClientesPJID(@PathVariable Long clientePjId) {
        return "Clientes PJ" + clientePjId;
    }



}
