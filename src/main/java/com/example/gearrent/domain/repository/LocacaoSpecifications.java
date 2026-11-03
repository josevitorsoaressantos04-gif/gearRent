package com.example.gearrent.domain.repository;

import com.example.gearrent.application.DTO.FiltroLocacaoDTO;
import com.example.gearrent.domain.entities.Equipamento;
import com.example.gearrent.domain.entities.Locacao;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class LocacaoSpecifications {

    private LocacaoSpecifications() {}

    /** Ponto de entrada: o locadoraId é OBRIGATÓRIO e sempre vem primeiro. */
    public static Specification<Locacao> comFiltros(Long locadoraId, FiltroLocacaoDTO f) {
        Objects.requireNonNull(locadoraId, "locadoraId é obrigatório em toda consulta");

        List<Specification<Locacao>> specs = new ArrayList<>();
        specs.add(daLocadora(locadoraId));   // regra inegociável: multi-tenant
        specs.add(clienteCarregado());       // fetch do cliente (evita N+1)

        // Filtros opcionais: só entram na lista se foram informados
        adicionarSeExistir(specs, comStatus(f));
        adicionarSeExistir(specs, doCliente(f));
        adicionarSeExistir(specs, comEquipamento(locadoraId, f));
        adicionarSeExistir(specs, iniciadaNoPeriodo(f));

        return Specification.allOf(specs);
    }

    private static void adicionarSeExistir(List<Specification<Locacao>> lista, Specification<Locacao> spec) {
        if (spec != null) {
            lista.add(spec);
        }
    }

    static Specification<Locacao> daLocadora(Long locadoraId) {
        return (root, query, cb) -> cb.equal(root.get("locadora").get("id"), locadoraId);
    }

    // Fetch do cliente (ToOne); pulado na query de count do Page
    static Specification<Locacao> clienteCarregado() {
        return (root, query, cb) -> {
            Class<?> tipo = query.getResultType();
            if (!Long.class.equals(tipo) && !long.class.equals(tipo)) {
                root.fetch("cliente", JoinType.INNER);
            }
            return cb.conjunction();
        };
    }

    static Specification<Locacao> comStatus(FiltroLocacaoDTO f) {
        if (f.status() == null) return null;
        return (root, query, cb) -> cb.equal(root.get("status"), f.status());
    }

    static Specification<Locacao> doCliente(FiltroLocacaoDTO f) {
        if (f.clienteId() == null) return null;
        return (root, query, cb) -> cb.equal(root.get("cliente").get("id"), f.clienteId());
    }

    // categoria, modelo e patrimônio compartilham UM join com equipamentos
    static Specification<Locacao> comEquipamento(Long locadoraId, FiltroLocacaoDTO f) {
        boolean semFiltro = f.categoria() == null
                && !temTexto(f.modelo())
                && !temTexto(f.numeroPatrimonio());
        if (semFiltro) return null;

        return (root, query, cb) -> {
            query.distinct(true);
            Join<Locacao, Equipamento> eq = root.join("equipamentos", JoinType.INNER);

            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(eq.get("locadora").get("id"), locadoraId)); // defesa em profundidade
            if (f.categoria() != null) {
                p.add(cb.equal(eq.get("categoria"), f.categoria()));
            }
            if (temTexto(f.modelo())) {
                p.add(cb.equal(cb.lower(eq.get("modelo")), f.modelo().trim().toLowerCase()));
            }
            if (temTexto(f.numeroPatrimonio())) {
                p.add(cb.like(cb.lower(eq.get("numeroPatrimonio")),
                        "%" + f.numeroPatrimonio().trim().toLowerCase() + "%"));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    // Locações cuja RETIRADA está dentro do intervalo (o dia de dataFim entra por completo)
    static Specification<Locacao> iniciadaNoPeriodo(FiltroLocacaoDTO f) {
        if (f.dataInicio() == null && f.dataFim() == null) return null;
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.dataInicio() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("dataRetirada"), f.dataInicio().atStartOfDay()));
            }
            if (f.dataFim() != null) {
                p.add(cb.lessThan(root.get("dataRetirada"), f.dataFim().plusDays(1).atStartOfDay()));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    private static boolean temTexto(String s) {
        return s != null && !s.isBlank();
    }
}