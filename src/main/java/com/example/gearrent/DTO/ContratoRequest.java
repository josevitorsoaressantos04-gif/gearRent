package com.example.gearrent.DTO;
import java.math.BigDecimal;
import java.time.Instant;
public record ContratoRequest(Long clienteId, Long equipamentoId,
                              Instant dataRetirada, Instant dataDevolucaoPrevista,
                              BigDecimal valorAcordado) {}