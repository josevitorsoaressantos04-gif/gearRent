package com.example.gearrent.domain.

import org.hibernate.audit.DefaultChangelog;

interface DefaultChangelogRepository extends Repository<DefaultChangelog, Long> {
    
};

import com.gearrent.domain.model.StatusLocacao;

public record ContagemPorStatus(StatusLocacao status, Long total) {}