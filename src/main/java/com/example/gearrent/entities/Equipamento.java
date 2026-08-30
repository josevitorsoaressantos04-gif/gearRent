package com.example.gearrent.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Equipamento {
    private long id;
    private String nome;
    private String numeroPatrimonio;
    private String modelo;
    private String versao;
    private double valorDiariaBase;
}
