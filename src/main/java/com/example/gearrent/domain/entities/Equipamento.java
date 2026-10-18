package com.example.gearrent.domain.entities;

import com.example.gearrent.domain.enums.StatusEquipamento;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.example.gearrent.domain.enums.CategoriaEquipamento;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Equipamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private int CodigoSequencial;
    private String nome;
    private String modelo;

    @Column(unique = true)
    private String numeroPatrimonio;

    @Enumerated(EnumType.STRING)
    private CategoriaEquipamento categoria;
    private StatusEquipamento status;
    private double valorDiaria;
    private LocalDateTime dataCadastro;
    private LocalDateTime dataAtualizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locadora_id", nullable = false)
    private Locadora locadora;
}
