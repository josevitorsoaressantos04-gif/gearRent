package com.example.gearrent.repository;

import com.example.gearrent.entities.Contrato;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ContratoRepository extends JpaRepository<Contrato, Long> {

    // Busca histórico de contratos de um determinado cliente
    Page<Contrato> findByClienteId(Long clienteId, Pageable pageable);

    // PREVENÇÃO DE OVERBOOKING / DOUBLE-BOOKING (Versão com JOIN em coleção)
    @Query("""
        SELECT COUNT(c) > 0 FROM Contrato c
        JOIN c.equipamento e
        WHERE e.id = :equipamentoId
          AND (:dataRetirada < c.dataDevolucaoPrevista AND :dataDevolucaoPrevista > c.dataRetirada)
    """)
    boolean isEquipamentoOcupadoNoPeriodo(
            @Param("equipamentoId") Long equipamentoId,
            @Param("dataRetirada") LocalDateTime dataRetirada,
            @Param("dataDevolucaoPrevista") LocalDateTime dataDevolucaoPrevista
    );

    // Busca contratos que possuem um determinado equipamento na lista
    @Query("SELECT c FROM Contrato c JOIN c.equipamento e WHERE e.id = :equipamentoId")
    List<Contrato> findByEquipamentoId(@Param("equipamentoId") Long equipamentoId);
}