package com.example.gearrent.presentation;

import com.example.gearrent.application.DTO.ClienteAtualizacaoRequest;
import com.example.gearrent.application.DTO.ClienteAtualizacaoResponse;
import com.example.gearrent.application.DTO.ClienteCadastroRequest;
import com.example.gearrent.application.DTO.ClienteCadastroResponse;
import com.example.gearrent.application.DTO.ClienteConsultaResponse;
import com.example.gearrent.application.DTO.ClienteExclusaoResponse;
import com.example.gearrent.application.DTO.ClienteListagemResponse;
import com.example.gearrent.application.services.ClienteService;
import com.example.gearrent.infra.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
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
            description = "Atualiza os dados cadastrais (nome, contatos, endereço) de um cliente existente. O CPF/CNPJ não pode ser alterado.")
    public ResponseEntity<ClienteAtualizacaoResponse> atualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody ClienteAtualizacaoRequest request) {
        return ResponseEntity.ok(clienteService.atualizar(usuario.locadoraId(), id, request));
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