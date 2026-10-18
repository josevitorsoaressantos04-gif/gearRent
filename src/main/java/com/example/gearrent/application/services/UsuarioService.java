package com.example.gearrent.application.services;

import com.example.gearrent.application.DTO.UsuarioCadastroRequest;
import com.example.gearrent.application.DTO.UsuarioCadastroResponse;
import com.example.gearrent.domain.entities.Usuario;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UsuarioService {
    public UsuarioCadastroResponse(UsuarioCadastroRequest request){

         Usuario usuario = new Usuario();
         usuario.setNome(request.nome());
         usuario.setLogin(request.email());
         usuario.setSenha(request.senha());
         usuario.setData_nascimento(request.dataNascimento());
         usuario.setDataCadastro(LocalDateTime.now());
         return usuario;



    }
}
