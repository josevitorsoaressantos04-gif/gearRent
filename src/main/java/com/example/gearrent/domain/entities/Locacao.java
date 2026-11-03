package com.example.gearrent.domain.entities;

import com.example.gearrent.domain.enums.StatusLocacao;
import com.example.gearrent.domain.enums.TipoPagamento;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "locacao",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_locacao_locadora_codigo",
                columnNames = {"locadora_id", "codigo_sequencial"}))
public class Locacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Número exibido na tela (1, 2, 3... por locadora). O id continua sendo a chave.
    @Column(name = "codigo_sequencial", nullable = false, updatable = false)
    private Long codigoSequencial;

    private LocalDateTime dataPrevista;
    private LocalDateTime dataReal;
    private LocalDateTime dataRegistro;
    private LocalDateTime dataAtualizacao;
    private LocalDateTime dataDevolucao;
    private LocalDateTime dataRetirada;

    @Enumerated(EnumType.STRING)
    private StatusLocacao status;
    private TipoPagamento tipoPagamento;
    private double valorDesconto;
    private double valorMultaPorAtraso;
    private double valorFinalCobrado;

    /*
    Como o foco é ser um sistema simples para um público de maior idade
    então foi colocado o responsavel pela retirada juntamente com o documento
    em caso de o cliente principal de locação for uma empresa porém
    precisa registrar o responsavel pela retirada(funcionário ou parceiro deste cliente que
    solicitou a locação)
     */
    private String responsavelRetirada;
    private String documentoResponsavelRetirada;

    /*
    @Embedded: o Hibernate "achata" os atributos do Endereco como colunas desta tabela,
    mantendo a classe separada no código e a tabela plana (sem JOINs).
     */
    @Embedded
    private Endereco enderecoEntrega;

    @ManyToMany
    @JoinTable(
            name = "locacao_equipamentos",
            joinColumns = @JoinColumn(name = "locacao_id"),
            inverseJoinColumns = @JoinColumn(name = "equipamento_id")
    )
    private Set<Equipamento> equipamentos = new HashSet<>();

    @ManyToOne()
    private Cliente cliente;

    @ManyToOne()
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locadora_id", nullable = false)
    private Locadora locadora;
}