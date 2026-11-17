package com.example.gearrent.application.DTO;

import com.example.gearrent.domain.enums.CategoriaEquipamento;
import java.util.List;

public record FiltrosDinamicosDTO(
        List<CategoriaEquipamento> categorias,
        List<String> modelos
) {}