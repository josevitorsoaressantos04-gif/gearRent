package com.example.gearrent.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.NoArgsConstructor;
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Boolean ativo;
    private String nome;
    @Column(unique = true)
    private String cpf;
    private LocalDate dataNascimento;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String login;
    private String senha;
    private String telefone;
    @ManyToOne
    @JoinColumn(name = "empresa_id", referencedColumnName = "id")
    private Empresa empresa;
    private LocalDateTime dataCadastro;
    private LocalDateTime dataAtualizacao;
}
