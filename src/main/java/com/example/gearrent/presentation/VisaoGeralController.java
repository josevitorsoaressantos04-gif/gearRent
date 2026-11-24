package com.example.gearrent.presentation;

import com.example.gearrent.application.DTO.*;
import com.example.gearrent.application.service.VisaoGeralQueryService;
import com.example.gearrent.infra.exception.ApiErroResposta;
import com.example.gearrent.infra.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/visao-geral")
@RequiredArgsConstructor
@Tag(
        name = "Visão Geral",
        description = "Consultas otimizadas para o painel administrativo (BFF/CQRS somente leitura), com isolamento por locadora."
)
@SecurityRequirement(name = "bearerAuth")
public class VisaoGeralController {

    private final VisaoGeralQueryService queryService;

    @Operation(
            summary = "Consultar KPIs consolidados da locadora",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`).\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` nunca é enviado pelo cliente HTTP, sendo resolvido internamente a partir do usuário autenticado.\n"
                    + "2. Agrega em consulta única os indicadores do topo do dashboard: total de locações, em andamento, "
                    + "atrasadas, faturamento total, receita acumulada do mês, valores em aberto e contagem de equipamentos por status "
                    + "(disponíveis, alugados e em manutenção)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Indicadores retornados com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    @GetMapping("/resumo")
    public ResponseEntity<VisaoGeralResumoDTO> resumo(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(queryService.buscarResumo(usuario.locadoraId()));
    }

    @Operation(
            summary = "Listar opções agregadas para filtros do dashboard",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`).\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` nunca é enviado pelo cliente HTTP, sendo resolvido internamente a partir do usuário autenticado.\n"
                    + "2. Retorna em uma única chamada as categorias e modelos de equipamentos cadastrados e a lista enxuta "
                    + "de clientes (id, código sequencial e nome) pertencentes exclusivamente à locadora autenticada."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções de filtro retornadas com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    @GetMapping("/filtros-opcoes")
    public ResponseEntity<OpcoesFiltrosDTO> filtrosOpcoes(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(queryService.buscarOpcoesFiltros(usuario.locadoraId()));
    }

    @Operation(
            summary = "Listar locações com filtros combinados e paginação",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`).\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` nunca é enviado pelo cliente HTTP, sendo resolvido internamente a partir do usuário autenticado.\n"
                    + "2. Valida o período informado (`dataInicio` não pode ser posterior a `dataFim`).\n"
                    + "3. Aplica todos os filtros opcionais combinados (`status`, `statusEquipamento`, `clienteId`, `categoria`, "
                    + "`modelo`, `numeroPatrimonio`, `dataInicio`, `dataFim` sobre a data de retirada).\n"
                    + "4. Ordenação permitida: `dataRetirada`, `dataPrevista`, `status` e `valorFinalCobrado` "
                    + "(padrão: `dataRetirada` decrescente). Tamanho máximo da página: 50."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de locações retornada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros de filtro ou paginação inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "422", description = "Período inválido (dataInicio posterior a dataFim)",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    @GetMapping("/locacoes")
    public ResponseEntity<Page<LocacaoListagemSinteticaDTO>> locacoes(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @ParameterObject FiltroLocacaoDTO filtro,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(queryService.listarLocacoes(usuario.locadoraId(), filtro, pageable));
    }
}