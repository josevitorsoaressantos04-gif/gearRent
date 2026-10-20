package com.example.gearrent.domain.repository;

import com.example.gearrent.domain.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByIdAndLocadoraId(Long id, Long locadoraId);

    List<Usuario> findByLocadoraId(Long locadoraId);

    @Query("select u.locadora.id from Usuario u where u.id = :usuarioId")
    Optional<Long> findLocadoraIdByUsuarioId(@Param("usuarioId") Long usuarioId);

    List<Usuario> findByLocadoraIdAndNomeContainingIgnoreCase(Long locadoraId, String nome);
}


