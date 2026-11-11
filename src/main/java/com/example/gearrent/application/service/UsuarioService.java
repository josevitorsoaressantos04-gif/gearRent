package com.example.gearrent.application.service;

import com.example.gearrent.application.DTO.OperadorCadastroRequest;
import com.example.gearrent.application.DTO.OperadorCadastroResponse;
import com.example.gearrent.application.DTO.PerfilAtualizacaoRequest;
import com.example.gearrent.application.DTO.PerfilConsultaResponse;
import com.example.gearrent.application.DTO.TrocaSenhaRequest;
import com.example.gearrent.application.DTO.UsuarioAtualizacaoRequest;
import com.example.gearrent.application.DTO.UsuarioAtualizacaoResponse;
import com.example.gearrent.application.DTO.UsuarioCadastroRequest;
import com.example.gearrent.application.DTO.UsuarioCadastroResponse;
import com.example.gearrent.application.DTO.UsuarioConsultaResponse;
import com.example.gearrent.application.DTO.UsuarioReativacaoResponse;
import com.example.gearrent.application.DTO.UsuarioStatusResponse;
import com.example.gearrent.domain.entities.Locadora;
import com.example.gearrent.domain.entities.Usuario;
import com.example.gearrent.domain.enums.PapelUsuario;
import com.example.gearrent.domain.enums.StatusCadastro;
import com.example.gearrent.domain.exception.CredenciaisInvalidasException;
import com.example.gearrent.domain.exception.EmailDuplicadoException;
import com.example.gearrent.domain.exception.RecursoNaoEncontradoException;
import com.example.gearrent.domain.exception.RegraDeNegocioException;
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

        // 2 - cadastra o usuário como ADMIN (dono da base) e vincula ele internamente
        Usuario usuario = new Usuario();
        usuario.setNome(request.nome().trim());
        usuario.setSobrenome(request.sobrenome().trim());
        usuario.setCpf(request.cpf().replaceAll("\\D", ""));
        usuario.setLogin(email);
        usuario.setSenha(passwordEncoder.encode(request.senha()));
        usuario.setData_nascimento(request.dataNascimento());
        usuario.setDataCadastro(LocalDateTime.now());
        usuario.setStatus(StatusCadastro.ATIVO);
        usuario.setRole(PapelUsuario.ADMIN);
        usuario.setLocadora(locadora);
        usuarioRepository.save(usuario);

        return new UsuarioCadastroResponse(usuario.getId(), "Usuário cadastrado com sucesso!");
    }

    // ---------- Operadores ----------

    @Transactional
    public OperadorCadastroResponse criarOperador(Long locadoraId, OperadorCadastroRequest request) {
        String email = request.email().trim();

        // O e-mail é único no sistema inteiro, não só na base
        if (usuarioRepository.existsByLogin(email)) {
            throw new EmailDuplicadoException(
                    "Já existe um usuário cadastrado com este e-mail. Use um e-mail diferente.");
        }

        Usuario operador = new Usuario();
        operador.setNome(request.nome().trim());
        operador.setSobrenome(request.sobrenome().trim());
        operador.setLogin(email);
        operador.setSenha(passwordEncoder.encode(request.senha()));
        operador.setRole(request.role());
        operador.setStatus(StatusCadastro.ATIVO);
        operador.setDataCadastro(LocalDateTime.now());
        // A base vem do administrador logado (token), nunca do corpo da requisição
        operador.setLocadora(locadoraRepository.getReferenceById(locadoraId));

        return OperadorCadastroResponse.de(usuarioRepository.save(operador));
    }

    @Transactional
    public UsuarioReativacaoResponse reativar(Long locadoraId, Long idUsuario) {
        // Usuário excluído (CANCELADO) não é encontrado aqui: só dá para reativar quem foi desativado
        Usuario usuario = buscarEntidade(locadoraId, idUsuario);

        if (usuario.getStatus() == StatusCadastro.ATIVO) {
            throw new RegraDeNegocioException("Este usuário já está ativo.");
        }

        usuario.setStatus(StatusCadastro.ATIVO);
        usuario.setDataAtualizacao(LocalDateTime.now());

        return new UsuarioReativacaoResponse(
                usuario.getId(),
                usuario.getLogin(),
                usuario.getStatus(),
                "O acesso de " + usuario.getNome() + " foi reativado com sucesso.");
    }

    // ---------- Perfil e senha ----------

    @Transactional(readOnly = true)
    public PerfilConsultaResponse consultarPerfil(Long locadoraId, Long idUsuario) {
        return PerfilConsultaResponse.de(buscarEntidade(locadoraId, idUsuario));
    }

    @Transactional
    public PerfilConsultaResponse atualizarPerfil(Long locadoraId, Long idUsuario, PerfilAtualizacaoRequest request) {
        Usuario usuario = buscarEntidade(locadoraId, idUsuario);

        String email = request.email().trim();

        if (usuarioRepository.existsByLoginAndIdNot(email, idUsuario)) {
            throw new EmailDuplicadoException(
                    "Já existe outro usuário cadastrado com este e-mail. Use um e-mail diferente.");
        }

        usuario.setNome(request.nome().trim());
        usuario.setSobrenome(request.sobrenome().trim());
        usuario.setLogin(email);
        usuario.setDataAtualizacao(LocalDateTime.now());

        return PerfilConsultaResponse.de(usuario);
    }

    @Transactional
    public void trocarSenha(Long locadoraId, Long idUsuario, TrocaSenhaRequest request) {
        if (!request.novaSenha().equals(request.confirmacaoSenha())) {
            throw new RegraDeNegocioException("A nova senha e a confirmação não são iguais. Digite novamente.");
        }

        Usuario usuario = buscarEntidade(locadoraId, idUsuario);

        if (!passwordEncoder.matches(request.senhaAtual(), usuario.getSenha())) {
            throw new CredenciaisInvalidasException("A senha atual não está correta.");
        }
        if (passwordEncoder.matches(request.novaSenha(), usuario.getSenha())) {
            throw new RegraDeNegocioException("A nova senha precisa ser diferente da senha atual.");
        }

        usuario.setSenha(passwordEncoder.encode(request.novaSenha()));
        usuario.setDataAtualizacao(LocalDateTime.now());
    }

    // ---------- Já existentes ----------

    @Transactional
    public UsuarioAtualizacaoResponse atualizar(Long idBase, Long idUsuario, UsuarioAtualizacaoRequest request) {
        Usuario usuario = buscarEntidade(idBase, idUsuario);
        String email = request.email().trim();
        String cpf = request.cpf().replaceAll("\\D", "");

        if (usuarioRepository.existsByLoginAndIdNot(email, idUsuario)) {
            throw new EmailDuplicadoException(
                    "Já existe outro usuário cadastrado com este e-mail. Use um e-mail diferente.");
        }
        if (usuarioRepository.existsByCpfAndLocadoraIdAndIdNot(cpf, idBase, idUsuario)) {
            throw new RegraDeNegocioException("Este CPF já está cadastrado para outro usuário da sua base.");
        }

        usuario.setNome(request.nome().trim());
        usuario.setSobrenome(request.sobrenome().trim());
        usuario.setCpf(cpf);
        usuario.setLogin(email);
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