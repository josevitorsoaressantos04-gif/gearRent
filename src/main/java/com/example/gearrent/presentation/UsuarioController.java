package com.example.gearrent.presentation;

import com.example.gearrent.application.DTO.*;
import com.example.gearrent.application.service.AvatarService;
import com.example.gearrent.application.service.UsuarioService;
import com.example.gearrent.infra.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuario Controller", description = "Classe responsavel por fazer o controle do CRUD de usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AvatarService avatarService;

    public UsuarioController(UsuarioService usuarioService, AvatarService avatarService) {
        this.usuarioService = usuarioService;
        this.avatarService = avatarService;
    }

    // ---------- Cadastro público ----------

    // Rota pública: cria o usuário e a base dele
    @PostMapping("/cadastro")
    @Operation(summary = "Cadastrar Novo Usuário (Público)", description = "Cria um novo usuário e automaticamente gera uma nova Locadora (Tenant) vinculada a ele, com o papel de administrador. Este endpoint é público e não requer token de autenticação.")
    public ResponseEntity<UsuarioCadastroResponse> cadastrar(
            @Valid @RequestBody UsuarioCadastroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(usuarioService.cadastroDeUsuarioInicial(request));
    }

    // ---------- Gestão de usuários da base ----------

    @GetMapping
    @Operation(summary = "Listar Usuários da Base", description = "Retorna uma lista de usuários vinculados à locadora (tenant) do usuário autenticado, com o papel (role) de cada um. Permite busca opcional por nome.")
    public ResponseEntity<List<UsuarioConsultaResponse>> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false, defaultValue = "") String nome) {
        return ResponseEntity.ok(usuarioService.listarUsuariosDaMinhaBase(usuario.locadoraId(), nome));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Criar operador", description = "Cria um usuário na mesma base do administrador logado.")
    @ApiResponse(responseCode = "201", description = "Operador criado")
    @ApiResponse(responseCode = "403", description = "Somente administradores")
    public ResponseEntity<OperadorCadastroResponse> criarOperador(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody OperadorCadastroRequest request) {

        OperadorCadastroResponse response = usuarioService.criarOperador(usuario.locadoraId(), request);
        return ResponseEntity.created(URI.create("/api/usuarios/" + response.id())).body(response);
    }

    // O "id" do método precisa se chamar id: o @PreAuthorize usa #id
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id()")
    @Operation(summary = "Atualizar Usuario", description = "Atualiza nome, sobrenome, CPF, data de nascimento e e-mail de um usuário da base. "
            + "Permitido ao administrador (qualquer usuário da base) ou ao próprio usuário. "
            + "Não altera a senha: para isso existe PATCH /api/usuarios/trocar-senha.\n"
            + "1 - Validação da Existência: confirma que o usuário existe.\n"
            + "2 - Isolamento Multi-Tenant: o usuário precisa pertencer à mesma locadora de quem fez a requisição.\n"
            + "3 - Prevenção de Duplicidade: e-mail já usado por outro usuário é recusado, assim como CPF repetido na mesma base.")
    public ResponseEntity<UsuarioAtualizacaoResponse> atualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody UsuarioAtualizacaoRequest request) {
        return ResponseEntity.ok(usuarioService.atualizar(usuario.locadoraId(), id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Excluir Usuário", description = "Exclui o usuário (exclusão lógica: ele deixa de aparecer e não consegue mais entrar). Somente administradores, e apenas para usuários da mesma locadora (tenant).")
    @ApiResponse(responseCode = "403", description = "Somente administradores")
    public ResponseEntity<UsuarioStatusResponse> excluir(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.deletar(usuario.locadoraId(), id));
    }

    @PatchMapping("/{id}/desativar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desativar Usuário", description = "Altera o status do usuário para inativo, impedindo futuros logins, sem remover o registro histórico do banco de dados. Somente administradores, restrito à mesma locadora (tenant).")
    @ApiResponse(responseCode = "403", description = "Somente administradores")
    public ResponseEntity<UsuarioStatusResponse> desativar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.desativar(usuario.locadoraId(), id));
    }

    @PatchMapping("/{id}/reativar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reativar usuário", description = "Volta o acesso de um usuário inativo da sua base.")
    @ApiResponse(responseCode = "200", description = "Usuário reativado")
    @ApiResponse(responseCode = "403", description = "Somente administradores")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    public UsuarioReativacaoResponse reativar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                              @PathVariable Long id) {
        return usuarioService.reativar(usuario.locadoraId(), id);
    }

    // ---------- Meu perfil e senha ----------

    @GetMapping("/me")
    @Operation(summary = "Ver meu perfil")
    public PerfilConsultaResponse meuPerfil(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return usuarioService.consultarPerfil(usuario.locadoraId(), usuario.id());
    }

    @PutMapping("/me")
    @Operation(summary = "Atualizar meu perfil", description = "Nome, sobrenome, e-mail e CPF. Não pede senha.")
    public PerfilConsultaResponse atualizarPerfil(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                  @Valid @RequestBody PerfilAtualizacaoRequest request) {
        return usuarioService.atualizarPerfil(usuario.locadoraId(), usuario.id(), request);
    }

    @PatchMapping("/trocar-senha")
    @Operation(summary = "Trocar minha senha", description = "Exige a senha atual e a confirmação da nova senha.")
    @ApiResponse(responseCode = "204", description = "Senha alterada")
    public ResponseEntity<Void> trocarSenha(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                            @Valid @RequestBody TrocaSenhaRequest request) {
        usuarioService.trocarSenha(usuario.locadoraId(), usuario.id(), request);
        return ResponseEntity.noContent().build();
    }

    // ---------- Foto de perfil ----------

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Enviar minha foto de perfil",
            description = "Campo 'arquivo'. Aceita JPG, PNG ou WEBP de até 2 MB. Substitui a foto anterior.")
    @ApiResponse(responseCode = "200", description = "Foto atualizada")
    @ApiResponse(responseCode = "422", description = "Arquivo inválido")
    public AvatarResponse enviarAvatar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                       @RequestParam("arquivo") MultipartFile arquivo) {
        return avatarService.salvar(usuario.locadoraId(), usuario.id(), arquivo);
    }

    @GetMapping("/me/avatar")
    @Operation(summary = "Ver minha foto de perfil")
    public ResponseEntity<byte[]> meuAvatar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return montarImagem(avatarService.buscar(usuario.locadoraId(), usuario.id()));
    }

    @GetMapping("/{id}/avatar")
    @Operation(summary = "Ver a foto de perfil de um usuário da minha base")
    public ResponseEntity<byte[]> avatarDoUsuario(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                  @PathVariable Long id) {
        return montarImagem(avatarService.buscar(usuario.locadoraId(), id));
    }

    @DeleteMapping("/avatar")
    @Operation(summary = "Remover minha foto de perfil")
    public AvatarResponse removerAvatar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return avatarService.remover(usuario.locadoraId(), usuario.id());
    }

    private ResponseEntity<byte[]> montarImagem(AvatarArquivo avatar) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(avatar.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .body(avatar.conteudo());
    }
}