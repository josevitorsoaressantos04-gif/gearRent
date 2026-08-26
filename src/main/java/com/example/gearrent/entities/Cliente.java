package com.example.gearrent.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class Cliente {
    private long id;
    private String nome;
    private String email;
    private String cpf;
}
