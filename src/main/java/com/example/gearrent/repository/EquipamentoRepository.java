package com.example.gearrent.repository;

import com.example.gearrent.entities.Equipamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EquipamentoRepository extends JpaRepository<Equipamento, Long> {

    // Busca rápida pelo tombamento/chassi
    Optional<Equipamento> findByNumeroPatrimonio(String numeroPatrimonio);

    boolean existsByNumeroPatrimonio(String numeroPatrimonio);

    // Busca lista de equipamentos disponíveis por categoria ou modelo
    Page<Equipamento> findByModeloContainingIgnoreCase(String modelo, Pageable pageable);
}