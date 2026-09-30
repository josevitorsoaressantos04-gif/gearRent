package com.example.gearrent.repository;

import com.example.gearrent.entities.Empresa;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;



@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    boolean existsByCnpj(String cnpj);
    Optional<Empresa> findByCnpj(String cnpj);
    boolean existsByEmail(String email);
}