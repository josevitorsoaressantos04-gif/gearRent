package com.example.gearrent.repository;

import com.example.gearrent.entities.Contrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ContratoRepository extends JpaRepository<Contrato, Long> {

    // Retorna 'true' se houver sobreposição de agenda para algum dos equipamentos no intervalo informado
    @Query("""
        SELECT COUNT(c) > 0 FROM Contrato c
        JOIN c.equipamento e
        WHERE e.id IN :equipamentoIds
          AND c.status = true
          AND (:dataRetirada < c.dataDevolucaoPrevista AND :dataDevolucaoPrevista > c.dataRetirada)
    """)
    boolean isEquipamentosOcupadosNoPeriodo(
            @Param("equipamentoIds") List<Long> equipamentoIds,
            @Param("dataRetirada") LocalDateTime dataRetirada,
            @Param("dataDevolucaoPrevista") LocalDateTime dataDevolucaoPrevista
    );
}