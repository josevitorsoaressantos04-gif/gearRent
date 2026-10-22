package com.example.gearrent.domain.repository;

import com.example.gearrent.domain.entities.Equipamento;
import com.example.gearrent.domain.enums.StatusEquipamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EquipamentoRepository extends JpaRepository<Equipamento, Long> {

    Optional<Equipamento> findByIdAndLocadoraId(Long id, Long locadoraId);

    boolean existsByLocadoraIdAndNumeroPatrimonio(Long locadoraId, String numeroPatrimonio);

    boolean existsByLocadoraIdAndNumeroPatrimonioAndIdNot(Long locadoraId, String numeroPatrimonio, Long id);

    List<Equipamento> findByLocadoraIdAndStatusNotAndNomeContainingIgnoreCaseOrderByNomeAsc(
            Long locadoraId, StatusEquipamento status, String nome);

    List<Equipamento> findByLocadoraIdAndStatusAndNomeContainingIgnoreCaseOrderByNomeAsc(
            Long locadoraId, StatusEquipamento status, String nome);

    @Query("select coalesce(max(e.codigoSequencial), 0) from Equipamento e where e.locadora.id = :locadoraId")
    int buscarUltimoCodigo(@Param("locadoraId") Long locadoraId);

    // Trava os registros durante a locação para duas pessoas não alugarem o mesmo equipamento ao mesmo tempo
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Equipamento e where e.id in :ids and e.locadora.id = :locadoraId")
    List<Equipamento> buscarParaLocacao(@Param("ids") Collection<Long> ids,
                                        @Param("locadoraId") Long locadoraId);
}