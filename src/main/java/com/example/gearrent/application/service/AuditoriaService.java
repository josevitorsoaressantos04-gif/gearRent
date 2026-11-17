package com.example.gearrent.application.service;

import com.example.gearrent.application.DTO.LogAuditoriaResponse;
import com.example.gearrent.domain.entities.LogAuditoria;
import com.example.gearrent.domain.entities.Usuario;
import com.example.gearrent.domain.repository.LogAuditoriaRepository;
import com.example.gearrent.domain.repository.UsuarioRepository;
import com.example.gearrent.infra.security.UsuarioAutenticado;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Serviço central de auditoria: identifica automaticamente o operador logado
 * a partir do contexto de segurança (JWT) e grava o registro na tabela log_auditoria.
 */
@Service
public class AuditoriaService {

    private final LogAuditoriaRepository logAuditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    public AuditoriaService(LogAuditoriaRepository logAuditoriaRepository,
                            UsuarioRepository usuarioRepository,
                            Clock clock) {
        this.logAuditoriaRepository = logAuditoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
    }

    /**
     * Registra um evento de auditoria vinculado à locadora atual.
     *
     * @param locadoraId ID da base (tenant)
     * @param modulo     Módulo do sistema (LOCACAO, CLIENTE, EQUIPAMENTO, OPERADOR, LOCADORA)
     * @param acao       Ação executada (CADASTRO, CADASTRO_RETROATIVO, ATUALIZACAO, DEVOLUCAO, etc.)
     * @param retroativo true quando uma locação é criada com data de retirada anterior à data atual
     * @param detalhes   Descrição legível da operação realizada
     */
    @Transactional
    public void registrar(Long locadoraId, String modulo, String acao, boolean retroativo, String detalhes) {
        if (locadoraId == null) return;

        Long usuarioId = null;
        String usuarioNome = "Sistema";

        // Recupera o usuário autenticado do token JWT sem precisar alterar a assinatura dos outros services
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioAutenticado principal) {
            usuarioId = principal.id();
            usuarioNome = usuarioRepository.findByIdAndLocadoraId(usuarioId, locadoraId)
                    .map(this::formatarNomeUsuario)
                    .orElse("Operador #" + usuarioId);
        }

        LogAuditoria log = new LogAuditoria();
        log.setLocadoraId(locadoraId);
        log.setUsuarioId(usuarioId);
        log.setUsuarioNome(usuarioNome);
        log.setModulo(modulo);
        log.setAcao(acao);
        log.setRetroativo(retroativo);
        log.setDetalhes(detalhes != null && detalhes.length() > 500 ? detalhes.substring(0, 500) : detalhes);
        log.setDataHora(LocalDateTime.now(clock));

        logAuditoriaRepository.save(log);
    }

    /**
     * Lista os últimos 200 registros de auditoria da locadora, permitindo filtrar
     * opcionalmente pelo módulo e/ou somente pelos cadastros retroativos.
     */
    @Transactional(readOnly = true)
    public List<LogAuditoriaResponse> listar(Long locadoraId, String modulo, Boolean apenasRetroativos) {
        return logAuditoriaRepository.findTop200ByLocadoraIdOrderByDataHoraDescIdDesc(locadoraId)
                .stream()
                .filter(log -> modulo == null || modulo.isBlank() || log.getModulo().equalsIgnoreCase(modulo.trim()))
                .filter(log -> !Boolean.TRUE.equals(apenasRetroativos) || log.isRetroativo())
                .map(LogAuditoriaResponse::de)
                .toList();
    }

    private String formatarNomeUsuario(Usuario u) {
        String nome = u.getNome() == null ? "" : u.getNome().trim();
        String sobrenome = u.getSobrenome() == null ? "" : u.getSobrenome().trim();
        String completo = (nome + " " + sobrenome).trim();
        return completo.isEmpty() ? u.getLogin() : completo;
    }
}
