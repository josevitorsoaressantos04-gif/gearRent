package com.example.gearrent.domain.entities;

import com.example.gearrent.domain.enums.StatusCadastro;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String login;
    private String senha; //TODO: adicionar uma criptografia para senha e um token para o gerenciamento de acesso
    private String nome;
    private LocalDateTime dataCadastro;
    private LocalDateTime dataAtualizacao;
    private LocalDate data_nascimento;

    @Enumerated(EnumType.STRING)
    private StatusCadastro status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locadora_id", nullable = false)
    private Locadora locadora;
}
