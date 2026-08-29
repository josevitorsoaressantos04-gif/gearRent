package com.example.gearrent.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
public class Empresa {
    private long id;
    private String nome;
    private String cnpj;
    private String telefone;
    private String email;
    private boolean status;
}
