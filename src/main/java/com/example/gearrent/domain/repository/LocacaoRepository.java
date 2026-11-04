package com.example.gearrent.domain.repository;

import com.example.gearrent.domain.entities.Locacao;
import com.example.gearrent.domain.enums.StatusLocacao;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LocacaoRepository
        extends JpaRepository<Locacao, Long>, JpaSpecificationExecutor<Locacao> {

    // KPIs: uma única query agrupada por status
    @Query("""
            SELECT new com.example.gearrent.domain.repository.ContagemPorStatus(l.status, COUNT(l))
            FROM Locacao l
            WHERE l.locadora.id = :locadoraId
            GROUP BY l.status
            """)
    List<ContagemPorStatus> contarPorStatus(@Param("locadoraId") Long locadoraId);

    // Faturamento ignora locações canceladas
    @Query("""
            SELECT COALESCE(SUM(l.valorFinalCobrado), 0)
            FROM Locacao l
            WHERE l.locadora.id = :locadoraId
              AND l.status <> :statusExcluido
            """)
    double somarFaturamento(@Param("locadoraId") Long locadoraId,
                                @Param("statusExcluido") StatusLocacao statusExcluido);

    // 2ª query da listagem: itens das locações da página, só as colunas necessárias
    @Query("""
            SELECT new com.example.gearrent.domain.repository.ItemLocacaoProjecao(
                l.id, e.id, e.nome, e.modelo, e.categoria, e.numeroPatrimonio)
            FROM Locacao l
            JOIN l.equipamentos e
            WHERE l.locadora.id = :locadoraId
              AND l.id IN :locacaoIds
            """)
    List<ItemLocacaoProjecao> buscarItensDasLocacoes(@Param("locadoraId") Long locadoraId,
                                                     @Param("locacaoIds") Collection<Long> locacaoIds);

    boolean existsByClienteIdAndLocadoraIdAndStatusIn(Long id, Long locadoraId, List<StatusLocacao> emAndamento);

    boolean existsByClienteIdAndLocadoraIdAndStatusIn(
            Long clienteId, Long locadoraId, Collection<StatusLocacao> status);

    // LocacaoRepository
    Optional<Locacao> findByIdAndLocadoraId(Long id, Long locadoraId);

    // Trava a linha: duas pessoas não devolvem/cancelam a mesma locação ao mesmo tempo
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Locacao l where l.id = :id and l.locadora.id = :locadoraId")
    Optional<Locacao> buscarParaAtualizacao(@Param("id") Long id,
                                            @Param("locadoraId") Long locadoraId);
}