package com.example.gearrent.application.service;

import com.example.gearrent.application.DTO.LocadoraAtualizacaoRequest;
import com.example.gearrent.application.DTO.LocadoraConsultaResponse;
import com.example.gearrent.domain.entities.Locadora;
import com.example.gearrent.domain.exception.RecursoNaoEncontradoException;
import com.example.gearrent.domain.repository.LocadoraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class LocadoraService {

    private final LocadoraRepository locadoraRepository;
    private final Clock clock;

    public LocadoraService(LocadoraRepository locadoraRepository, Clock clock) {
        this.locadoraRepository = locadoraRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public LocadoraConsultaResponse consultar(Long locadoraId) {
        return LocadoraConsultaResponse.de(buscar(locadoraId));
    }

    @Transactional
    public LocadoraConsultaResponse atualizar(Long locadoraId, LocadoraAtualizacaoRequest req) {
        Locadora locadora = buscar(locadoraId);

        String nomeFantasia = req.nomeFantasia() == null || req.nomeFantasia().isBlank()
                ? req.nome()
                : req.nomeFantasia();
        locadora.setNomeFantasia(nomeFantasia.trim());
        locadora.setTelefone(req.telefone().trim());
        if (req.tema() != null) {
            locadora.setTema(req.tema().trim());
        }
        locadora.setEndereco(req.endereco().paraEntidade());
        locadora.setDataAtualizacao(LocalDateTime.now(clock));

        return LocadoraConsultaResponse.de(locadora);
    }

    private Locadora buscar(Long locadoraId) {
        return locadoraRepository.findById(locadoraId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sua base não foi encontrada."));
    }
}