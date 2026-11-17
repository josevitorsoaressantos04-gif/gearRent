package com.example.gearrent.presentation;

import com.example.gearrent.application.DTO.LogAuditoriaResponse;
import com.example.gearrent.application.service.AuditoriaService;
import com.example.gearrent.infra.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller responsável pela consulta dos logs de auditoria da locadora (tenant).
 * Restrito a usuários com perfil ADMIN.
 */
@RestController
@RequestMapping("/api/auditoria")
@Tag(name = "Auditoria", description = "Logs de auditoria das operações realizadas na base (exclusivo para administradores)")
@SecurityRequirement(name = "bearerAuth")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Listar logs de auditoria da base",
            description = "Retorna os últimos 200 registros de auditoria da locadora autenticada, do mais recente para o mais antigo. "
                    + "Permite filtrar opcionalmente por módulo (LOCACAO, CLIENTE, EQUIPAMENTO, OPERADOR, LOCADORA) "
                    + "e destacar apenas cadastros retroativos de locações."
    )
    @ApiResponse(responseCode = "200", description = "Lista de logs de auditoria retornada com sucesso")
    @ApiResponse(responseCode = "401", description = "Sessão inválida ou expirada")
    @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores (ROLE_ADMIN)")
    public ResponseEntity<List<LogAuditoriaResponse>> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Parameter(description = "Módulo a filtrar (LOCACAO, CLIENTE, EQUIPAMENTO, OPERADOR, LOCADORA)")
            @RequestParam(required = false) String modulo,
            @Parameter(description = "Quando true, retorna apenas cadastros retroativos de locações")
            @RequestParam(required = false) Boolean apenasRetroativos) {
        return ResponseEntity.ok(auditoriaService.listar(usuario.locadoraId(), modulo, apenasRetroativos));
    }
}
