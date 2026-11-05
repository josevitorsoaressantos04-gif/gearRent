package com.example.gearrent.domain.repository;

import com.example.gearrent.application.DTO.ClienteDropdownDTO;
import com.example.gearrent.domain.entities.Cliente;
import com.example.gearrent.domain.enums.StatusCadastro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // Sem filtro de status: enxerga ATIVO, INATIVO e CANCELADO (regra da reativação)
    Optional<Cliente> findByLocadoraIdAndDocumento(Long locadoraId, String documento);

    // Busca por id sempre amarrada à locadora (multi-tenant)
    Optional<Cliente> findByIdAndLocadoraId(Long id, Long locadoraId);

    // Listagem: esconde os cancelados e filtra por parte do nome (nome vazio traz todos)
    List<Cliente> findByLocadoraIdAndStatusNotAndNomeContainingIgnoreCaseOrderByNome(
            Long locadoraId, StatusCadastro status, String nome);

    // ClienteRepository
    @Query("""
        SELECT new com.example.gearrent.application.DTO.ClienteDropdownDTO(c.id, c.codigoSequencial, c.nome)
        FROM Cliente c
        WHERE c.locadora.id = :locadoraId
        ORDER BY c.nome
        """)
    List<ClienteDropdownDTO> listarParaDropdown(@Param("locadoraId") Long locadoraId);
}