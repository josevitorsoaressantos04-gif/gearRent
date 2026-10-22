package com.example.gearrent.domain.entities;

import com.example.gearrent.domain.enums.StatusCadastro;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(unique = true)
    private String documento;
    private String email;
    private String celular;
    private String telefone;
    private LocalDateTime  dataCadastro;
    private LocalDateTime dataAtualizacao;

    @Enumerated(EnumType.STRING)
    private StatusCadastro status;

    @Embedded
    private Endereco enderecoResidencial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locadora_id", nullable = false)
    private Locadora locadora;
}
