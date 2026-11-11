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

    @Column(length = 20)
    private String telefone;

    @Column(length = 30)
    private String tema;

    @Embedded
    private Endereco endereco;

    private LocalDateTime dataCadastro;
    private LocalDateTime dataAtualizacao;

    @Enumerated(EnumType.STRING)
    private StatusCadastro status;

    @Column(name = "ultimo_codigo_cliente", nullable = false, updatable = false)
    private Long ultimoCodigoCliente = 0L;

    @Column(name = "ultimo_codigo_equipamento", nullable = false, updatable = false)
    private Long ultimoCodigoEquipamento = 0L;

    @Column(name = "ultimo_codigo_locacao", nullable = false, updatable = false)
    private Long ultimoCodigoLocacao = 0L;
}