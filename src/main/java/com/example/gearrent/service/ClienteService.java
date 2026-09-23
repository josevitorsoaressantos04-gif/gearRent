package com.example.gearrent.service;


import com.example.gearrent.entities.Cliente;
import com.example.gearrent.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClienteService{
    @Autowired
    private ClienteRepository clienteRepository;

    @Transactional
    public void deleteLogico(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Cliente não encontrado com o ID: " + id));

        if (!Boolean.TRUE.equals(cliente.getAtivo())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Este cliente já está desativado.");
        }

        cliente.setAtivo(false);
        clienteRepository.save(cliente);
    }
}
