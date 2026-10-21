package com.example.gearrent.application.services;

import com.example.gearrent.application.DTO.LoginRequest;
import com.example.gearrent.application.DTO.LoginResponse;
import com.example.gearrent.domain.entities.Usuario;
import com.example.gearrent.domain.enums.StatusCadastro;
import com.example.gearrent.domain.exceptions.CredenciaisInvalidasException;
import com.example.gearrent.domain.repository.UsuarioRepository;
import com.example.gearrent.infra.security.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    @Autowired
    private final UsuarioRepository usuarioRepository;
    @Autowired
    private final PasswordEncoder passwordEncoder;
    @Autowired
    private final TokenService tokenService;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByLogin(request.email())
                .filter(u -> u.getStatus() == StatusCadastro.ATIVO)
                .filter(u -> passwordEncoder.matches(request.senha(), u.getSenha()))
                .orElseThrow(() -> new CredenciaisInvalidasException("E-mail ou senha incorretos."));

        return new LoginResponse("Login realizado com sucesso", tokenService.gerarToken(usuario));
    }
}