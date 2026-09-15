package com.example.gearrent.repository;

import com.example.gearrent.entities.Endereco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnderecoRepository extends JpaRepository<Endereco, Long> {

    List<Endereco> findByCep(String cep);

    List<Endereco> findByCidadeId(Long cidadeId);
}