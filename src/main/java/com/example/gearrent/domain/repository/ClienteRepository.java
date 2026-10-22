package com.example.gearrent.domain.repository;

import com.example.gearrent.domain.entities.Cliente;
import com.example.gearrent.domain.enums.StatusCadastro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByIdAndLocadoraId(Long id, Long locadoraId);

    boolean existsByLocadoraIdAndDocumento(Long locadoraId, String documento);

    List<Cliente> findByLocadoraIdAndStatusNotAndNomeContainingIgnoreCaseOrderByNomeAsc(
            Long locadoraId, StatusCadastro status, String nome);
}