package com.example.gearrent.application.DTO;

import com.example.gearrent.domain.enums.CategoriaEquipamento;
import com.example.gearrent.domain.enums.StatusLocacao;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record LocacaoListagemSinteticaDTO(
        Long id,
        ClienteResumo cliente,
        List<ItemResumo> itens,
        long diarias,
        StatusLocacao status,
        LocalDateTime dataRetirada,
        LocalDateTime dataPrevista,
        LocalDateTime dataReal,
        double valorFinalCobrado
) {
    public record ClienteResumo(Long codigoSequencial, String nome) {}
    public record ItemResumo(Long equipamentoId, String nome, String modelo,
                             CategoriaEquipamento categoria, String numeroPatrimonio) {}
}