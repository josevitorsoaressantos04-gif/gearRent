package com.example.gearrent.presentation;

import com.example.gearrent.application.DTO.LocacaoAtualizacaoRequest;
import com.example.gearrent.application.DTO.LocacaoCadastroRequest;
import com.example.gearrent.application.DTO.LocacaoDevolucaoRequest;
import com.example.gearrent.application.DTO.LocacaoResponse;
import com.example.gearrent.application.service.LocacaoService;
// import do UsuarioAutenticado: use o pacote onde ele está no seu projeto
import com.example.gearrent.infra.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/locacoes")
@Tag(name = "Locações", description = "Contratos de locação da sua locadora")
@SecurityRequirement(name = "bearerAuth")
public class LocacaoController {

    private final LocacaoService locacaoService;

    public LocacaoController(LocacaoService locacaoService) {
        this.locacaoService = locacaoService;
    }

    @PostMapping
    @Operation(summary = "Cadastrar locação (inclusive retroativa)",
            description = "Cria o contrato, reserva os equipamentos e calcula o valor previsto. "
                    + "Permite data de retirada retroativa: caso a data prevista de devolução já tenha passado, "
                    + "a locação já nasce com status ATRASADA e com a multa de 10% sobre as diárias em atraso calculada. "
                    + "Cadastros retroativos são destacados automaticamente nos logs de auditoria.")
    @ApiResponse(responseCode = "201", description = "Locação criada")
    @ApiResponse(responseCode = "404", description = "Cliente ou equipamento não encontrado")
    @ApiResponse(responseCode = "409", description = "Equipamento indisponível")
    @ApiResponse(responseCode = "422", description = "Regra de negócio violada (ex.: cliente inativo, desconto inválido ou data prevista anterior/igual à retirada)")
    public ResponseEntity<LocacaoResponse> cadastrar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody LocacaoCadastroRequest request) {

        LocacaoResponse response = locacaoService.cadastrar(usuario.locadoraId(), usuario.id(), request);
        return ResponseEntity.created(URI.create("/api/locacoes/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar locação (detalhes e contrato)",
            description = "Se estiver atrasada, mostra o valor acumulado até hoje.")
    @ApiResponse(responseCode = "200", description = "Locação encontrada")
    @ApiResponse(responseCode = "404", description = "Locação não encontrada")
    public LocacaoResponse consultar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                     @PathVariable Long id) {
        return locacaoService.consultar(usuario.locadoraId(), id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar locação",
            description = "Só funciona com a locação EM_ANDAMENTO. Permite alterar data prevista de devolução, desconto, "
                    + "responsável pela retirada e endereço de entrega (campos não enviados são mantidos). "
                    + "Se a data prevista ou o desconto mudarem, diárias e multa são recalculadas.")
    @ApiResponse(responseCode = "200", description = "Locação atualizada")
    @ApiResponse(responseCode = "404", description = "Locação não encontrada")
    @ApiResponse(responseCode = "422", description = "Locação não está em andamento ou dados inválidos")
    public LocacaoResponse atualizar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                     @PathVariable Long id,
                                     @Valid @RequestBody LocacaoAtualizacaoRequest request) {
        return locacaoService.atualizar(usuario.locadoraId(), id, request);
    }

    @PatchMapping("/{id}/devolucao")
    @Operation(summary = "Registrar devolução",
            description = "Libera os equipamentos e fixa o valor final, com multa se houve atraso. "
                    + "Corpo opcional: {\"dataDevolucao\": \"yyyy-MM-dd\"}; sem corpo, usa a data de hoje.")
    @ApiResponse(responseCode = "200", description = "Devolução registrada")
    @ApiResponse(responseCode = "404", description = "Locação não encontrada")
    @ApiResponse(responseCode = "422", description = "Locação não pode mais ser devolvida")
    public LocacaoResponse devolver(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                    @PathVariable Long id,
                                    @Valid @RequestBody(required = false) LocacaoDevolucaoRequest request) {
        return locacaoService.devolver(usuario.locadoraId(), id, request);
    }

    // Contrato oficial usado pela interface: PATCH /api/locacoes/{id}/cancelamento
    @PatchMapping("/{id}/cancelamento")
    @Operation(summary = "Cancelar locação",
            description = "Só funciona enquanto estiver em andamento e antes da data de retirada. Libera os equipamentos.")
    @ApiResponse(responseCode = "200", description = "Locação cancelada")
    @ApiResponse(responseCode = "404", description = "Locação não encontrada")
    @ApiResponse(responseCode = "422", description = "Locação não pode mais ser cancelada")
    public LocacaoResponse cancelar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                    @PathVariable Long id) {
        return locacaoService.cancelar(usuario.locadoraId(), id);
    }

    // Alias mantido por compatibilidade com clientes antigos (mesmo comportamento do PATCH)
    @DeleteMapping("/{id}")
    @Operation(summary = "Cancelar locação (alias legado)",
            description = "Equivalente a PATCH /api/locacoes/{id}/cancelamento. Prefira o PATCH.")
    @ApiResponse(responseCode = "200", description = "Locação cancelada")
    @ApiResponse(responseCode = "404", description = "Locação não encontrada")
    @ApiResponse(responseCode = "422", description = "Locação não pode mais ser cancelada")
    public LocacaoResponse cancelarLegado(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                          @PathVariable Long id) {
        return locacaoService.cancelar(usuario.locadoraId(), id);
    }
}