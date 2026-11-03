package com.example.gearrent.domain.repository;

import com.example.gearrent.domain.entities.Locadora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LocadoraRepository extends JpaRepository<Locadora, Long> {

    @Query(value = """
            UPDATE locadora SET ultimo_codigo_cliente = ultimo_codigo_cliente + 1
            WHERE id = :locadoraId RETURNING ultimo_codigo_cliente
            """, nativeQuery = true)
    Long proximoCodigoCliente(@Param("locadoraId") Long locadoraId);

    @Query(value = """
            UPDATE locadora SET ultimo_codigo_equipamento = ultimo_codigo_equipamento + 1
            WHERE id = :locadoraId RETURNING ultimo_codigo_equipamento
            """, nativeQuery = true)
    Long proximoCodigoEquipamento(@Param("locadoraId") Long locadoraId);

    @Query(value = """
            UPDATE locadora SET ultimo_codigo_locacao = ultimo_codigo_locacao + 1
            WHERE id = :locadoraId RETURNING ultimo_codigo_locacao
            """, nativeQuery = true)
    Long proximoCodigoLocacao(@Param("locadoraId") Long locadoraId);
}