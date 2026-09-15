package com.example.gearrent.service;

import com.example.gearrent.DTO.EnderecoRequest;
import com.example.gearrent.DTO.EnderecoResponse;
import com.example.gearrent.entities.Cidade;
import com.example.gearrent.entities.Endereco;
import com.example.gearrent.repository.CidadeRepository;
import com.example.gearrent.repository.EnderecoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EnderecoService {

    private final EnderecoRepository enderecoRepository;
    private final CidadeRepository cidadeRepository;

    public EnderecoService(EnderecoRepository enderecoRepository, CidadeRepository cidadeRepository) {
        this.enderecoRepository = enderecoRepository;
        this.cidadeRepository = cidadeRepository;
    }

    @Transactional
    public EnderecoResponse criarEndereco(EnderecoRequest request) {
        // Busca a cidade pelo ID fornecido no DTO
        Cidade cidade = cidadeRepository.findById(request.cidadeId())
                .orElseThrow(() -> new RuntimeException("Cidade não encontrada com o ID: " + request.cidadeId()));

        // Converte o DTO para a Entidade JPA
        Endereco endereco = new Endereco();
        endereco.setLogradouro(request.logradouro());
        endereco.setNumero(request.numero());
        endereco.setCep(request.cep());
        endereco.setBairro(request.bairro());
        endereco.setCidade(cidade); // Define o relacionamento @ManyToOne

        Endereco enderecoSalvo = enderecoRepository.save(endereco);

        // Retorna o DTO de Resposta
        return new EnderecoResponse(
                enderecoSalvo.getId(),
                enderecoSalvo.getLogradouro(),
                enderecoSalvo.getNumero(),
                enderecoSalvo.getCep(),
                enderecoSalvo.getBairro(),
                cidade.getNome(),
                cidade.getUf()
        );
    }

    @Transactional(readOnly = true)
    public List<EnderecoResponse> listarTodos() {
        return enderecoRepository.findAll().stream()
                .map(e -> new EnderecoResponse(
                        e.getId(),
                        e.getLogradouro(),
                        e.getNumero(),
                        e.getCep(),
                        e.getBairro(),
                        e.getCidade().getNome(),
                        e.getCidade().getUf()
                ))
                .toList();
    }
}