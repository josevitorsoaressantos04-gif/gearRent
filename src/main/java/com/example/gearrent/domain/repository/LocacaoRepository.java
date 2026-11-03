package com.example.gearrent.domain.repository;

import com.example.gearrent.domain.entities.Locacao;
import com.example.gearrent.domain.enums.StatusLocacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

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
}