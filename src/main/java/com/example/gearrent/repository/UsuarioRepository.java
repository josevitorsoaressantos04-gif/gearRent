package com.example.gearrent.repository;

import com.example.gearrent.entities.Usuario;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Retorna a lista direto. Se não achar, retorna lista vazia.
    List<Usuario> findByNome(String nome, Sort sort);

    // Retorna o Optional da entidade Usuario limpo e pronto para uso no serviço de autenticação
    Optional<Usuario> findByEmail(String email);

   Optional<Usuario> findByLogin(@NotBlank(message = "Campo Obrigatório") String login);
}