package com.example.gearrent.service;

import com.example.gearrent.DTO.ClienteRequest;
import com.example.gearrent.DTO.ClienteResponse;
import org.springframework.aop.support.AopUtils;
import org.springframework.http.ResponseEntity;

public class ClienteService {
    ClienteResponse clienteResponse;
    public void verificarCampo(ClienteRequest clienteRequest) {
        try{
            if(clienteRequest != null){
                if(clienteRequest.nome() == null){
                    System.out.println("Nome não pode ser nulo");
                }else if (clienteRequest.cpf() == null){
                    System.out.println("CPF não pode ser nulo");
                }else if(clienteRequest.email() == null){
                    System.out.println("Email não pode ser nulo");
            } else if(clienteRequest.telefone() == null){
                    System.out.println("Telefone não pode ser nulo");
            }
            }
        } catch (Exception e) {
            System.out.println("Cadastro falhou");
        }
    }
}
