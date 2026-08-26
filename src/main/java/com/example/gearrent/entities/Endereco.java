package com.example.gearrent.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Endereco {
    private long id;
    private String rua;
    private int numero;
    private String cidade;
    private String estado;
    private String cep;
}
