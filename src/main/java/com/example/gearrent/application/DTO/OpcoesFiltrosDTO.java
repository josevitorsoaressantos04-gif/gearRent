package com.example.gearrent.application.DTO;

import java.util.List;

/** Resposta agregada do endpoint /filtros-opcoes (uma única chamada popula todos os dropdowns). */
public record OpcoesFiltrosDTO(
        FiltrosDinamicosDTO filtros,
        List<ClienteDropdownDTO> clientes
) {}