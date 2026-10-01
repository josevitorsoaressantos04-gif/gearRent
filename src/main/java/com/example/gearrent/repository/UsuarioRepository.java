package com.example.gearrent.repository;

import com.example.gearrent.entities.Usuario;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Retorna a lista direto. Se não achar, retorna lista vazia.
    List<Usuario> findByNomeContainingIgnoreCase(String nome, Sort sort);

    // Retorna o Optional da entidade Usuario limpo e pronto para uso no serviço de autenticação
    Optional<Usuario> findByEmail(String email);

    // Busca exata por Login (único)
    Optional<Usuario> findByLogin(@NotBlank(message = "Campo Obrigatório") String login);

    // Busca exata por CPF (único)
    Optional<Usuario> findByCpf(@NotBlank(message = "Campo Obrigatório")String cpf);

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);

    // Busca por intervalo na data de criação/cadastro
    List<Usuario> findByDataCadastroBetween(LocalDateTime inicio, LocalDateTime fim);
}