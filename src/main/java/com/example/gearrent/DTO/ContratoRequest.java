package com.example.gearrent.DTO;

import com.example.gearrent.entities.Cliente;
import com.example.gearrent.entities.Equipamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ContratoRequest(Cliente clienteId, Equipamento equipamentoId,
                              LocalDateTime dataRetirada, LocalDateTime dataDevolucaoPrevista,
                              LocalDateTime dataDevolucaoReal,
                              double valorAcordado) {
}