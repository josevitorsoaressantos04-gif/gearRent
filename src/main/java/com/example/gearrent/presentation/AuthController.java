package com.example.gearrent.presentation;

import com.example.gearrent.application.DTO.EsqueciSenhaRequest;
import com.example.gearrent.application.DTO.EsqueciSenhaResponse;
import com.example.gearrent.application.DTO.LoginRequest;
import com.example.gearrent.application.DTO.LoginResponse;
import com.example.gearrent.application.DTO.RedefinirSenhaRequest;
import com.example.gearrent.application.DTO.RedefinirSenhaResponse;
import com.example.gearrent.application.service.AuthService;
import com.example.gearrent.application.service.RecuperacaoSenhaService;
import com.example.gearrent.infra.exception.ApiErroResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(
        name = "Autenticação",
        description = "Endpoints públicos para autenticação via token JWT e fluxo seguro de recuperação de senha."
)
@SecurityRequirements
public class AuthController {

    private final AuthService authService;
    private final RecuperacaoSenhaService recuperacaoSenhaService;

    public AuthController(AuthService authService, RecuperacaoSenhaService recuperacaoSenhaService) {
        this.authService = authService;
        this.recuperacaoSenhaService = recuperacaoSenhaService;
    }

    @PostMapping("/login")
    @Operation(
            summary = "Realizar login e emitir token JWT",
            description = "Autentica o usuário no sistema (endpoint público, não exige token JWT).\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. Valida o preenchimento obrigatório de e-mail e senha.\n"
                    + "2. Verifica a existência do usuário e compara a senha com o hash BCrypt armazenado.\n"
                    + "3. Confirma se o usuário está ativo na sua locadora (`locadoraId` será embutido nas claims do token).\n"
                    + "4. Emite o token JWT assinado para uso no cabeçalho `Authorization: Bearer <token>`.\n"
                    + "5. Em caso de falha, retorna mensagem genérica para evitar enumeração de contas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticação realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "401", description = "E-mail ou senha incorretos, ou usuário inativo",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/esqueci-senha")
    @Operation(
            summary = "Solicitar recuperação de senha",
            description = "Inicia o fluxo de redefinição de senha (endpoint público, não exige token JWT).\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. Valida o formato do e-mail informado.\n"
                    + "2. Caso o e-mail exista, gera um token temporário com validade de 30 minutos e envia o link por e-mail.\n"
                    + "3. Por segurança e privacidade, a resposta da API é idêntica independentemente de o e-mail existir na base."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitação processada com sucesso"),
            @ApiResponse(responseCode = "400", description = "E-mail inválido ou malformado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public ResponseEntity<EsqueciSenhaResponse> esqueciSenha(@Valid @RequestBody EsqueciSenhaRequest request) {
        return ResponseEntity.ok(recuperacaoSenhaService.solicitar(request));
    }

    @PostMapping("/redefinir-senha")
    @Operation(
            summary = "Redefinir senha com token de recuperação",
            description = "Conclui a alteração de senha usando o token recebido por e-mail (endpoint público, não exige token JWT).\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. Valida a existência do token de recuperação e se ele pertence a um usuário válido.\n"
                    + "2. Verifica se o token não expirou (limite de 30 minutos) e se ainda não foi utilizado.\n"
                    + "3. Criptografa a nova senha com BCrypt, atualiza o usuário e invalida o token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Senha redefinida com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou token de recuperação inválido, expirado ou já utilizado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public ResponseEntity<RedefinirSenhaResponse> redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest request) {
        return ResponseEntity.ok(recuperacaoSenhaService.redefinir(request));
    }
}