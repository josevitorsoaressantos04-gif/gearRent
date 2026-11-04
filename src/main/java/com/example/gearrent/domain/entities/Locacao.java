package com.example.gearrent.domain.entities;

import com.example.gearrent.domain.enums.StatusLocacao;
import com.example.gearrent.domain.enums.TipoPagamento;
import com.example.gearrent.domain.exception.RegraDeNegocioException;
import com.example.gearrent.domain.exception.TransicaoStatusInvalidaException;
import com.example.gearrent.domain.service.CalculadoraLocacao;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

    // ------------------------------------------------------------------
    // Comportamento (regras do ciclo de vida da locação)
    // ------------------------------------------------------------------

    /** Abre uma locação nova. O valor aqui é uma PREVISÃO (devolução na data prevista). */
    public static Locacao abrir(Locadora locadora, Long codigoSequencial, Cliente cliente, Usuario usuario,
                                Set<Equipamento> equipamentos, LocalDateTime dataRetirada,
                                LocalDateTime dataPrevista, TipoPagamento tipoPagamento,
                                BigDecimal valorDesconto, String responsavelRetirada,
                                String documentoResponsavelRetirada, Endereco enderecoEntrega,
                                LocalDateTime agora) {

        Locacao locacao = new Locacao();
        locacao.locadora = locadora;
        locacao.codigoSequencial = codigoSequencial;
        locacao.cliente = cliente;
        locacao.usuario = usuario;
        locacao.dataRetirada = dataRetirada;
        locacao.dataPrevista = dataPrevista;
        locacao.tipoPagamento = tipoPagamento;
        locacao.valorDesconto = valorDesconto.doubleValue();
        locacao.responsavelRetirada = responsavelRetirada;
        locacao.documentoResponsavelRetirada = documentoResponsavelRetirada;
        locacao.enderecoEntrega = enderecoEntrega;
        locacao.status = StatusLocacao.EM_ANDAMENTO;
        locacao.dataRegistro = agora;
        locacao.dataAtualizacao = agora;

        for (Equipamento equipamento : equipamentos) {
            equipamento.marcarComoAlugado();   // lança EquipamentoIndisponivelException se já estiver alugado
            locacao.equipamentos.add(equipamento);
        }

        CalculadoraLocacao.Resultado previsto = locacao.simular(dataPrevista);
        if (previsto.valorDesconto().compareTo(valorDesconto) < 0) {
            throw new RegraDeNegocioException(
                    "O desconto não pode ser maior que o valor das diárias.");
        }
        locacao.aplicar(previsto);
        return locacao;
    }

    /** Marca como ATRASADA e atualiza o valor acumulado se passou da data prevista. */
    public void sincronizarAtraso(LocalDateTime agora) {
        if (status != StatusLocacao.EM_ANDAMENTO && status != StatusLocacao.ATRASADA) {
            return;
        }
        if (agora.toLocalDate().isAfter(dataPrevista.toLocalDate())) {
            if (status == StatusLocacao.EM_ANDAMENTO) {
                status = StatusLocacao.ATRASADA;
                dataAtualizacao = agora;
            }
            aplicar(simular(agora));
        }
    }

    public void devolver(LocalDateTime dataDevolucaoReal, LocalDateTime agora) {
        exigirTransicao(StatusLocacao.CONCLUIDA);

        if (dataDevolucaoReal.toLocalDate().isBefore(dataRetirada.toLocalDate())) {
            throw new RegraDeNegocioException(
                    "A data de devolução não pode ser anterior à data de retirada.");
        }

        aplicar(simular(dataDevolucaoReal));
        this.dataReal = dataDevolucaoReal;
        this.dataDevolucao = dataDevolucaoReal;
        this.status = StatusLocacao.CONCLUIDA;
        this.dataAtualizacao = agora;
        equipamentos.forEach(Equipamento::marcarComoDisponivel);
    }

    public void cancelar(LocalDateTime agora) {
        if (status != StatusLocacao.EM_ANDAMENTO) {
            throw new TransicaoStatusInvalidaException(
                    "Só é possível cancelar uma locação que ainda está em andamento.");
        }
        this.status = StatusLocacao.CANCELADA;
        this.valorDesconto = 0;
        this.valorMultaPorAtraso = 0;
        this.valorFinalCobrado = 0;
        this.dataAtualizacao = agora;
        equipamentos.forEach(Equipamento::marcarComoDisponivel);
    }

    /** Dias de atraso apurados (para exibir na tela). */
    public long calcularDiasAtraso(LocalDateTime agora) {
        if (status == StatusLocacao.CANCELADA) {
            return 0;
        }
        LocalDateTime referencia = dataReal != null ? dataReal : agora;
        return Math.max(0, ChronoUnit.DAYS.between(dataPrevista.toLocalDate(), referencia.toLocalDate()));
    }

    private void exigirTransicao(StatusLocacao destino) {
        if (!status.podeIrPara(destino)) {
            throw new TransicaoStatusInvalidaException(switch (status) {
                case CONCLUIDA -> "Esta locação já foi concluída e não pode mais ser alterada.";
                case CANCELADA -> "Esta locação foi cancelada e não pode mais ser alterada.";
                default -> "Esta ação não é permitida para a situação atual da locação.";
            });
        }
    }

    private CalculadoraLocacao.Resultado simular(LocalDateTime referencia) {
        BigDecimal somaDiarias = equipamentos.stream()
                .map(Equipamento::getValorDiaria)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CalculadoraLocacao.calcular(
                dataRetirada.toLocalDate(),
                dataPrevista.toLocalDate(),
                referencia.toLocalDate(),
                somaDiarias,
                BigDecimal.valueOf(valorDesconto));
    }

    private void aplicar(CalculadoraLocacao.Resultado r) {
        this.valorDesconto = r.valorDesconto().doubleValue();
        this.valorMultaPorAtraso = r.valorMulta().doubleValue();
        this.valorFinalCobrado = r.valorTotal().doubleValue();
    }
}