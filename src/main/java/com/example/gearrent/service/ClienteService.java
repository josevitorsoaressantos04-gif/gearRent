package com.example.gearrent.service;


import com.example.gearrent.entities.Cliente;
import com.example.gearrent.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClienteService{
    @Autowired
    private ClienteRepository clienteRepository;

    public void deleteLogico(Long id){
        Cliente cliente = clienteRepository.findById(id).orElseThrow(RuntimeException::new);
        cliente.setAtivo(false);
        clienteRepository.save(cliente);
    }
}
