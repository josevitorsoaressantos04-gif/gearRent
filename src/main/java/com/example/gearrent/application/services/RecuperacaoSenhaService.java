package com.example.gearrent.application.services;

import com.example.gearrent.application.DTO.EsqueciSenhaRequest;
import com.example.gearrent.application.DTO.EsqueciSenhaResponse;
import com.example.gearrent.application.DTO.RedefinirSenhaRequest;
import com.example.gearrent.application.DTO.RedefinirSenhaResponse;
import com.example.gearrent.domain.entities.TokenRecuperacaoSenha;
import com.example.gearrent.domain.entities.Usuario;
import com.example.gearrent.domain.enums.StatusCadastro;
import com.example.gearrent.domain.exceptions.RegraDeNegocioException;
import com.example.gearrent.domain.exceptions.TokenRecuperacaoInvalidoException;
import com.example.gearrent.domain.repository.TokenRecuperacaoSenhaRepository;
import com.example.gearrent.domain.repository.UsuarioRepository;
import com.example.gearrent.infra.email.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class RecuperacaoSenhaService {

    // Sempre a mesma resposta, exista o e-mail ou não (não revela quem tem conta)
    private static final String MENSAGEM_PADRAO =
            "Se este e-mail estiver cadastrado, enviaremos as instruções para criar uma nova senha. "
                    + "Confira também a caixa de spam.";

    private static final String MENSAGEM_LINK_INVALIDO =
            "Este link para criar uma nova senha não é mais válido. Peça um novo link.";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacaoSenhaRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.recuperacao-senha.expiracao-minutos}")
    private long expiracaoMinutos;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public RecuperacaoSenhaService(UsuarioRepository usuarioRepository,
                                   TokenRecuperacaoSenhaRepository tokenRepository,
                                   PasswordEncoder passwordEncoder,
                                   EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public EsqueciSenhaResponse solicitar(EsqueciSenhaRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        usuarioRepository.findByLogin(email)
                .filter(u -> u.getStatus() == StatusCadastro.ATIVO)
                .ifPresent(this::gerarEEnviarLink);

        return new EsqueciSenhaResponse(MENSAGEM_PADRAO);
    }

    @Transactional
    public RedefinirSenhaResponse redefinir(RedefinirSenhaRequest request) {
        if (!request.novaSenha().equals(request.confirmacaoSenha())) {
            throw new RegraDeNegocioException("As senhas digitadas não são iguais. Digite novamente.");
        }

        LocalDateTime agora = LocalDateTime.now();

        TokenRecuperacaoSenha token = tokenRepository.buscarPorHash(gerarHash(request.token().trim()))
                .filter(t -> t.getUsadoEm() == null)
                .filter(t -> t.getExpiraEm().isAfter(agora))
                .orElseThrow(() -> new TokenRecuperacaoInvalidoException(MENSAGEM_LINK_INVALIDO));

        Usuario usuario = token.getUsuario();
        if (usuario.getStatus() != StatusCadastro.ATIVO) {
            throw new TokenRecuperacaoInvalidoException(MENSAGEM_LINK_INVALIDO);
        }

        usuario.setSenha(passwordEncoder.encode(request.novaSenha()));
        usuario.setDataAtualizacao(agora);
        token.setUsadoEm(agora);

        return new RedefinirSenhaResponse("Senha alterada com sucesso! Agora você já pode entrar.");
    }

    private void gerarEEnviarLink(Usuario usuario) {
        LocalDateTime agora = LocalDateTime.now();

        // Evita pedidos em sequência (spam na caixa de entrada)
        if (tokenRepository.existsByUsuarioIdAndCriadoEmAfter(usuario.getId(), agora.minusMinutes(1))) {
            return;
        }

        // Só o link mais recente vale
        tokenRepository.apagarPendentesDoUsuario(usuario.getId());

        String tokenPuro = gerarTokenAleatorio();

        TokenRecuperacaoSenha token = new TokenRecuperacaoSenha();
        token.setTokenHash(gerarHash(tokenPuro));
        token.setUsuario(usuario);
        token.setCriadoEm(agora);
        token.setExpiraEm(agora.plusMinutes(expiracaoMinutos));
        tokenRepository.save(token);

        String link = frontendUrl + "/redefinir-senha?token=" + tokenPuro;
        emailService.enviarRecuperacaoSenha(usuario.getLogin(), usuario.getNome(), link, expiracaoMinutos);
    }

    private String gerarTokenAleatorio() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String gerarHash(String token) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha256.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível", e);
        }
    }
}