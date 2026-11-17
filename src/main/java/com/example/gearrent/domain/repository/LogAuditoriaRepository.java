package com.example.gearrent.domain.repository;

import com.example.gearrent.domain.entities.LogAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {

    List<LogAuditoria> findTop200ByLocadoraIdOrderByDataHoraDescIdDesc(Long locadoraId);
}
