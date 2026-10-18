package com.example.gearrent.domain.repository;

import com.example.gearrent.domain.entities.Cliente;
import com.example.gearrent.domain.entities.Equipamento;
import com.example.gearrent.domain.entities.Locacao;
import com.example.gearrent.domain.entities.Locadora;
import com.example.gearrent.domain.enums.StatusLocacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface LocacaoRepository extends JpaRepository<Locacao, Long> {

    // 1. Essencial para a devolução: acha a locação em andamento daquele equipamento, NAQUELA locadora
    Optional<Locacao> findByLocadoraAndEquipamentosAndStatus(Locadora locadora, Set<Equipamento> equipamentos, StatusLocacao statusLocacao);

    // 2. Essencial para o histórico e para a visão de pendências do cliente daquela empresa
    List<Locacao> findByLocadoraAndCliente(Locadora locadora, Cliente cliente);

    // 3. Essencial para o setor financeiro (receitas da loja em um período)
    List<Locacao> findByLocadoraAndDataRetiradaBetween(Locadora locadora, LocalDate inicio, LocalDate fim);
}
