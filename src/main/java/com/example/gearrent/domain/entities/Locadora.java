package com.example.gearrent.domain.entities;

import com.example.gearrent.domain.enums.StatusCadastro;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Locadora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nomeFantasia;

    @Column(unique = true)
    private String cnpj;

    private LocalDateTime dataCadastro;
    private LocalDateTime dataAtualizacao;


    @Enumerated(EnumType.STRING)
    private StatusCadastro status;
}
