package com.example.gearrent.service;

import com.example.gearrent.DTO.MensagemResponse;
import com.example.gearrent.DTO.UsuarioConsultaResponse;
import com.example.gearrent.DTO.UsuarioRequest;
import com.example.gearrent.entities.Empresa;
import com.example.gearrent.entities.Usuario;
import com.example.gearrent.exception.RegraNegocioException;
import com.example.gearrent.repository.EmpresaRepository;
import com.example.gearrent.repository.UsuarioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;

    public UsuarioService(UsuarioRepository usuarioRepository, EmpresaRepository empresaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
    }

    // --- CONSULTAS ---

    @Transactional(readOnly = true)
    public List<UsuarioConsultaResponse> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioConsultaResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioConsultaResponse buscarPorId(Long id) {
        Usuario usuario = buscarEntidadePorId(id);
        return new UsuarioConsultaResponse(usuario);
    }

    @Transactional(readOnly = true)
    public List<UsuarioConsultaResponse> buscarPorNome(String nome) {
        return usuarioRepository.findByNomeContainingIgnoreCase(nome, Sort.by("nome").ascending()).stream()
                .map(UsuarioConsultaResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioConsultaResponse buscarPorCpf(String cpf) {
        Usuario usuario = usuarioRepository.findByCpf(cpf)
                .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado com o CPF informado."));
        return new UsuarioConsultaResponse(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioConsultaResponse buscarPorLogin(String login) {
        Usuario usuario = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado com o Login informado."));
        return new UsuarioConsultaResponse(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioConsultaResponse buscarPorEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado com o E-mail informado."));
        return new UsuarioConsultaResponse(usuario);
    }

    @Transactional(readOnly = true)
    public List<UsuarioConsultaResponse> buscarPorIntervaloDataCadastro(LocalDateTime inicio, LocalDateTime fim) {
        return usuarioRepository.findByDataCadastroBetween(inicio, fim).stream()
                .map(UsuarioConsultaResponse::new)
                .toList();
    }

    // --- MUTACÕES (MANTIDAS) ---

    @Transactional
    public MensagemResponse criarUsuario(UsuarioRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new RegraNegocioException("O e-mail informado já está cadastrado.");
        }

        if (usuarioRepository.existsByCpf(request.cpf())) {
            throw new RegraNegocioException("O CPF informado já está cadastrado.");
        }

        Empresa empresaBanco = empresaRepository.findById(request.empresa_id())
                .orElseThrow(() -> new RegraNegocioException("A empresa informada não está cadastrada no sistema."));

        if (Boolean.FALSE.equals(empresaBanco.getStatus())) {
            throw new RegraNegocioException("Não é possível associar usuários a uma empresa inativa.");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.nome());
        usuario.setCpf(request.cpf());
        usuario.setLogin(request.login());
        usuario.setEmail(request.email());
        usuario.setSenha(request.senha()); // TODO: BCrypt
        usuario.setTelefone(request.telefone());
        usuario.setDataNascimento(request.dataNascimento());
        usuario.setAtivo(true);
        usuario.setEmpresa(empresaBanco);
        usuario.setDataCadastro(LocalDateTime.now());

        usuarioRepository.save(usuario);
        return new MensagemResponse(usuario.getId(), "Usuário cadastrado com sucesso.");
    }

    @Transactional
    public MensagemResponse atualizarUsuario(Long id, UsuarioRequest request) {
        Usuario usuario = buscarEntidadePorId(id);

        if (Boolean.FALSE.equals(usuario.getAtivo())) {
            throw new RegraNegocioException("Não é possível alterar os dados de um usuário inativo.");
        }

        if (!usuario.getEmail().equalsIgnoreCase(request.email()) && usuarioRepository.existsByEmail(request.email())) {
            throw new RegraNegocioException("Este e-mail já está em uso por outro usuário.");
        }

        if (!usuario.getCpf().equals(request.cpf()) && usuarioRepository.existsByCpf(request.cpf())) {
            throw new RegraNegocioException("O CPF informado já está cadastrado no sistema.");
        }

        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setLogin(request.login());
        usuario.setSenha(request.senha()); // TODO: BCrypt
        usuario.setCpf(request.cpf());
        usuario.setTelefone(request.telefone());
        usuario.setDataNascimento(request.dataNascimento());
        usuario.setDataAtualizacao(LocalDateTime.now());

        usuarioRepository.save(usuario);
        return new MensagemResponse(usuario.getId(), "Usuário atualizado com sucesso.");
    }

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
                .orElseThrow(() -> new RegraNegocioException("Não existe usuário cadastrado com o ID: " + id));
    }
}