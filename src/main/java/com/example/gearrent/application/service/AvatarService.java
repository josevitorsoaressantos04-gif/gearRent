package com.example.gearrent.application.service;

import com.example.gearrent.application.DTO.AvatarArquivo;
import com.example.gearrent.application.DTO.AvatarResponse;
import com.example.gearrent.domain.entities.Usuario;
import com.example.gearrent.domain.entities.UsuarioAvatar;
import com.example.gearrent.domain.enums.StatusCadastro;
import com.example.gearrent.domain.exception.RecursoNaoEncontradoException;
import com.example.gearrent.domain.exception.RegraDeNegocioException;
import com.example.gearrent.domain.repository.UsuarioAvatarRepository;
import com.example.gearrent.domain.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class AvatarService {

    private static final long TAMANHO_MAXIMO_BYTES = 2L * 1024 * 1024;   // 2 MB

    private final UsuarioRepository usuarioRepository;
    private final UsuarioAvatarRepository avatarRepository;
    private final Clock clock;

    public AvatarService(UsuarioRepository usuarioRepository,
                         UsuarioAvatarRepository avatarRepository,
                         Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.avatarRepository = avatarRepository;
        this.clock = clock;
    }

    @Transactional
    public AvatarResponse salvar(Long locadoraId, Long usuarioId, MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraDeNegocioException("Escolha uma foto para enviar.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new RegraDeNegocioException("A foto é muito grande. Envie uma imagem de até 2 MB.");
        }

        byte[] conteudo;
        try {
            conteudo = arquivo.getBytes();
        } catch (IOException e) {
            throw new RegraDeNegocioException("Não foi possível ler a foto. Tente outra imagem.");
        }

        // O tipo é descoberto pelo conteúdo, não pelo que o navegador informou
        String tipo = detectarTipo(conteudo);
        if (tipo == null) {
            throw new RegraDeNegocioException("Formato não aceito. Envie uma foto JPG, PNG ou WEBP.");
        }

        Usuario usuario = buscarUsuario(locadoraId, usuarioId);

        UsuarioAvatar avatar = avatarRepository.findById(usuarioId).orElseGet(() -> {
            UsuarioAvatar novo = new UsuarioAvatar();
            novo.setUsuarioId(usuarioId);
            return novo;
        });
        avatar.setContentType(tipo);
        avatar.setTamanho(conteudo.length);
        avatar.setConteudo(conteudo);
        avatarRepository.save(avatar);

        LocalDateTime agora = LocalDateTime.now(clock);
        usuario.setAvatarAtualizadoEm(agora);

        return new AvatarResponse("Foto de perfil atualizada com sucesso!", agora);
    }

    @Transactional(readOnly = true)
    public AvatarArquivo buscar(Long locadoraId, Long usuarioId) {
        Usuario usuario = buscarUsuario(locadoraId, usuarioId);

        UsuarioAvatar avatar = avatarRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Este usuário ainda não tem foto de perfil."));

        return new AvatarArquivo(avatar.getConteudo(), avatar.getContentType(), usuario.getAvatarAtualizadoEm());
    }

    @Transactional
    public AvatarResponse remover(Long locadoraId, Long usuarioId) {
        Usuario usuario = buscarUsuario(locadoraId, usuarioId);

        avatarRepository.deleteById(usuarioId);   // se não existir, não faz nada
        usuario.setAvatarAtualizadoEm(null);

        return new AvatarResponse("Foto de perfil removida.", null);
    }

    private Usuario buscarUsuario(Long locadoraId, Long usuarioId) {
        return usuarioRepository.findByIdAndLocadoraId(usuarioId, locadoraId)
                .filter(u -> u.getStatus() != StatusCadastro.CANCELADO)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Não encontramos este usuário nesta locadora. Confira os dados e tente novamente."));
    }

    // Reconhece JPEG, PNG e WEBP pelos primeiros bytes do arquivo
    private String detectarTipo(byte[] b) {
        if (b.length >= 3
                && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 8
                && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return "image/png";
        }
        if (b.length >= 12
                && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        return null;
    }
}