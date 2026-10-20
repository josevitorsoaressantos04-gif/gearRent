package com.example.gearrent.application.services;

import com.example.gearrent.application.DTO.*;
import com.example.gearrent.domain.entities.Locadora;
import com.example.gearrent.domain.entities.Usuario;
import com.example.gearrent.domain.enums.StatusCadastro;
import com.example.gearrent.domain.exceptions.RecursoNaoEncontradoException;
import com.example.gearrent.domain.repository.LocadoraRepository;
import com.example.gearrent.domain.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LocadoraRepository locadoraRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioCadastroResponse cadastroDeUsuarioInicial(UsuarioCadastroRequest request) {

        //1 - cria a base única do usuário

        Locadora locadora = new Locadora();
        locadora.setNomeFantasia("Base de" + request.nome());
        locadora.setDataCadastro(LocalDateTime.now());
        locadora.setStatus(StatusCadastro.ATIVO);
        locadora = locadoraRepository.save(locadora);

        //2 - cadastra o usuário e vincula ele internamente
        Usuario usuario = new Usuario();
        usuario.setNome(request.nome());
        usuario.setLogin(request.email());
        usuario.setSenha(passwordEncoder.encode(request.senha()));
        usuario.setData_nascimento(request.dataNascimento());
        usuario.setDataCadastro(LocalDateTime.now());
        usuario.setStatus(StatusCadastro.ATIVO);
        usuario.setLocadora(locadora);
        usuarioRepository.save(usuario);

        return new UsuarioCadastroResponse(usuario.getId(), "Usuario Cadastrado com Sucesso!");
    }

    @Transactional
    public UsuarioAtualizacaoResponse atualizar(Long idBase, Long idUsuario, UsuarioAtualizacaoRequest request) {
        Usuario usuario = usuarioRepository
                .findByIdAndLocadoraId(idUsuario, idBase)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Não encontramos este usuário nesta locadora. Confira os dados e tente novamente."));
        usuario.setNome(request.nome());
        usuario.setLogin(request.email());
        if (request.senha() != null && !request.senha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(request.senha()));
        }
        usuario.setData_nascimento(request.dataNascimento());
        usuario.setDataAtualizacao(LocalDateTime.now());
        usuarioRepository.save(usuario);

        return new UsuarioAtualizacaoResponse(usuario.getId(), "Usuario atualizado com sucesso");
    }

    @Transactional
    public UsuarioStatusResponse deletar(Long idBase, Long idUsuario) {
        Usuario usuario = usuarioRepository
                .findByIdAndLocadoraId(idUsuario, idBase)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Não encontramos este usuário nesta locadora. Confira os dados e tente novamente."));
        usuario.setStatus(StatusCadastro.CANCELADO);
        usuario.setDataAtualizacao(LocalDateTime.now());
        return new UsuarioStatusResponse(usuario.getId(), "Usuário excluído com sucesso!");
    }

    @Transactional
    public UsuarioStatusResponse desativar(Long idBase, Long idUsuario) {
        Usuario usuario = usuarioRepository
                .findByIdAndLocadoraId(idUsuario, idBase)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Não encontramos este usuário nesta locadora. Confira os dados e tente novamente."));
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

    // Consulta 2: o usuário lista os usuários da base dele
    @Transactional(readOnly = true)
    public List<UsuarioConsultaResponse> listarUsuariosDaMinhaBase(Long usuarioId, Long locadoraId) {
        usuarioRepository.findByIdAndLocadoraId(usuarioId, locadoraId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Não encontramos este usuário nesta locadora. Confira os dados e tente novamente."));

        return usuarioRepository.findByLocadoraId(locadoraId).stream()
                .map(u -> new UsuarioConsultaResponse(
                        u.getId(),
                        u.getLogin(),
                        u.getNome(),
                        u.getData_nascimento(),
                        u.getStatus()))
                .toList();
    }
}
