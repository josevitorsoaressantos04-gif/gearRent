package com.example.gearrent.domain.repository;

import com.example.gearrent.domain.entities.Equipamento;
import com.example.gearrent.domain.entities.Locadora;
import com.example.gearrent.domain.enums.CategoriaEquipamento;
import com.example.gearrent.domain.enums.StatusEquipamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipamentoRepository extends JpaRepository<Equipamento, Long> {

    // Essencial para listar itens disponíveis na tela inicial da empresa
    List<Equipamento> findByLocadoraAndStatus(Locadora locadora, StatusEquipamento status);

    // Permite agrupar os itens visualmente na interface daquela locadora
    List<Equipamento> findByLocadoraAndCategoria(Locadora locadora, CategoriaEquipamento categoria);

    // Permite agrupar categorias excluindo ferramentas que estão em manutenção ou alugadas
    List<Equipamento> findByLocadoraAndCategoriaAndStatus(Locadora locadora,
                                                                                CategoriaEquipamento categoria,
                                                                                StatusEquipamento status);

    List<Equipamento> findByLocadoraAndNome(Locadora locadora, String nome);

    List<Equipamento> findByLocadoraAndModelo(Locadora locadora, String modelo);

    // Consulta de faixas de valores restrita à locadora
    List<Equipamento> findByLocadoraAndValorDiariaBetween(Locadora locadora, double valorInicial, double valorFinal);
}

