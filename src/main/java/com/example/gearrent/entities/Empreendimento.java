package com.example.gearrent.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class Empreendimento {
    private Long id;
    private Cliente cliente;
    private List<Equipamento> equipamento;
    private String endereco;
    private String numeroEndereco;
    private String cep;
    private String bairro;
}
