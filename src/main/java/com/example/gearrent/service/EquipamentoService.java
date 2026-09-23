package com.example.gearrent.service;

import com.example.gearrent.entities.Empresa;
import com.example.gearrent.entities.Equipamento;
import com.example.gearrent.repository.EquipamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EquipamentoService {

    @Autowired
    private EquipamentoRepository equipamentoRepository;

    public void deleteLogico(Long id){
        Equipamento equipamento = equipamentoRepository.findById(id).orElseThrow(RuntimeException::new);
        equipamento.setStatus(false);
        equipamentoRepository.save(equipamento);
    }
}
