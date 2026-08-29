package com.example.gearrent.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
public class Contrato{
    private long id;
    private Cliente cliente;
    private List<Equipamento> equipamento;
    private LocalDateTime dataRetirada;
    private LocalDateTime dataDevolucaoPrevista;
    private LocalDateTime dataDevlucaoReal;
    private double valorAcordado;
}
