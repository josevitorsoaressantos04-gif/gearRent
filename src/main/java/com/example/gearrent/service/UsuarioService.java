package com.example.gearrent.service;

import com.example.gearrent.DTO.MensagemResponse;
import com.example.gearrent.DTO.UsuarioRequest;
import com.example.gearrent.DTO.UsuarioResponse;
import com.example.gearrent.entities.Usuario;
import com.example.gearrent.exception.RegraNegocioException;
import com.example.gearrent.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // GET: Retorna a lista completa com todas as informações do usuário
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(this::mapearParaResponse)
                .toList();
    }

    // GET: Retorna um usuário específico
    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        Usuario usuario = buscarEntidadePorId(id);
        return mapearParaResponse(usuario);
    }

    // POST: Cria o usuário e garante o salvamento da dataNascimento
    @Transactional
    public MensagemResponse criarUsuario(UsuarioRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new RegraNegocioException("O e-mail/login informado já está cadastrado.");
        }

        if (usuarioRepository.existsByCpf(request.cpf())) {
            throw new RegraNegocioException("O CPF informado já está cadastrado.");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.nome());
        usuario.setCpf(request.cpf()); // Sanitizado sem pontos e traços
        usuario.setEmail(request.email());
        usuario.setSenha(request.senha()); // TODO: Criptografia BCrypt
        usuario.setTelefone(request.telefone());
        usuario.setDataNascimento(request.dataNascimento()); // Garante o preenchimento da data!
        usuario.setAtivo(true);
        usuario.setDataCadastro(LocalDateTime.now());

        usuarioRepository.save(usuario);
        return new MensagemResponse(usuario.getId(), "Usuário cadastrado com sucesso.");
    }

    // PUT: Atualiza as informações do usuário
    @Transactional
    public MensagemResponse atualizarUsuario(Long id, UsuarioRequest request) {
        Usuario usuario = buscarEntidadePorId(id);

        if (!usuario.getEmail().equalsIgnoreCase(request.email()) && usuarioRepository.existsByEmail(request.email())) {
            throw new RegraNegocioException("Este e-mail já está em uso por outro usuário.");
        }

        if (!usuario.getCpf().equals(request.cpf()) && usuarioRepository.existsByCpf(request.cpf())) {
            throw new RegraNegocioException("O CPF informado já está cadastrado.");
        }

        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setSenha(request.senha());
        usuario.setCpf(request.cpf());
        usuario.setTelefone(request.telefone());
        usuario.setDataNascimento(request.dataNascimento());
        usuario.setDataAtualizacao(LocalDateTime.now());

        usuarioRepository.save(usuario);
        return new MensagemResponse(usuario.getId(), "Usuário atualizado com sucesso.");
    }

    // DELETE: Inativação lógica (Soft Delete)
    @Transactional
    public void inativarUsuario(Long id) {
        Usuario usuario = buscarEntidadePorId(id);

        if (Boolean.FALSE.equals(usuario.getAtivo())) {
            throw new RegraNegocioException("Este usuário já se encontra desativado.");
        }

        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
    }

    private Usuario buscarEntidadePorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado com o ID: " + id));
    }

    // Mapeamento correto para a consulta GET
    private UsuarioResponse mapearParaResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getCpf(),
                usuario.getDataNascimento(), // Mapeia a data sem null
                usuario.getEmail(),
                usuario.getTelefone(),
                usuario.getAtivo()
        );
    }
}