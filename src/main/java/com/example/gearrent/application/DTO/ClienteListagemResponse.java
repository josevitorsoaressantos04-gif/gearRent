package com.example.gearrent.application.DTO;

import com.example.gearrent.domain.entities.Cliente;
import com.example.gearrent.domain.enums.StatusCadastro;

public record ClienteListagemResponse(
        Long id,
        Long codigoSequencial,
        String nome,
        String documento,
        String celular,
        String email,
        StatusCadastro status
) {
    public static ClienteListagemResponse de(Cliente cliente) {
        return new ClienteListagemResponse(
                cliente.getId(),
                cliente.getCodigoSequencial(),
                cliente.getNome(),
                cliente.getDocumento(),
                cliente.getCelular(),
                cliente.getEmail(),
                cliente.getStatus());
    }
}