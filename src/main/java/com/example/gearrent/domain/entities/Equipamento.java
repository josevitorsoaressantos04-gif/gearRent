package com.example.gearrent.domain.entities;

import com.example.gearrent.domain.enums.CategoriaEquipamento;
import com.example.gearrent.domain.enums.StatusEquipamento;
import com.example.gearrent.domain.exception.EquipamentoIndisponivelException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "equipamento",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_equipamento_locadora_patrimonio",
                        columnNames = {"locadora_id", "numero_patrimonio"}),
                @UniqueConstraint(
                        name = "uk_equipamento_locadora_codigo",
                        columnNames = {"locadora_id", "codigo_sequencial"})
        })
public class Equipamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unicidade só por locadora (ver @Table). Nunca use unique = true isolado aqui.
    private int codigoSequencial;
    private String nome;
    private String modelo;

    private String numeroPatrimonio;

    @Enumerated(EnumType.STRING)
    private CategoriaEquipamento categoria;
    private StatusEquipamento status;
    private BigDecimal valorDiaria;
    private LocalDateTime dataCadastro;
    private LocalDateTime dataAtualizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locadora_id", nullable = false)
    private Locadora locadora;

    public boolean estaDisponivel() {
        return this.status == StatusEquipamento.DISPONIVEL;
    }

    public void marcarComoAlugado() {
        if (!estaDisponivel()) {
            throw new EquipamentoIndisponivelException(
                    "O equipamento \"" + nome + "\" não está disponível para locação.");
        }
        this.status = StatusEquipamento.ALUGADO;
    }

    public void marcarComoDisponivel() {
        this.status = StatusEquipamento.DISPONIVEL;
    }
}