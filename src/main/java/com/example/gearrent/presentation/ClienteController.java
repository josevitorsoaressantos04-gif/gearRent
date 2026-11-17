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
import com.example.gearrent.infra.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
@Tag(name = "Cliente Controller", description = "Classe responsável pelo gerenciamento de clientes (Pessoa Física e Jurídica) da locadora")
@SecurityRequirement(name = "bearerAuth")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    @Operation(summary = "Cadastrar Cliente",
            description = "Registra um novo cliente (PF ou PJ) no sistema. Regra de Negócio: o documento (CPF/CNPJ) é verificado na locadora (tenant), "
                    + "independentemente do status. Se já existir um cliente ATIVO com ele, retorna 409 (duplicidade). "
                    + "Se existir um cliente removido (cancelado/inativo), o cadastro é reativado com os dados informados, "
                    + "mantendo o mesmo id e o mesmo número sequencial. Se não existir, cria um novo cliente.")
    public ResponseEntity<ClienteCadastroResponse> cadastrar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ClienteCadastroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clienteService.cadastrar(request, usuario.locadoraId()));
    }

    @GetMapping
    @Operation(summary = "Listar Clientes",
            description = "Retorna a lista de clientes não removidos vinculados à locadora do usuário autenticado. "
                    + "Permite busca rápida opcional por parte do nome, sem diferenciar maiúsculas de minúsculas.")
    public ResponseEntity<List<ClienteListagemResponse>> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false, defaultValue = "") String nome) {
        return ResponseEntity.ok(clienteService.listar(usuario.locadoraId(), nome));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar Cliente por ID",
            description = "Recupera os detalhes completos de um cliente específico. A operação garante de forma estrita que o cliente pertence à mesma base (tenant) do usuário logado.")
    public ResponseEntity<ClienteConsultaResponse> buscarPorId(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        return ResponseEntity.ok(clienteService.consultar(usuario.locadoraId(), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar Cliente",
            description = "Atualiza os dados cadastrais (nome, tipo de pessoa, CPF/CNPJ, contatos, endereço) de um cliente existente. "
                    + "Tipo de pessoa e documento são opcionais (se omitidos, são mantidos). O documento precisa ter "
                    + "11 números (física) ou 14 (jurídica) e não pode pertencer a outro cliente da mesma locadora (409).")
    public ResponseEntity<ClienteAtualizacaoResponse> atualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody ClienteAtualizacaoRequest request) {
        return ResponseEntity.ok(clienteService.atualizar(usuario.locadoraId(), id, request));
    }

    @GetMapping("/{id}/resumo")
    @Operation(summary = "Resumo de locações do cliente",
            description = "Total de locações (sem canceladas), locações em andamento/atrasadas, equipamentos em posse "
                    + "e valor total em aberto. Restrito à locadora do usuário autenticado.")
    public ResponseEntity<ClienteResumoDTO> resumo(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        return ResponseEntity.ok(clienteService.resumo(usuario.locadoraId(), id));
    }

    @GetMapping("/{id}/historico")
    @Operation(summary = "Histórico de locações do cliente",
            description = "Lista todas as locações do cliente, da mais recente para a mais antiga, com os nomes dos equipamentos. "
                    + "Restrito à locadora do usuário autenticado.")
    public ResponseEntity<List<ClienteHistoricoLocacaoDTO>> historico(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        return ResponseEntity.ok(clienteService.historico(usuario.locadoraId(), id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir Cliente",
            description = "Remove o cliente por exclusão lógica (status CANCELADO), preservando o histórico de locações e faturamento. "
                    + "Regra de Negócio: a exclusão é bloqueada caso o cliente possua locações com status EM_ANDAMENTO ou ATRASADA.")
    public ResponseEntity<ClienteExclusaoResponse> excluir(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        return ResponseEntity.ok(clienteService.excluir(usuario.locadoraId(), id));
    }
}