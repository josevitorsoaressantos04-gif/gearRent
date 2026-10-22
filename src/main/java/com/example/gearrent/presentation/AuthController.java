package com.example.gearrent.presentation;

import com.example.gearrent.application.DTO.EsqueciSenhaRequest;
import com.example.gearrent.application.DTO.EsqueciSenhaResponse;
import com.example.gearrent.application.DTO.LoginRequest;
import com.example.gearrent.application.DTO.LoginResponse;
import com.example.gearrent.application.DTO.RedefinirSenhaRequest;
import com.example.gearrent.application.DTO.RedefinirSenhaResponse;
import com.example.gearrent.application.services.AuthService;
import com.example.gearrent.application.services.RecuperacaoSenhaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RecuperacaoSenhaService recuperacaoSenhaService;

    public AuthController(AuthService authService, RecuperacaoSenhaService recuperacaoSenhaService) {
        this.authService = authService;
        this.recuperacaoSenhaService = recuperacaoSenhaService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/esqueci-senha")
    public ResponseEntity<EsqueciSenhaResponse> esqueciSenha(@Valid @RequestBody EsqueciSenhaRequest request) {
        return ResponseEntity.ok(recuperacaoSenhaService.solicitar(request));
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<RedefinirSenhaResponse> redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest request) {
        return ResponseEntity.ok(recuperacaoSenhaService.redefinir(request));
    }
}