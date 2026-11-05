package com.example.gearrent.domain.repository;

import com.example.gearrent.domain.enums.CategoriaEquipamento;

public record ItemLocacaoProjecao(
        Long locacaoId,
        Long equipamentoId,
        String nome,
        String modelo,
        CategoriaEquipamento categoria,
        String numeroPatrimonio
) {}