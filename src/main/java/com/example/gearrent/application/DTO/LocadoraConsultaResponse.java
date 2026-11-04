package com.example.gearrent.application.DTO;

import com.example.gearrent.domain.entities.Locadora;
import com.example.gearrent.domain.enums.StatusCadastro;

public record LocadoraConsultaResponse(
        Long id,
        String nomeFantasia,
        String cnpj,
        String telefone,
        EnderecoDTO endereco,
        StatusCadastro status) {

    public static LocadoraConsultaResponse de(Locadora l) {
        return new LocadoraConsultaResponse(
                l.getId(), l.getNomeFantasia(), l.getCnpj(), l.getTelefone(),
                EnderecoDTO.de(l.getEndereco()), l.getStatus());
    }
}