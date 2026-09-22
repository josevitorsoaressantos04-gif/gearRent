package com.example.gearrent.repository;

import com.example.gearrent.entities.Usuario;

import org.springdoc.core.converters.models.Sort;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario,Long> {

    // Retorna a lista direto. Se não achar, retorna lista vazia.
    List<Usuario> findByNome(String nome, Sort sort);

}
