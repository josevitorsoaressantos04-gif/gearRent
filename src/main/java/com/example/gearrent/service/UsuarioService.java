package com.example.gearrent.service;

import com.example.gearrent.DTO.MensagemResponse;
import com.example.gearrent.DTO.UsuarioConsultaResponse;
import com.example.gearrent.DTO.UsuarioRequest;
import com.example.gearrent.DTO.UsuarioResponse;
import com.example.gearrent.entities.Empresa;
import com.example.gearrent.entities.Usuario;
import com.example.gearrent.exception.RegraNegocioException;
import com.example.gearrent.repository.EmpresaRepository;
import com.example.gearrent.repository.UsuarioRepository;
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

    // GET: Retorna a lista completa convertida para UsuarioResponse
    @Transactional(readOnly = true)
    public List<UsuarioConsultaResponse> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioConsultaResponse::new)
                .toList();
    }

    // GET: Retorna um usuário específico
    @Transactional(readOnly = true)
    public UsuarioConsultaResponse buscarPorId(Long id) {
        Usuario usuario = buscarEntidadePorId(id);
        return new UsuarioConsultaResponse(usuario);
    }

    // POST: Cria o usuário e vincula à Empresa cadastrada
    @Transactional
    public MensagemResponse criarUsuario(UsuarioRequest request) {

        if (usuarioRepository.existsByEmail(request.email())) {
            throw new RegraNegocioException("O e-mail/login informado já está cadastrado.");
        }

        if (usuarioRepository.existsByCpf(request.cpf())) {
            throw new RegraNegocioException("O CPF informado já está cadastrado.");
        }

        Empresa empresaBanco = empresaRepository.findById(request.empresa_id())
                .orElseThrow(() -> new RegraNegocioException("A empresa informada não está cadastrada no sistema."));

        Usuario usuario = new Usuario();
        usuario.setNome(request.nome());
        usuario.setCpf(request.cpf()); // Sanitizado sem pontos e traços
        usuario.setEmail(request.email());
        usuario.setSenha(request.senha()); // TODO: Criptografia BCrypt
        usuario.setTelefone(request.telefone());
        usuario.setDataNascimento(request.dataNascimento());
        usuario.setAtivo(true);
        usuario.setEmpresa(empresaBanco);
        usuario.setDataCadastro(LocalDateTime.now());

        usuarioRepository.save(usuario);
        return new MensagemResponse(usuario.getId(), "Usuário cadastrado com sucesso.");
    }

    // PUT: Atualiza os dados do usuário com validações de unicidade
    @Transactional
    public MensagemResponse atualizarUsuario(Long id, UsuarioRequest request) {
        Usuario usuario = buscarEntidadePorId(id);

        if (!usuario.getEmail().equalsIgnoreCase(request.email()) && usuarioRepository.existsByEmail(request.email())) {
            throw new RegraNegocioException("Este e-mail já está em uso por outro usuário.");
        }

        if (!usuario.getCpf().equals(request.cpf()) && usuarioRepository.existsByCpf(request.cpf())) {
            throw new RegraNegocioException("O CPF informado já está cadastrado no sistema.");
        }

        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setSenha(request.senha()); // TODO: Criptografia BCrypt
        usuario.setCpf(request.cpf());
        usuario.setTelefone(request.telefone());
        usuario.setDataNascimento(request.dataNascimento());
        usuario.setDataAtualizacao(LocalDateTime.now());

        usuarioRepository.save(usuario);
        return new MensagemResponse(usuario.getId(), "Usuário atualizado com sucesso.");
    }

    // DELETE: Soft Delete com validação do estado ativo
    @Transactional
    public void inativarUsuario(Long id) {
        Usuario usuario = buscarEntidadePorId(id);

        if (Boolean.FALSE.equals(usuario.getAtivo())) {
            throw new RegraNegocioException("Este usuário já se encontra desativado.");
        }

        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
    }

    // Método auxiliar privado para buscar entidade
    private Usuario buscarEntidadePorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Não existe usuário cadastrado com o ID: " + id));
    }
}