package com.example.gearrent.application.services;

import com.example.gearrent.application.DTO.UsuarioAtualizacaoRequest;
import com.example.gearrent.application.DTO.UsuarioAtualizacaoResponse;
import com.example.gearrent.application.DTO.UsuarioCadastroRequest;
import com.example.gearrent.application.DTO.UsuarioCadastroResponse;
import com.example.gearrent.application.DTO.UsuarioConsultaResponse;
import com.example.gearrent.application.DTO.UsuarioStatusResponse;
import com.example.gearrent.domain.entities.Locadora;
import com.example.gearrent.domain.entities.Usuario;
import com.example.gearrent.domain.enums.StatusCadastro;
import com.example.gearrent.domain.exceptions.EmailDuplicadoException;
import com.example.gearrent.domain.exceptions.RecursoNaoEncontradoException;
import com.example.gearrent.domain.repository.LocadoraRepository;
import com.example.gearrent.domain.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final LocadoraRepository locadoraRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          LocadoraRepository locadoraRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.locadoraRepository = locadoraRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioCadastroResponse cadastroDeUsuarioInicial(UsuarioCadastroRequest request) {
        String email = request.email().trim();

        if (usuarioRepository.existsByLogin(email)) {
            throw new EmailDuplicadoException(
                    "Já existe um usuário cadastrado com este e-mail. Tente entrar ou use outro e-mail.");
        }

        // 1 - cria a base única do usuário
        Locadora locadora = new Locadora();
        locadora.setNomeFantasia("Base de " + request.nome().trim());
        locadora.setDataCadastro(LocalDateTime.now());
        locadora.setStatus(StatusCadastro.ATIVO);
        locadora = locadoraRepository.save(locadora);

        // 2 - cadastra o usuário e vincula ele internamente
        Usuario usuario = new Usuario();
        usuario.setNome(request.nome().trim());
        usuario.setLogin(email);
        usuario.setSenha(passwordEncoder.encode(request.senha()));
        usuario.setData_nascimento(request.dataNascimento());
        usuario.setDataCadastro(LocalDateTime.now());
        usuario.setStatus(StatusCadastro.ATIVO);
        usuario.setLocadora(locadora);
        usuarioRepository.save(usuario);

        return new UsuarioCadastroResponse(usuario.getId(), "Usuário cadastrado com sucesso!");
    }

    @Transactional
    public UsuarioAtualizacaoResponse atualizar(Long idBase, Long idUsuario, UsuarioAtualizacaoRequest request) {
        Usuario usuario = buscarEntidade(idBase, idUsuario);
        String email = request.email().trim();

        if (usuarioRepository.existsByLoginAndIdNot(email, idUsuario)) {
            throw new EmailDuplicadoException(
                    "Já existe outro usuário cadastrado com este e-mail. Use um e-mail diferente.");
        }

        usuario.setNome(request.nome().trim());
        usuario.setLogin(email);
        if (request.senha() != null && !request.senha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(request.senha()));
        }
        usuario.setData_nascimento(request.dataNascimento());
        usuario.setDataAtualizacao(LocalDateTime.now());

        return new UsuarioAtualizacaoResponse(usuario.getId(), "Usuário atualizado com sucesso!");
    }

    @Transactional
    public UsuarioStatusResponse deletar(Long idBase, Long idUsuario) {
        Usuario usuario = buscarEntidade(idBase, idUsuario);
        usuario.setStatus(StatusCadastro.CANCELADO);
        usuario.setDataAtualizacao(LocalDateTime.now());
        return new UsuarioStatusResponse(usuario.getId(), "Usuário excluído com sucesso!");
    }

    @Transactional
    public UsuarioStatusResponse desativar(Long idBase, Long idUsuario) {
        Usuario usuario = buscarEntidade(idBase, idUsuario);
        usuario.setStatus(StatusCadastro.INATIVO);
        usuario.setDataAtualizacao(LocalDateTime.now());
        return new UsuarioStatusResponse(usuario.getId(), "Usuário desativado com sucesso!");
    }

    // Consulta 1: uso interno (log, auditoria)
    @Transactional(readOnly = true)
    public Long buscarLocadoraId(Long usuarioId) {
        return usuarioRepository.findLocadoraIdByUsuarioId(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Não encontramos este usuário. Confira os dados e tente novamente."));
    }

    // Consulta 2: lista os usuários da base de quem está logado
    @Transactional(readOnly = true)
    public List<UsuarioConsultaResponse> listarUsuariosDaMinhaBase(Long locadoraId, String nome) {
        String termo = nome == null ? "" : nome.trim();
        return usuarioRepository
                .findByLocadoraIdAndStatusNotAndNomeContainingIgnoreCaseOrderByNomeAsc(
                        locadoraId, StatusCadastro.CANCELADO, termo)
                .stream()
                .map(UsuarioConsultaResponse::de)
                .toList();
    }

    private Usuario buscarEntidade(Long idBase, Long idUsuario) {
        return usuarioRepository.findByIdAndLocadoraId(idUsuario, idBase)
                .filter(u -> u.getStatus() != StatusCadastro.CANCELADO)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Não encontramos este usuário nesta locadora. Confira os dados e tente novamente."));
    }
}