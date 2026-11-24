package com.example.gearrent.domain.entities;

import com.example.gearrent.domain.enums.PapelUsuario;
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
@Table(name = "usuario",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_usuario_login",
                        columnNames = {"login"}),
                @UniqueConstraint(
                        name = "uk_usuario_locadora_cpf",
                        columnNames = {"locadora_id", "cpf"})
        })
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String login;
    private String senha;
    private String nome;
    private String sobrenome;

    private String cpf;
    private LocalDateTime dataCadastro;
    private LocalDateTime dataAtualizacao;
    private LocalDate data_nascimento;
    private LocalDateTime avatarAtualizadoEm;

    @Enumerated(EnumType.STRING)
    private StatusCadastro status;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PapelUsuario role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locadora_id", nullable = false)
    private Locadora locadora;
}
