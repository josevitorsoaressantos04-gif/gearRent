package com.example.gearrent.presentation;

import com.example.gearrent.application.DTO.ClienteAtualizacaoRequest;
import com.example.gearrent.application.DTO.ClienteAtualizacaoResponse;
import com.example.gearrent.application.DTO.ClienteCadastroRequest;
import com.example.gearrent.application.DTO.ClienteCadastroResponse;
import com.example.gearrent.application.DTO.ClienteConsultaResponse;
import com.example.gearrent.application.DTO.ClienteExclusaoResponse;
import com.example.gearrent.application.DTO.ClienteHistoricoLocacaoDTO;
import com.example.gearrent.application.DTO.ClienteResumoDTO;
import com.example.gearrent.application.DTO.ClienteListagemResponse;
import com.example.gearrent.application.service.ClienteService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@Tag(
        name = "Clientes",
        description = "Gerenciamento de clientes (Pessoa Física e Jurídica) da locadora, incluindo histórico, resumo financeiro e reativação cadastral."
)
@SecurityRequirement(name = "bearerAuth")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    @Operation(
            summary = "Cadastrar ou reativar cliente",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`).\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` nunca é enviado pelo cliente HTTP, sendo resolvido internamente a partir do usuário autenticado.\n"
                    + "2. Valida formato de CPF (11 dígitos) ou CNPJ (14 dígitos) conforme o tipo de pessoa.\n"
                    + "3. Verifica o documento na locadora autenticada: se houver cliente `ATIVO`, retorna `409 Conflict`.\n"
                    + "4. Se existir cliente removido (`CANCELADO`/`INATIVO`) com o mesmo documento, reativa o cadastro preservando ID e código sequencial; caso contrário, cria um novo registro."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente cadastrado ou reativado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "409", description = "Já existe um cliente ativo com este CPF/CNPJ nesta locadora",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public ResponseEntity<ClienteCadastroResponse> cadastrar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ClienteCadastroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clienteService.cadastrar(request, usuario.locadoraId()));
    }

    @GetMapping
    @Operation(
            summary = "Listar clientes da locadora",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`).\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` nunca é enviado pelo cliente HTTP, sendo resolvido internamente a partir do usuário autenticado.\n"
                    + "2. Retorna os clientes não removidos da locadora autenticada, com filtro opcional por parte do nome (case-insensitive)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de clientes retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public ResponseEntity<List<ClienteListagemResponse>> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Parameter(description = "Filtro opcional por parte do nome") @RequestParam(required = false, defaultValue = "") String nome) {
        return ResponseEntity.ok(clienteService.listar(usuario.locadoraId(), nome));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar cliente por ID",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`).\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` nunca é enviado pelo cliente HTTP, sendo resolvido internamente a partir do usuário autenticado.\n"
                    + "2. Checa a existência do cliente e valida o isolamento multi-tenant na locadora autenticada."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Identificador inválido",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado na locadora autenticada",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public ResponseEntity<ClienteConsultaResponse> buscarPorId(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Parameter(description = "ID do cliente", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(clienteService.consultar(usuario.locadoraId(), id));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar cliente",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`).\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` nunca é enviado pelo cliente HTTP, sendo resolvido internamente a partir do usuário autenticado.\n"
                    + "2. Checa a existência do cliente na locadora autenticada.\n"
                    + "3. Atualiza nome, tipo de pessoa, CPF/CNPJ, contatos e endereço (tipo de pessoa e documento são opcionais e mantidos quando omitidos).\n"
                    + "4. Valida tamanho do documento (11 para PF, 14 para PJ) e impede duplicidade com outro cliente da mesma locadora."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado na locadora autenticada",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "409", description = "CPF/CNPJ já utilizado por outro cliente da mesma locadora",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public ResponseEntity<ClienteAtualizacaoResponse> atualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Parameter(description = "ID do cliente", example = "1") @PathVariable Long id,
            @Valid @RequestBody ClienteAtualizacaoRequest request) {
        return ResponseEntity.ok(clienteService.atualizar(usuario.locadoraId(), id, request));
    }

    @GetMapping("/{id}/resumo")
    @Operation(
            summary = "Consultar resumo de locações do cliente",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`).\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` nunca é enviado pelo cliente HTTP, sendo resolvido internamente a partir do usuário autenticado.\n"
                    + "2. Checa a existência do cliente na locadora autenticada.\n"
                    + "3. Consolida total de locações (excluindo canceladas), locações em andamento/atrasadas, equipamentos em posse e valor total em aberto."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumo do cliente retornado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado na locadora autenticada",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public ResponseEntity<ClienteResumoDTO> resumo(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Parameter(description = "ID do cliente", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(clienteService.resumo(usuario.locadoraId(), id));
    }

    @GetMapping("/{id}/historico")
    @Operation(
            summary = "Consultar histórico de locações do cliente",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`).\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` nunca é enviado pelo cliente HTTP, sendo resolvido internamente a partir do usuário autenticado.\n"
                    + "2. Checa a existência do cliente na locadora autenticada.\n"
                    + "3. Lista todas as locações do cliente da mais recente para a mais antiga, incluindo os nomes dos equipamentos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico de locações retornado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado na locadora autenticada",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public ResponseEntity<List<ClienteHistoricoLocacaoDTO>> historico(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Parameter(description = "ID do cliente", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(clienteService.historico(usuario.locadoraId(), id));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Excluir cliente (exclusão lógica)",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`).\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` nunca é enviado pelo cliente HTTP, sendo resolvido internamente a partir do usuário autenticado.\n"
                    + "2. Checa a existência do cliente na locadora autenticada.\n"
                    + "3. Bloqueia a exclusão caso o cliente possua locações com status `EM_ANDAMENTO` ou `ATRASADA`.\n"
                    + "4. Aplica exclusão lógica (status `CANCELADO`), preservando histórico de locações e faturamento."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente excluído logicamente com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado na locadora autenticada",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "422", description = "Cliente possui locações em andamento ou atrasadas",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public ResponseEntity<ClienteExclusaoResponse> excluir(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Parameter(description = "ID do cliente", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(clienteService.excluir(usuario.locadoraId(), id));
    }
}