package com.example.gearrent.DTO;
import java.math.BigDecimal;
public record EquipamentoRequest(String nome, String numeroPatrimonio, String modelo, BigDecimal valorDiariaBase) {}