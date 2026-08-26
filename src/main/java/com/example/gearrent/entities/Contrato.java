package com.example.gearrent.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class Contrato{
    private long id;
    private long clienteId;
    private long equipamentoId;
    private Date dataRetirada;
    private Date dataDevolucaoPrevista;
    private Date dataDevlucaoReal;
    private double valorAcordado;
    private long enderecoId;
}
