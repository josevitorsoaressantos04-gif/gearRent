package com.example.gearrent.presentation;

import com.example.gearrent.application.DTO.EquipamentoAtualizacaoRequest;
import com.example.gearrent.application.DTO.EquipamentoAtualizacaoResponse;
import com.example.gearrent.application.DTO.EquipamentoCadastroRequest;
import com.example.gearrent.application.DTO.EquipamentoCadastroResponse;
import com.example.gearrent.application.DTO.EquipamentoConsultaResponse;
import com.example.gearrent.application.DTO.EquipamentoDisponivelResponse;
import com.example.gearrent.application.DTO.EquipamentoExclusaoResponse;
import com.example.gearrent.application.DTO.EquipamentoListagemResponse;
import com.example.gearrent.application.DTO.PaginaResponse;
import com.example.gearrent.application.service.EquipamentoService;
import com.example.gearrent.domain.enums.CategoriaEquipamento;
import com.example.gearrent.domain.enums.StatusEquipamento;
import com.example.gearrent.infra.exception.ApiErroResposta;
import com.example.gearrent.infra.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipamentos")
@Tag(
        name = "Equipamentos",
        description = "Gerenciamento do catálogo e estoque de equipamentos da locadora, com filtros, paginação e isolamento por tenant."
)
@SecurityRequirement(name = "bearerAuth")
public class EquipamentoController {

    private final EquipamentoService equipamentoService;

    public EquipamentoController(EquipamentoService equipamentoService) {
        this.equipamentoService = equipamentoService;
    }

    @PostMapping
    @Operation(
            summary = "Cadastrar equipamento",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`).\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` nunca é enviado pelo cliente HTTP, sendo resolvido internamente a partir do usuário autenticado.\n"
                    + "2. Valida campos obrigatórios e unicidade do número de patrimônio dentro da locadora autenticada.\n"
                    + "3. Gera código sequencial na base, registra o equipamento com status inicial apropriado (ex.: `DISPONIVEL`) e grava log de auditoria."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Equipamento cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "409", description = "Número de patrimônio já cadastrado nesta locadora",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public ResponseEntity<EquipamentoCadastroResponse> cadastrar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody EquipamentoCadastroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(equipamentoService.cadastrar(usuario.locadoraId(), request));
    }

    @GetMapping
    @Operation(
            summary = "Listar equipamentos com filtros e paginação",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`).\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` nunca é enviado pelo cliente HTTP, sendo resolvido internamente a partir do usuário autenticado.\n"
                    + "2. Aplica filtros opcionais por nome, categoria e status do equipamento, com paginação (padrão: 10 itens ordenados por nome)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de equipamentos retornada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros de filtro ou paginação inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public ResponseEntity<PaginaResponse<EquipamentoListagemResponse>> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Parameter(description = "Filtro opcional por nome (busca parcial)") @RequestParam(required = false) String nome,
            @Parameter(description = "Filtro opcional por categoria do equipamento") @RequestParam(required = false) CategoriaEquipamento categoria,
            @Parameter(description = "Filtro opcional por status do equipamento") @RequestParam(required = false) StatusEquipamento status,
            @ParameterObject @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(equipamentoService.listar(
                usuario.locadoraId(), nome, categoria, status, pageable));
    }

    @GetMapping("/disponiveis")
    @Operation(
            summary = "Listar equipamentos disponíveis para locação",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`).\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` nunca é enviado pelo cliente HTTP, sendo resolvido internamente a partir do usuário autenticado.\n"
                    + "2. Filtra exclusivamente equipamentos da locadora atual com status `DISPONIVEL`, com busca opcional por nome."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de equipamentos disponíveis retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public ResponseEntity<List<EquipamentoDisponivelResponse>> listarDisponiveis(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Parameter(description = "Filtro opcional por nome") @RequestParam(required = false, defaultValue = "") String nome) {
        return ResponseEntity.ok(equipamentoService.listarDisponiveis(usuario.locadoraId(), nome));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar equipamento por ID",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`).\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` nunca é enviado pelo cliente HTTP, sendo resolvido internamente a partir do usuário autenticado.\n"
                    + "2. Checa a existência do equipamento e valida o isolamento multi-tenant na locadora autenticada."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Equipamento encontrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Identificador inválido",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "404", description = "Equipamento não encontrado na locadora autenticada",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public ResponseEntity<EquipamentoConsultaResponse> buscarPorId(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Parameter(description = "ID do equipamento", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(equipamentoService.buscarPorId(usuario.locadoraId(), id));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar equipamento",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`).\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` nunca é enviado pelo cliente HTTP, sendo resolvido internamente a partir do usuário autenticado.\n"
                    + "2. Checa a existência do equipamento na locadora autenticada.\n"
                    + "3. Bloqueia a alteração caso o equipamento esteja com status `ALUGADO` para preservar contratos em andamento.\n"
                    + "4. Valida unicidade de patrimônio na locadora e registra a alteração na auditoria."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Equipamento atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "404", description = "Equipamento não encontrado na locadora autenticada",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "409", description = "Número de patrimônio já utilizado por outro equipamento da locadora",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "422", description = "Equipamento com status ALUGADO não pode ser alterado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public ResponseEntity<EquipamentoAtualizacaoResponse> atualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Parameter(description = "ID do equipamento", example = "1") @PathVariable Long id,
            @Valid @RequestBody EquipamentoAtualizacaoRequest request) {
        return ResponseEntity.ok(equipamentoService.atualizar(usuario.locadoraId(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Excluir equipamento",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`).\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` nunca é enviado pelo cliente HTTP, sendo resolvido internamente a partir do usuário autenticado.\n"
                    + "2. Checa a existência do equipamento na locadora autenticada.\n"
                    + "3. Bloqueia a exclusão caso o equipamento esteja com status `ALUGADO`.\n"
                    + "4. Realiza a exclusão lógica do equipamento e registra na auditoria."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Equipamento excluído com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "404", description = "Equipamento não encontrado na locadora autenticada",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "422", description = "Equipamento com status ALUGADO não pode ser excluído",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public ResponseEntity<EquipamentoExclusaoResponse> excluir(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Parameter(description = "ID do equipamento", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(equipamentoService.excluir(usuario.locadoraId(), id));
    }
}