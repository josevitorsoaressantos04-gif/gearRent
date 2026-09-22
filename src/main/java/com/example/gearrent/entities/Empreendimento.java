package com.example.gearrent.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tb_empreendimento")
public class Empreendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome; // Ex: "Obra Edifício Horizon"

    // Vários empreendimentos pertencem a UM cliente
    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // Um empreendimento pode reutilizar a estrutura da entidade Endereco
    @ManyToOne
    @JoinColumn(name = "endereco_id", nullable = false)
    private Endereco endereco;

    // Um empreendimento pode ter vários equipamentos alocados na obra
    @OneToMany
    @JoinTable(
            name = "tb_empreendimento_equipamento",
            joinColumns = @JoinColumn(name = "empreendimento_id"),
            inverseJoinColumns = @JoinColumn(name = "equipamento_id")
    )
    private List<Equipamento> equipamentos;
}