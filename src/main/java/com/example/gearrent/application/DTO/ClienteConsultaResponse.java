package com.example.gearrent.application.DTO;

import com.example.gearrent.domain.entities.Cliente;
import com.example.gearrent.domain.enums.StatusCadastro;

public record ClienteConsultaResponse(
        Long id,
        String nome,
        String documento,
        String email,
        String celular,
        String telefone,
        EnderecoDTO endereco,
        StatusCadastro status
) {
    public static ClienteConsultaResponse de(Cliente cliente) {
        return new ClienteConsultaResponse(
                cliente.getId(),
                cliente.getNome(),
                cliente.getDocumento(),
                cliente.getEmail(),
                cliente.getCelular(),
                cliente.getTelefone(),
                EnderecoDTO.de(cliente.getEnderecoResidencial()),
                cliente.getStatus());
    }
}