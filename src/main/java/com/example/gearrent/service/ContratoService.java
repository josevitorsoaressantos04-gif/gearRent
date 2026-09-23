package com.example.gearrent.service;

import com.example.gearrent.entities.Contrato;
import com.example.gearrent.repository.ContratoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ContratoService {

    private final ContratoRepository contratoRepository;

    public ContratoService(ContratoRepository contratoRepository) {
        this.contratoRepository = contratoRepository;
    }

    @Transactional
    public void deleteLogico(Long id) {
        Contrato contrato = contratoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Contrato não encontrado com o ID: " + id));

        if (!Boolean.TRUE.equals(contrato.getStatusContrato())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Este contrato já está cancelado/desativado.");
        }

        contrato.setStatusContrato(false);
        contratoRepository.save(contrato);
    }
}
