package com.example.gearrent.presentation;

import com.example.gearrent.application.DTO.UsuarioAtualizacaoRequest;
import com.example.gearrent.application.DTO.UsuarioAtualizacaoResponse;
import com.example.gearrent.application.DTO.UsuarioCadastroRequest;
import com.example.gearrent.application.DTO.UsuarioCadastroResponse;
import com.example.gearrent.application.DTO.UsuarioConsultaResponse;
import com.example.gearrent.application.DTO.UsuarioStatusResponse;
import com.example.gearrent.application.services.UsuarioService;
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
@RequestMapping("/api/usuarios")
@Tag(name = "Usuario Controller", description = "Classe responsavel por fazer o controle do CRUD de usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // Rota pública: cria o usuário e a base dele
    @PostMapping("/cadastro")
    @Operation(summary = "Cadastrar Novo Usuário (Público)", description = "Cria um novo usuário e automaticamente gera uma nova Locadora (Tenant) vinculada a ele. Este endpoint é público e não requer token de autenticação.")
    public ResponseEntity<UsuarioCadastroResponse> cadastrar(
            @Valid @RequestBody UsuarioCadastroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(usuarioService.cadastroDeUsuarioInicial(request));
    }

    @GetMapping
    @Operation(summary = "Listar Usuários da Base", description = "Retorna uma lista de usuários vinculados à locadora (tenant) do usuário autenticado. Permite busca opcional por nome.")
    public ResponseEntity<List<UsuarioConsultaResponse>> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false, defaultValue = "") String nome) {
        return ResponseEntity.ok(usuarioService.listarUsuariosDaMinhaBase(usuario.locadoraId(), nome));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar Usuario", description = "Metodo utilizado para realizar a atualização das informações do Usuario\n" +
            "1 - Validação da Existência: Confirma que o usuário fornecido pelo ID existe no banco de dados.\n" +
            "2 - Isolamento Multi-Tenant: Garante que a atualização seja restrita, validando se o usuário a ser atualizado pertence à mesma locadora (tenant) do usuário que iniciou a requisição.\n" +
            "3 - Prevenção de Duplicidade: Caso haja alteração no campo de e-mail/login, o sistema cruza os dados para evitar que credenciais duplicadas existam na mesma base de dados.\n" +
            "4 - Atualização Segura de Credenciais: Quando aplicável, aplica a criptografia (BCrypt) para o armazenamento seguro da nova senha informada.")
    public ResponseEntity<UsuarioAtualizacaoResponse> atualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody UsuarioAtualizacaoRequest request) {
        return ResponseEntity.ok(usuarioService.atualizar(usuario.locadoraId(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir Usuário", description = "Remove um usuário do sistema de forma definitiva. A operação só é permitida se o usuário pertencer à mesma locadora (tenant) de quem disparou a requisição.")
    public ResponseEntity<UsuarioStatusResponse> excluir(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.deletar(usuario.locadoraId(), id));
    }

    @PatchMapping("/{id}/desativar")
    @Operation(summary = "Desativar Usuário", description = "Altera o status do usuário para inativo, impedindo futuros logins, sem remover o registro histórico do banco de dados. A operação é restrita à mesma locadora (tenant).")
    public ResponseEntity<UsuarioStatusResponse> desativar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.desativar(usuario.locadoraId(), id));
    }
}