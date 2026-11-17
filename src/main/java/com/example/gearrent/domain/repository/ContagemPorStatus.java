package com.example.gearrent.domain.repository;

import com.example.gearrent.domain.enums.StatusLocacao;

public record ContagemPorStatus(StatusLocacao status, Long total) {}