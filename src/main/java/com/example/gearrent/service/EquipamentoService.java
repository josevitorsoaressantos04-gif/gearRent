package com.example.gearrent.service;



import com.example.gearrent.entities.Equipamento;
import com.example.gearrent.repository.EquipamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


@Service
public class EquipamentoService {

    @Autowired
    private EquipamentoRepository equipamentoRepository;

    @Transactional
    public void deleteLogico(Long id) {
        Equipamento equipamento = equipamentoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Equipamento não encontrado com o ID: " + id));

        if (!Boolean.TRUE.equals(equipamento.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Este equipamento já está desativado.");
        }

        equipamento.setStatus(false);
        equipamentoRepository.save(equipamento);
    }
}
