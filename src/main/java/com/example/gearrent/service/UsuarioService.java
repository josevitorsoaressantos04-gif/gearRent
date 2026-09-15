package com.example.gearrent.service;

import com.example.gearrent.entities.Usuario;
import com.example.gearrent.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {
    @Autowired
    private UsuarioRepository usuarioRepository;

    public void deleteLogico(Long id){
        Usuario usuario = usuarioRepository.findById(id).orElseThrow(RuntimeException::new);
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
    }
}
