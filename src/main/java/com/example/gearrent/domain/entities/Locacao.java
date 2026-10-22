package com.example.gearrent.domain.entities;

import com.example.gearrent.domain.enums.TipoPagamento;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.example.gearrent.domain.enums.StatusLocacao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Locacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private int codigoSequencial;
    private LocalDateTime dataPrevista;
    private LocalDateTime dataReal;
    private LocalDateTime dataRegistro;
    private LocalDateTime dataAtualizacao;
    private LocalDate dataDevolucao;
    private LocalDate dataRetirada;

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
    Funções e Capacidades do @Embedded (A Aplicação)

    Achatamento Estrutural (Flattening): A função principal desta anotação é instruir o Hibernate a extrair
    os atributos do molde e injetá-los diretamente como colunas na tabela da entidade que a declarou.

    Isolamento de Domínio: Ela permite separar conceitos coesos no back-end
    (um endereço é logicamente um grupo de dados inseparáveis), mantendo as classes enxutas
    enquanto o banco de dados permanece em uma estrutura plana e otimizada
    (sem necessidade de JOINs em consultas SQL).
     */
    @Embedded
    private Endereco enderecoEntrega;

    @ManyToMany
    @JoinTable(
            name = "locacao_equipamentos", // Opcional: nome da tabela associativa que será criada
            joinColumns = @JoinColumn(name = "locacao_id"), // Chave da locação
            inverseJoinColumns = @JoinColumn(name = "equipamento_id") // Chave do equipamento
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
