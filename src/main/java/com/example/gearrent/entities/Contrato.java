package com.example.gearrent.entities; // <--- Deve ser exatamente este pacote

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tb_contrato")
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @OneToMany
    private List<Equipamento> equipamento;

    private LocalDateTime dataRetirada;
    private LocalDateTime dataDevolucaoPrevista;
    private LocalDateTime dataDevolucaoReal;
    private double valorAcordado;
    private boolean statusContrato;
}