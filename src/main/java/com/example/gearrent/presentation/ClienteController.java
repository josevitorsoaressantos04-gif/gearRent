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
    @Operation(summary = "Cadastrar Cliente", description = "Registra um novo cliente (PF ou PJ) no sistema. Regra de Negócio: O documento (CPF/CNPJ) é validado para garantir que não haja duplicidade de clientes ativos na mesma locadora (tenant).")
    public ResponseEntity<ClienteCadastroResponse> cadastrar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ClienteCadastroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clienteService.cadastrar(usuario.locadoraId(), request));
    }

    @GetMapping
    @Operation(summary = "Listar Clientes", description = "Retorna a lista de clientes vinculados à locadora do usuário autenticado. Permite busca rápida opcional através do parâmetro de nome.")
    public ResponseEntity<List<ClienteListagemResponse>> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false, defaultValue = "") String nome) {
        return ResponseEntity.ok(clienteService.listar(usuario.locadoraId(), nome));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar Cliente por ID", description = "Recupera os detalhes completos de um cliente específico. A operação garante de forma estrita que o cliente pertence à mesma base (tenant) do usuário logado.")
    public ResponseEntity<ClienteConsultaResponse> buscarPorId(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        return ResponseEntity.ok(clienteService.buscarPorId(usuario.locadoraId(), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar Cliente", description = "Atualiza os dados cadastrais (nome, contatos, endereço) de um cliente existente. Caso o documento seja alterado, o sistema revalida a regra de prevenção de duplicidade na locadora.")
    public ResponseEntity<ClienteAtualizacaoResponse> atualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody ClienteAtualizacaoRequest request) {
        return ResponseEntity.ok(clienteService.atualizar(usuario.locadoraId(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir/Inativar Cliente", description = "Altera o status do cliente ou o remove do sistema. Regra de Negócio: A inativação/exclusão é bloqueada caso o cliente possua contratos de locação vinculados a ele com status EM_ANDAMENTO ou ATRASADA.")
    public ResponseEntity<ClienteExclusaoResponse> excluir(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        return ResponseEntity.ok(clienteService.excluir(usuario.locadoraId(), id));
    }
}