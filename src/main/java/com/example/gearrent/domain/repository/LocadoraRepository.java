package com.example.gearrent.domain.repository;

import com.example.gearrent.domain.entities.Locadora;
import com.example.gearrent.domain.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocadoraRepository extends JpaRepository<Locadora, Long> {
}
