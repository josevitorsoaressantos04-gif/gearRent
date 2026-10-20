package com.example.gearrent.domain.repository;

import com.example.gearrent.domain.entities.Sessao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SessaoRepository extends JpaRepository<Sessao, Long> {
    Optional<Sessao> findByTokenHash(String tokenHash);
    void deleteByTokenHash(String tokenHash);
}
