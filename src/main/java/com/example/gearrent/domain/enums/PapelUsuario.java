package com.example.gearrent.domain.enums;

public enum PapelUsuario {
    ADMIN,      // dono/administrador da base
    OPERADOR;   // usuário operacional da base

    // Nome usado pelo Spring Security (hasRole('ADMIN') procura por ROLE_ADMIN)
    public String autoridade() {
        return "ROLE_" + name();
    }
}