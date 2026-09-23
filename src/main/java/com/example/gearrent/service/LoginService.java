package com.example.gearrent.service;

import com.example.gearrent.DTO.LoginRequest;
import com.example.gearrent.DTO.LoginResponse;
import com.example.gearrent.entities.Usuario;
import com.example.gearrent.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

    private final UsuarioRepository usuarioRepository;

    public LoginService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public LoginResponse autenticar(LoginRequest request) {
        // 1. Busca o usuário pelo e-mail
        Usuario usuario = usuarioRepository.findByEmail(request.login())
                .orElseThrow(() -> new RuntimeException("Credenciais inválidas."));

        // 2. Verifica se o usuário não foi inativado (Soft Delete)
        if (usuario.getAtivo() != null && !usuario.getAtivo()) {
            throw new RuntimeException("Acesso bloqueado: Usuário inativado.");
        }

        // 3. Comparação PROVISÓRIA em texto plano (sem criptografia)
        if (!usuario.getSenha().equals(request.senha())) {
            throw new RuntimeException("Credenciais inválidas.");
        }

        // 4. Retorna a resposta de sucesso
        return new LoginResponse(usuario.getLogin(), "Logado Com Sucesso");
    }
}