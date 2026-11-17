package com.example.gearrent.domain.repository;

import com.example.gearrent.domain.entities.Equipamento;
import com.example.gearrent.domain.enums.CategoriaEquipamento;
import com.example.gearrent.domain.enums.StatusEquipamento;
import org.springframework.data.jpa.domain.Specification;

public final class EquipamentoSpecifications {

    private EquipamentoSpecifications() {
    }

    // Sempre a primeira condição: isolamento do tenant
    public static Specification<Equipamento> daLocadora(Long locadoraId) {
        return (root, query, cb) -> cb.equal(root.get("locadora").get("id"), locadoraId);
    }

    public static Specification<Equipamento> comNome(String nome) {
        String padrao = "%" + escapar(nome.trim().toLowerCase()) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("nome")), padrao, '\\');
    }

    public static Specification<Equipamento> comCategoria(CategoriaEquipamento categoria) {
        return (root, query, cb) -> cb.equal(root.get("categoria"), categoria);
    }

    public static Specification<Equipamento> comStatus(StatusEquipamento status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public static Specification<Equipamento> naoCancelado() {
        return (root, query, cb) ->
                cb.notEqual(root.get("status"), StatusEquipamento.CANCELADO);
    }
}