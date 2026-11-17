package com.example.gearrent.application.DTO;

import com.example.gearrent.domain.entities.Cliente;
import com.example.gearrent.domain.enums.StatusCadastro;
import com.example.gearrent.domain.enums.TipoPessoa;

public record ClienteConsultaResponse(
        Long id,
        Long codigoSequencial,
        String nome,
        TipoPessoa tipoPessoa,
        String documento,
        String email,
        String celular,
        String telefone,
        EnderecoDTO endereco,
        StatusCadastro status
) {
    public static ClienteConsultaResponse de(Cliente cliente) {
        // Clientes antigos não têm o tipo gravado: deduz pelo tamanho do documento (11 = CPF, 14 = CNPJ)
        TipoPessoa tipo = cliente.getTipoPessoa() != null
                ? cliente.getTipoPessoa()
                : TipoPessoa.deduzirDoDocumento(cliente.getDocumento());

        return new ClienteConsultaResponse(
                cliente.getId(),
                cliente.getCodigoSequencial(),
                cliente.getNome(),
                tipo,
                cliente.getDocumento(),
                cliente.getEmail(),
                cliente.getCelular(),
                cliente.getTelefone(),
                EnderecoDTO.de(cliente.getEnderecoResidencial()),
                cliente.getStatus());
    }
}
