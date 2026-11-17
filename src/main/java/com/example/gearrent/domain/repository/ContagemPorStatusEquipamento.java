package com.example.gearrent.domain.repository;

import com.example.gearrent.domain.enums.StatusEquipamento;

public record ContagemPorStatusEquipamento(StatusEquipamento status, Long total) {}
