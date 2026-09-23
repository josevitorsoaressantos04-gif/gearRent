package com.example.gearrent.service;

import com.example.gearrent.entities.Usuario;
import com.example.gearrent.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public void deleteLogico(Long id) {
        // 1. Busca o objeto no banco ou lança 400 caso não exista
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Objeto não encontrado com o ID: " + id));

        // 2. Valida se o status já é false
        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Este registro já está desativado.");
        }

        // 3. Aplica o soft delete e altera para false
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
    }
}