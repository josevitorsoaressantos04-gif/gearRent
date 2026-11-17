package com.example.gearrent.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entidade que armazena o histórico de auditoria das ações executadas na locadora (tenant).
 * Registra qual operador realizou a ação, em qual módulo (LOCACAO, CLIENTE, EQUIPAMENTO, etc.),
 * se foi um cadastro retroativo de locação e a descrição detalhada do evento.
 */
@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "log_auditoria")
public class LogAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identificador da locadora (isolamento multi-tenant)
    @Column(name = "locadora_id", nullable = false)
    private Long locadoraId;

    // ID e nome do operador autenticado que executou a operação
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "usuario_nome", length = 160)
    private String usuarioNome;

    // Módulo afetado: LOCACAO, CLIENTE, EQUIPAMENTO, OPERADOR ou LOCADORA
    @Column(nullable = false, length = 50)
    private String modulo;

    // Tipo da operação: CADASTRO, CADASTRO_RETROATIVO, ATUALIZACAO, DEVOLUCAO, CANCELAMENTO, etc.
    @Column(nullable = false, length = 80)
    private String acao;

    // Indica se a operação foi um cadastro retroativo de locação (para destaque visual na auditoria)
    @Column(nullable = false)
    private boolean retroativo;

    // Resumo legível do que foi alterado ou criado
    @Column(nullable = false, length = 500)
    private String detalhes;

    // Momento exato em que a ação foi registrada no servidor
    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;
}
