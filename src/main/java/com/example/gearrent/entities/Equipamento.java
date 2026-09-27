package com.example.gearrent.entities;

import com.example.gearrent.entities.enums.StatusEquipamento;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_equipamento")
public class Equipamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    // Garante que não existam dois patrimônios físicos iguais no pátio
    @Column(nullable = false, unique = true)
    private String numeroPatrimonio;

    private String modelo;

    // Correção do Risco R-14: Substituição do primitivo double
    @Column(nullable = false)
    private BigDecimal valorDiariaBase;

    // Correção do Risco R-02: Máquina de estados logísticos
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusEquipamento status;

    // Correção do Risco R-03: Lock Otimista contra dupla locação simultânea
    @Version
    private Long versao;

    @Column(nullable = false)
    private Boolean ativo;
}