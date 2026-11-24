package com.example.gearrent.presentation;

import com.example.gearrent.application.DTO.LocadoraAtualizacaoRequest;
import com.example.gearrent.application.DTO.LocadoraConsultaResponse;
import com.example.gearrent.application.service.LocadoraService;
import com.example.gearrent.infra.exception.ApiErroResposta;
import com.example.gearrent.infra.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/locadora")
@Tag(
        name = "Locadora",
        description = "Consulta e atualização dos dados cadastrais da locadora (tenant) vinculada ao usuário autenticado."
)
@SecurityRequirement(name = "bearerAuth")
public class LocadoraController {

    private final LocadoraService locadoraService;

    public LocadoraController(LocadoraService locadoraService) {
        this.locadoraService = locadoraService;
    }

    @GetMapping
    @Operation(
            summary = "Consultar dados da locadora autenticada",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`).\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` nunca é enviado pelo cliente HTTP, sendo resolvido internamente a partir do usuário autenticado.\n"
                    + "2. Busca e retorna os dados cadastrais da locadora (razão social/nome fantasia, CNPJ, telefone e endereço)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados da locadora retornados com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "404", description = "Locadora não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public LocadoraConsultaResponse consultar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return locadoraService.consultar(usuario.locadoraId());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Atualizar dados da locadora autenticada",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`) e perfil `ROLE_ADMIN`.\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` nunca é enviado pelo cliente HTTP, sendo resolvido internamente a partir do usuário autenticado.\n"
                    + "2. Valida que o usuário possui permissão `ROLE_ADMIN`.\n"
                    + "3. Atualiza nome fantasia, telefone e endereço da locadora; o CNPJ é imutável após o cadastro.\n"
                    + "4. Registra a alteração nos logs de auditoria da base."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados da locadora atualizados com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores (ROLE_ADMIN)",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "404", description = "Locadora não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public LocadoraConsultaResponse atualizar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                              @Valid @RequestBody LocadoraAtualizacaoRequest request) {
        return locadoraService.atualizar(usuario.locadoraId(), request);
    }
}