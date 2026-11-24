package com.example.gearrent.presentation;

import com.example.gearrent.application.DTO.*;
import com.example.gearrent.application.service.AvatarService;
import com.example.gearrent.application.service.UsuarioService;
import com.example.gearrent.infra.exception.ApiErroResposta;
import com.example.gearrent.infra.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
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
@Tag(
        name = "Usuários",
        description = "Cadastro inicial de locadora e administrador (público), gestão de operadores, perfil pessoal, troca de senha e foto de perfil."
)
@SecurityRequirement(name = "bearerAuth")
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
    @SecurityRequirements
    @Operation(
            summary = "Cadastrar usuário administrador e criar locadora (público)",
            description = "Endpoint público (não exige token JWT).\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. Valida os dados pessoais do administrador e os dados da nova locadora (incluindo CNPJ).\n"
                    + "2. Verifica unicidade de e-mail e CNPJ para evitar duplicidades.\n"
                    + "3. Cria a nova `Locadora` (tenant), vincula o usuário inicial com papel `ROLE_ADMIN` e criptografa a senha com BCrypt."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário administrador e locadora criados com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "409", description = "E-mail ou CNPJ já cadastrado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public ResponseEntity<UsuarioCadastroResponse> cadastrar(
            @Valid @RequestBody UsuarioCadastroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(usuarioService.cadastroDeUsuarioInicial(request));
    }

    // ---------- Gestão de usuários da base ----------

    @GetMapping
    @Operation(
            summary = "Listar usuários da locadora",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`).\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` nunca é enviado pelo cliente HTTP, sendo resolvido internamente a partir do usuário autenticado.\n"
                    + "2. Lista todos os usuários vinculados à locadora autenticada com seus respectivos papéis (`role`), permitindo filtro opcional por nome."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de usuários retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public ResponseEntity<List<UsuarioConsultaResponse>> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Parameter(description = "Filtro opcional por nome") @RequestParam(required = false, defaultValue = "") String nome) {
        return ResponseEntity.ok(usuarioService.listarUsuariosDaMinhaBase(usuario.locadoraId(), nome));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Criar operador na locadora",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`) e perfil `ROLE_ADMIN`.\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` nunca é enviado pelo cliente HTTP, sendo resolvido internamente a partir do administrador autenticado.\n"
                    + "2. Valida unicidade de e-mail e de CPF dentro da locadora.\n"
                    + "3. Cria o novo operador vinculado à mesma locadora com senha criptografada via BCrypt e registra na auditoria."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Operador criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores (ROLE_ADMIN)",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "409", description = "E-mail ou CPF já cadastrado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public ResponseEntity<OperadorCadastroResponse> criarOperador(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody OperadorCadastroRequest request) {

        OperadorCadastroResponse response = usuarioService.criarOperador(usuario.locadoraId(), request);
        return ResponseEntity.created(URI.create("/api/usuarios/" + response.id())).body(response);
    }

    // O "id" do método precisa se chamar id: o @PreAuthorize usa #id
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id()")
    @Operation(
            summary = "Atualizar usuário",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`), sendo permitido ao `ROLE_ADMIN` ou ao próprio usuário.\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` nunca é enviado pelo cliente HTTP, sendo resolvido internamente a partir do usuário autenticado.\n"
                    + "2. Confirma a existência do usuário e valida o isolamento multi-tenant na locadora autenticada.\n"
                    + "3. Previne duplicidade de e-mail global e de CPF dentro da mesma locadora.\n"
                    + "4. Atualiza nome, sobrenome, CPF, data de nascimento e e-mail (para troca de senha utilize `PATCH /api/usuarios/trocar-senha`)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado — requer ROLE_ADMIN ou ser o próprio usuário",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado na locadora autenticada",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "409", description = "E-mail ou CPF já utilizado por outro usuário",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public ResponseEntity<UsuarioAtualizacaoResponse> atualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Parameter(description = "ID do usuário", example = "1") @PathVariable Long id,
            @Valid @RequestBody UsuarioAtualizacaoRequest request) {
        return ResponseEntity.ok(usuarioService.atualizar(usuario.locadoraId(), id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Excluir usuário (exclusão lógica)",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`) e perfil `ROLE_ADMIN`.\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` nunca é enviado pelo cliente HTTP, sendo resolvido internamente a partir do usuário autenticado.\n"
                    + "2. Checa a existência do usuário dentro da locadora autenticada.\n"
                    + "3. Realiza exclusão lógica preservando o histórico e impedindo novos logins."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário excluído logicamente com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores (ROLE_ADMIN)",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado na locadora autenticada",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "422", description = "Regra de negócio violada",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public ResponseEntity<UsuarioStatusResponse> excluir(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Parameter(description = "ID do usuário", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.deletar(usuario.locadoraId(), id));
    }

    @PatchMapping("/{id}/desativar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Desativar usuário",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`) e perfil `ROLE_ADMIN`.\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` nunca é enviado pelo cliente HTTP, sendo resolvido internamente a partir do usuário autenticado.\n"
                    + "2. Checa a existência do usuário na locadora autenticada.\n"
                    + "3. Impede que o administrador desative o próprio acesso.\n"
                    + "4. Altera o status para `INATIVO`, bloqueando novos logins sem apagar o histórico."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário desativado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores (ROLE_ADMIN)",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado na locadora autenticada",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "422", description = "Administrador tentando desativar o próprio acesso",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public ResponseEntity<UsuarioStatusResponse> desativar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Parameter(description = "ID do usuário", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.desativar(usuario.locadoraId(), usuario.id(), id));
    }

    @PatchMapping("/{id}/reativar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Reativar usuário",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`) e perfil `ROLE_ADMIN`.\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` nunca é enviado pelo cliente HTTP, sendo resolvido internamente a partir do usuário autenticado.\n"
                    + "2. Checa a existência do usuário inativo na locadora autenticada e restaura o acesso (`ATIVO`)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário reativado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores (ROLE_ADMIN)",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado na locadora autenticada",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public UsuarioReativacaoResponse reativar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Parameter(description = "ID do usuário", example = "1") @PathVariable Long id) {
        return usuarioService.reativar(usuario.locadoraId(), id);
    }

    // ---------- Meu perfil e senha ----------

    @GetMapping("/me")
    @Operation(
            summary = "Consultar perfil do usuário autenticado",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`). "
                    + "Tanto o `usuarioId` quanto o `locadoraId` são resolvidos internamente a partir do token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil retornado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public PerfilConsultaResponse meuPerfil(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return usuarioService.consultarPerfil(usuario.locadoraId(), usuario.id());
    }

    @PutMapping("/me")
    @Operation(
            summary = "Atualizar perfil do usuário autenticado",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`).\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` e o `usuarioId` são resolvidos internamente a partir do token.\n"
                    + "2. Atualiza nome, sobrenome, e-mail e CPF sem exigir senha, validando duplicidade."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "409", description = "E-mail ou CPF já utilizado por outro registro",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public PerfilConsultaResponse atualizarPerfil(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                  @Valid @RequestBody PerfilAtualizacaoRequest request) {
        return usuarioService.atualizarPerfil(usuario.locadoraId(), usuario.id(), request);
    }

    @PatchMapping("/trocar-senha")
    @Operation(
            summary = "Alterar senha do usuário autenticado",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`).\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` e o `usuarioId` são resolvidos internamente a partir do token.\n"
                    + "2. Valida a senha atual com BCrypt e exige confirmação da nova senha.\n"
                    + "3. Atualiza o hash da senha no banco."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha alterada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou senha atual incorreta",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "422", description = "Nova senha e confirmação divergentes",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public ResponseEntity<Void> trocarSenha(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                            @Valid @RequestBody TrocaSenhaRequest request) {
        usuarioService.trocarSenha(usuario.locadoraId(), usuario.id(), request);
        return ResponseEntity.noContent().build();
    }

    // ---------- Foto de perfil ----------

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Enviar foto de perfil",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`).\n\n"
                    + "Etapas e regras de negócio:\n"
                    + "1. O `locadoraId` e o `usuarioId` são resolvidos internamente a partir do token.\n"
                    + "2. Recebe o campo `arquivo` (JPG, PNG ou WEBP) com tamanho máximo de 2 MB e substitui a foto anterior."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Foto de perfil atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Arquivo ausente na requisição",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "413", description = "Arquivo excede o limite de 2 MB",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "422", description = "Formato de arquivo inválido",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public AvatarResponse enviarAvatar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                       @RequestParam("arquivo") MultipartFile arquivo) {
        return avatarService.salvar(usuario.locadoraId(), usuario.id(), arquivo);
    }

    @GetMapping("/me/avatar")
    @Operation(
            summary = "Consultar minha foto de perfil",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`). "
                    + "Retorna os bytes da imagem do usuário autenticado na sua locadora."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagem retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "404", description = "Foto de perfil não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public ResponseEntity<byte[]> meuAvatar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return montarImagem(avatarService.buscar(usuario.locadoraId(), usuario.id()));
    }

    @GetMapping("/{id}/avatar")
    @Operation(
            summary = "Consultar foto de perfil de um usuário da locadora",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`). "
                    + "O `locadoraId` é resolvido internamente a partir do token, garantindo acesso apenas a usuários da mesma base."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagem retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class))),
            @ApiResponse(responseCode = "404", description = "Usuário ou foto de perfil não encontrada na locadora",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
    public ResponseEntity<byte[]> avatarDoUsuario(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Parameter(description = "ID do usuário", example = "1") @PathVariable Long id) {
        return montarImagem(avatarService.buscar(usuario.locadoraId(), id));
    }

    @DeleteMapping("/avatar")
    @Operation(
            summary = "Remover minha foto de perfil",
            description = "Requer autenticação via token JWT (`Authorization: Bearer <token>`). "
                    + "Remove a imagem associada ao usuário autenticado na sua locadora."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Foto de perfil removida com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiErroResposta.class)))
    })
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