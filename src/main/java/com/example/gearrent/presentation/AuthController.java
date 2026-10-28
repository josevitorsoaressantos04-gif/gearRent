package com.example.gearrent.presentation;

import com.example.gearrent.application.DTO.EsqueciSenhaRequest;
import com.example.gearrent.application.DTO.EsqueciSenhaResponse;
import com.example.gearrent.application.DTO.LoginRequest;
import com.example.gearrent.application.DTO.LoginResponse;
import com.example.gearrent.application.DTO.RedefinirSenhaRequest;
import com.example.gearrent.application.DTO.RedefinirSenhaResponse;
import com.example.gearrent.application.services.AuthService;
import com.example.gearrent.application.services.RecuperacaoSenhaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth Controller", description = "Endpoints públicos responsáveis pela autenticação de usuários (geração de token JWT) e pelo fluxo seguro de recuperação de senha.")
public class AuthController {

    private final AuthService authService;
    private final RecuperacaoSenhaService recuperacaoSenhaService;

    public AuthController(AuthService authService, RecuperacaoSenhaService recuperacaoSenhaService) {
        this.authService = authService;
        this.recuperacaoSenhaService = recuperacaoSenhaService;
    }

    @PostMapping("/login")
    @Operation(summary = "Realizar Login", description = "Autentica o usuário no sistema. Valida as credenciais e retorna o token JWT assinado para acesso às rotas protegidas. Em caso de falha, retorna intencionalmente uma mensagem genérica de erro (E-mail ou senha incorretos) para prevenir enumeração de contas.")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/esqueci-senha")
    @Operation(summary = "Solicitar Recuperação de Senha", description = "Gera e envia um token temporal de recuperação para o e-mail informado. Por segurança, a resposta da API é idêntica independentemente de o e-mail existir ou não na base de dados, garantindo privacidade.")
    public ResponseEntity<EsqueciSenhaResponse> esqueciSenha(@Valid @RequestBody EsqueciSenhaRequest request) {
        return ResponseEntity.ok(recuperacaoSenhaService.solicitar(request));
    }

    @PostMapping("/redefinir-senha")
    @Operation(summary = "Redefinir Senha", description = "Altera a senha do usuário com base no token recebido via e-mail. Regras de negócio: O token é validado quanto à sua existência, expiração (limite de 30 minutos) e se já foi utilizado anteriormente. A nova senha é imediatamente criptografada com BCrypt.")
    public ResponseEntity<RedefinirSenhaResponse> redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest request) {
        return ResponseEntity.ok(recuperacaoSenhaService.redefinir(request));
    }
}