package com.example.gearrent.domain.repository;

import com.example.gearrent.domain.entities.TokenRecuperacaoSenha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TokenRecuperacaoSenhaRepository extends JpaRepository<TokenRecuperacaoSenha, Long> {

    @Query("select t from TokenRecuperacaoSenha t join fetch t.usuario where t.tokenHash = :tokenHash")
    Optional<TokenRecuperacaoSenha> buscarPorHash(@Param("tokenHash") String tokenHash);

    boolean existsByUsuarioIdAndCriadoEmAfter(Long usuarioId, LocalDateTime limite);

    @Modifying
    @Query("delete from TokenRecuperacaoSenha t where t.usuario.id = :usuarioId and t.usadoEm is null")
    void apagarPendentesDoUsuario(@Param("usuarioId") Long usuarioId);
}