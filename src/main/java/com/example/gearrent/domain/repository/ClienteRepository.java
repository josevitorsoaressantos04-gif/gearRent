package com.example.gearrent.domain.repository;

import com.example.gearrent.domain.entities.Cliente;
import com.example.gearrent.domain.entities.Locadora;
import com.example.gearrent.domain.enums.StatusCadastro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    // Regra de prevenção de duplicidade restrita à mesma empresa
    boolean existsByDocumentoAndLocadora(String documento, Locadora locadora);

    // Busca exata e segura dentro da mesma empresa
    Optional<Cliente> findByDocumentoAndLocadora(String documento, Locadora locadora);

    List<Cliente> findByLocadoraAndNome(Locadora locadora, String nome);

    List<Cliente> findByLocadoraAndStatus(Locadora locadora, StatusCadastro status);
}
