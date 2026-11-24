package com.example.gearrent.domain.repository;

import com.example.gearrent.domain.entities.Locadora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LocadoraRepository extends JpaRepository<Locadora, Long> {

    @Modifying
    @Query("update Locadora l set l.ultimoCodigoCliente = l.ultimoCodigoCliente + 1 where l.id = :id")
    int incrementarUltimoCodigoCliente(@Param("id") Long id);

    @Query("select l.ultimoCodigoCliente from Locadora l where l.id = :id")
    Long buscarUltimoCodigoCliente(@Param("id") Long id);

    default Long proximoCodigoCliente(Long locadoraId) {
        incrementarUltimoCodigoCliente(locadoraId);
        return buscarUltimoCodigoCliente(locadoraId);
    }

    @Modifying
    @Query("update Locadora l set l.ultimoCodigoEquipamento = l.ultimoCodigoEquipamento + 1 where l.id = :id")
    int incrementarUltimoCodigoEquipamento(@Param("id") Long id);

    @Query("select l.ultimoCodigoEquipamento from Locadora l where l.id = :id")
    Long buscarUltimoCodigoEquipamento(@Param("id") Long id);

    default Long proximoCodigoEquipamento(Long locadoraId) {
        incrementarUltimoCodigoEquipamento(locadoraId);
        return buscarUltimoCodigoEquipamento(locadoraId);
    }

    @Modifying
    @Query("update Locadora l set l.ultimoCodigoLocacao = l.ultimoCodigoLocacao + 1 where l.id = :id")
    int incrementarUltimoCodigoLocacao(@Param("id") Long id);

    @Query("select l.ultimoCodigoLocacao from Locadora l where l.id = :id")
    Long buscarUltimoCodigoLocacao(@Param("id") Long id);

    default Long proximoCodigoLocacao(Long locadoraId) {
        incrementarUltimoCodigoLocacao(locadoraId);
        return buscarUltimoCodigoLocacao(locadoraId);
    }
}
