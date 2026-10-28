package com.example.gearrent.presentation;

import com.example.gearrent.application.DTO.EquipamentoAtualizacaoRequest;
import com.example.gearrent.application.DTO.EquipamentoAtualizacaoResponse;
import com.example.gearrent.application.DTO.EquipamentoCadastroRequest;
import com.example.gearrent.application.DTO.EquipamentoCadastroResponse;
import com.example.gearrent.application.DTO.EquipamentoConsultaResponse;
import com.example.gearrent.application.DTO.EquipamentoDisponivelResponse;
import com.example.gearrent.application.DTO.EquipamentoExclusaoResponse;
import com.example.gearrent.application.DTO.EquipamentoListagemResponse;
import com.example.gearrent.application.services.EquipamentoService;
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
@RequestMapping("/api/equipamentos")
@Tag(name = "Equipamento Controller", description = "Classe responsável pelo gerenciamento do catálogo e estoque de equipamentos da locadora")
public class EquipamentoController {

    private final EquipamentoService equipamentoService;

    public EquipamentoController(EquipamentoService equipamentoService) {
        this.equipamentoService = equipamentoService;
    }

    @PostMapping
    @Operation(summary = "Cadastrar Equipamento", description = "Registra um novo equipamento no estoque. O equipamento é vinculado obrigatoriamente à locadora (tenant) do usuário autenticado e criado com status inicial apropriado (ex: DISPONIVEL).")
    public ResponseEntity<EquipamentoCadastroResponse> cadastrar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody EquipamentoCadastroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(equipamentoService.cadastrar(usuario.locadoraId(), request));
    }

    @GetMapping
    @Operation(summary = "Listar Todos os Equipamentos", description = "Retorna a lista completa de equipamentos (em qualquer status) pertencentes à locadora do usuário autenticado. Permite filtragem opcional por nome.")
    public ResponseEntity<List<EquipamentoListagemResponse>> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false, defaultValue = "") String nome) {
        return ResponseEntity.ok(equipamentoService.listar(usuario.locadoraId(), nome));
    }

    @GetMapping("/disponiveis")
    @Operation(summary = "Listar Equipamentos Disponíveis", description = "Filtro essencial para a tela inicial: Retorna exclusivamente os equipamentos da locadora atual que estão com status DISPONIVEL, ou seja, prontos para serem alugados.")
    public ResponseEntity<List<EquipamentoDisponivelResponse>> listarDisponiveis(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false, defaultValue = "") String nome) {
        return ResponseEntity.ok(equipamentoService.listarDisponiveis(usuario.locadoraId(), nome));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar Equipamento por ID", description = "Recupera os detalhes completos de um equipamento específico. Valida de forma estrita se o equipamento pertence à mesma locadora (tenant) do usuário.")
    public ResponseEntity<EquipamentoConsultaResponse> buscarPorId(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        return ResponseEntity.ok(equipamentoService.buscarPorId(usuario.locadoraId(), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar Equipamento", description = "Atualiza os dados de um equipamento existente na base. Regra de Negócio: Equipamentos com status ALUGADO não podem ter seus dados alterados para garantir a integridade de contratos em andamento.")
    public ResponseEntity<EquipamentoAtualizacaoResponse> atualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody EquipamentoAtualizacaoRequest request) {
        return ResponseEntity.ok(equipamentoService.atualizar(usuario.locadoraId(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir Equipamento", description = "Remove um equipamento do sistema. Regra de Negócio: A exclusão é bloqueada se o equipamento estiver atualmente com status ALUGADO. A operação restringe-se ao tenant do usuário logado.")
    public ResponseEntity<EquipamentoExclusaoResponse> excluir(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        return ResponseEntity.ok(equipamentoService.excluir(usuario.locadoraId(), id));
    }
}