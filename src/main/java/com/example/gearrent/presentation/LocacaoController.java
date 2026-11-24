package com.example.gearrent.presentation;

import com.example.gearrent.application.DTO.LocacaoAtualizacaoRequest;
import com.example.gearrent.application.DTO.LocacaoCadastroRequest;
import com.example.gearrent.application.DTO.LocacaoDevolucaoRequest;
import com.example.gearrent.application.DTO.LocacaoResponse;
import com.example.gearrent.application.service.LocacaoService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/locacoes")
@Tag(
        name = "Locações",
        description = "Gerenciamento completo de contratos de locação (abertura, consulta, edição, devolução e cancelamento) com isolamento por locadora."
)
@SecurityRequirement(name = "bearerAuth")
public class LocacaoController {

    private final LocacaoService locacaoService;

    public LocacaoController(LocacaoService locacaoService) {
        this.locacaoService = locacaoService;
    }

    @PostMapping
    @Operation(
            summary = "Cadastrar locação (inclusive retroativa)",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`).\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` nunca é enviado pelo cliente HTTP, sendo resolvido internamente a partir do usuário autenticado.\n"
                    + "2. Valida que a data prevista de devolução é posterior à data de retirada.\n"
                    + "3. Checa a existência do cliente na locadora autenticada e exige status `ATIVO`.\n"
                    + "4. Ordena os IDs e aplica bloqueio pessimista (`lock`) nos equipamentos para evitar locação concorrente, validando disponibilidade.\n"
                    + "5. Gera código sequencial atômico por locadora, calcula diárias previstas, desconto e eventual multa retroativa (10% sobre diárias em atraso quando a data prevista já passou, nascendo com status `ATRASADA`).\n"
                    + "6. Registra evento na auditoria (destacando cadastros retroativos)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Locação criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "404", description = "Cliente ou equipamento não encontrado na locadora autenticada",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "409", description = "Um ou mais equipamentos indisponíveis para locação",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "422", description = "Regra de negócio violada (cliente inativo, desconto inválido ou data prevista anterior/igual à retirada)",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public ResponseEntity<LocacaoResponse> cadastrar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody LocacaoCadastroRequest request) {

        LocacaoResponse response = locacaoService.cadastrar(usuario.locadoraId(), usuario.id(), request);
        return ResponseEntity.created(URI.create("/api/locacoes/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Consultar locação por ID",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`).\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` nunca é enviado pelo cliente HTTP, sendo resolvido internamente a partir do usuário autenticado.\n"
                    + "2. Checa a existência do contrato dentro da locadora autenticada (isolamento multi-tenant).\n"
                    + "3. Caso a locação esteja em atraso, calcula e exibe o valor acumulado atualizado até a data corrente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Locação encontrada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Identificador inválido",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "404", description = "Locação não encontrada na locadora autenticada",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public LocacaoResponse consultar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Parameter(description = "ID da locação", example = "1") @PathVariable Long id) {
        return locacaoService.consultar(usuario.locadoraId(), id);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Editar locação em andamento",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`).\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` nunca é enviado pelo cliente HTTP, sendo resolvido internamente a partir do usuário autenticado.\n"
                    + "2. Checa a existência da locação na locadora autenticada e exige que o status seja `EM_ANDAMENTO`.\n"
                    + "3. Atualiza data prevista de devolução, desconto, responsável pela retirada e endereço de entrega (campos omitidos são preservados).\n"
                    + "4. Recalcula automaticamente diárias e eventual multa caso a data prevista ou o desconto sejam alterados."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Locação atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "404", description = "Locação não encontrada na locadora autenticada",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "422", description = "Locação não está em andamento ou regra de negócio violada",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public LocacaoResponse atualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Parameter(description = "ID da locação", example = "1") @PathVariable Long id,
            @Valid @RequestBody LocacaoAtualizacaoRequest request) {
        return locacaoService.atualizar(usuario.locadoraId(), id, request);
    }

    @PatchMapping("/{id}/devolucao")
    @Operation(
            summary = "Registrar devolução de locação",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`).\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` nunca é enviado pelo cliente HTTP, sendo resolvido internamente a partir do usuário autenticado.\n"
                    + "2. Checa a existência da locação na locadora autenticada e valida se pode ser devolvida.\n"
                    + "3. Utiliza a `dataDevolucao` informada no corpo (opcional; padrão: data atual), calcula diárias efetivas, aplica multa de atraso quando cabível e fixa o valor final.\n"
                    + "4. Libera os equipamentos vinculados para o status `DISPONIVEL` e registra a operação na auditoria."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Devolução registrada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "404", description = "Locação não encontrada na locadora autenticada",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "422", description = "Locação já concluída/cancelada ou data de devolução inválida",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public LocacaoResponse devolver(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Parameter(description = "ID da locação", example = "1") @PathVariable Long id,
            @Valid @RequestBody(required = false) LocacaoDevolucaoRequest request) {
        return locacaoService.devolver(usuario.locadoraId(), id, request);
    }

    // Contrato oficial usado pela interface: PATCH /api/locacoes/{id}/cancelamento
    @PatchMapping("/{id}/cancelamento")
    @Operation(
            summary = "Cancelar locação",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`).\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` nunca é enviado pelo cliente HTTP, sendo resolvido internamente a partir do usuário autenticado.\n"
                    + "2. Checa a existência da locação na locadora autenticada.\n"
                    + "3. Permite cancelamento apenas enquanto a locação estiver `EM_ANDAMENTO` e antes da data de retirada.\n"
                    + "4. Altera o status do contrato para `CANCELADA` (exclusão lógica), libera os equipamentos e registra na auditoria."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Locação cancelada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "404", description = "Locação não encontrada na locadora autenticada",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "422", description = "Locação não pode mais ser cancelada",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public LocacaoResponse cancelar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Parameter(description = "ID da locação", example = "1") @PathVariable Long id) {
        return locacaoService.cancelar(usuario.locadoraId(), id);
    }

    // Alias mantido por compatibilidade com clientes antigos (mesmo comportamento do PATCH)
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Cancelar locação (alias legado)",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`). "
                    + "Equivalente a `PATCH /api/locacoes/{id}/cancelamento` (mantido por compatibilidade; prefira o PATCH).\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` nunca é enviado pelo cliente HTTP, sendo resolvido internamente a partir do usuário autenticado.\n"
                    + "2. Checa existência, valida se está `EM_ANDAMENTO` antes da retirada, marca como `CANCELADA` e libera os equipamentos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Locação cancelada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "404", description = "Locação não encontrada na locadora autenticada",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "422", description = "Locação não pode mais ser cancelada",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public LocacaoResponse cancelarLegado(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Parameter(description = "ID da locação", example = "1") @PathVariable Long id) {
        return locacaoService.cancelar(usuario.locadoraId(), id);
    }
}