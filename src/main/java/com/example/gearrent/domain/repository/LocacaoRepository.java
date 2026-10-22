package com.example.gearrent.domain.repository;

import com.example.gearrent.domain.entities.Locacao;
import com.example.gearrent.domain.enums.StatusLocacao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LocacaoRepository extends JpaRepository<Locacao, Long> {

    @EntityGraph(attributePaths = {"cliente", "equipamentos"})
    Optional<Locacao> findByIdAndLocadoraId(Long id, Long locadoraId);

    @EntityGraph(attributePaths = {"cliente", "equipamentos"})
    List<Locacao> findByLocadoraIdOrderByDataRegistroDesc(Long locadoraId);

    boolean existsByClienteIdAndLocadoraIdAndStatusIn(Long clienteId, Long locadoraId,
                                                      Collection<StatusLocacao> status);

    @Query("select coalesce(max(l.codigoSequencial), 0) from Locacao l where l.locadora.id = :locadoraId")
    int buscarUltimoCodigo(@Param("locadoraId") Long locadoraId);
}