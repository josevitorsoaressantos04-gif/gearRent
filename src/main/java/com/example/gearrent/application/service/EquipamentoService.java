package com.example.gearrent.application.service;

import com.example.gearrent.application.DTO.EquipamentoAtualizacaoRequest;
import com.example.gearrent.application.DTO.EquipamentoAtualizacaoResponse;
import com.example.gearrent.application.DTO.EquipamentoCadastroRequest;
import com.example.gearrent.application.DTO.EquipamentoCadastroResponse;
import com.example.gearrent.application.DTO.EquipamentoConsultaResponse;
import com.example.gearrent.application.DTO.EquipamentoDisponivelResponse;
import com.example.gearrent.application.DTO.EquipamentoExclusaoResponse;
import com.example.gearrent.application.DTO.EquipamentoListagemResponse;
import com.example.gearrent.domain.entities.Equipamento;
import com.example.gearrent.domain.enums.StatusEquipamento;
import com.example.gearrent.domain.exception.EquipamentoIndisponivelException;
import com.example.gearrent.domain.exception.PatrimonioDuplicadoException;
import com.example.gearrent.domain.exception.RecursoNaoEncontradoException;
import com.example.gearrent.domain.exception.RegraDeNegocioException;
import com.example.gearrent.domain.repository.EquipamentoRepository;
import com.example.gearrent.domain.repository.EquipamentoSpecifications;
import com.example.gearrent.domain.repository.LocadoraRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.gearrent.application.DTO.PaginaResponse;
import com.example.gearrent.domain.enums.CategoriaEquipamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.Set;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EquipamentoService {

    private final EquipamentoRepository equipamentoRepository;
    private final LocadoraRepository locadoraRepository;

    public EquipamentoService(EquipamentoRepository equipamentoRepository,
                              LocadoraRepository locadoraRepository) {
        this.equipamentoRepository = equipamentoRepository;
        this.locadoraRepository = locadoraRepository;
    }

    @Transactional
    public EquipamentoCadastroResponse cadastrar(Long locadoraId, EquipamentoCadastroRequest request) {
        String patrimonio = limpar(request.numeroPatrimonio());

        if (patrimonio != null
                && equipamentoRepository.existsByLocadoraIdAndNumeroPatrimonio(locadoraId, patrimonio)) {
            throw new PatrimonioDuplicadoException(
                    "Já existe um equipamento cadastrado com este número de patrimônio.");
        }

        Equipamento equipamento = new Equipamento();
        equipamento.setCodigoSequencial(equipamentoRepository.buscarUltimoCodigo(locadoraId) + 1);
        equipamento.setNome(request.nome().trim());
        equipamento.setModelo(limpar(request.modelo()));
        equipamento.setNumeroPatrimonio(patrimonio);
        equipamento.setCategoria(request.categoria());
        equipamento.setValorDiaria(request.valorDiaria());
        equipamento.setStatus(StatusEquipamento.DISPONIVEL);
        equipamento.setDataCadastro(LocalDateTime.now());
        equipamento.setLocadora(locadoraRepository.getReferenceById(locadoraId));

        Equipamento salvo = equipamentoRepository.save(equipamento);

        return new EquipamentoCadastroResponse(
                salvo.getId(), salvo.getCodigoSequencial(), salvo.getNome(),
                "Equipamento cadastrado com sucesso!");
    }

    @Transactional(readOnly = true)
    public List<EquipamentoListagemResponse> listar(Long locadoraId, String nome) {
        return equipamentoRepository
                .findByLocadoraIdAndStatusNotAndNomeContainingIgnoreCaseOrderByNomeAsc(
                        locadoraId, StatusEquipamento.CANCELADO, termo(nome))
                .stream()
                .map(EquipamentoListagemResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EquipamentoDisponivelResponse> listarDisponiveis(Long locadoraId, String nome) {
        return equipamentoRepository
                .findByLocadoraIdAndStatusAndNomeContainingIgnoreCaseOrderByNomeAsc(
                        locadoraId, StatusEquipamento.DISPONIVEL, termo(nome))
                .stream()
                .map(EquipamentoDisponivelResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public EquipamentoConsultaResponse buscarPorId(Long locadoraId, Long id) {
        return EquipamentoConsultaResponse.de(buscarEntidade(locadoraId, id));
    }

    @Transactional
    public EquipamentoAtualizacaoResponse atualizar(Long locadoraId, Long id,
                                                    EquipamentoAtualizacaoRequest request) {
        Equipamento equipamento = buscarEntidade(locadoraId, id);

        String patrimonio = limpar(request.numeroPatrimonio());
        if (patrimonio != null
                && equipamentoRepository.existsByLocadoraIdAndNumeroPatrimonioAndIdNot(locadoraId, patrimonio, id)) {
            throw new PatrimonioDuplicadoException(
                    "Já existe outro equipamento cadastrado com este número de patrimônio.");
        }

        if (request.status() != null && request.status() != equipamento.getStatus()) {
            if (request.status() != StatusEquipamento.DISPONIVEL
                    && request.status() != StatusEquipamento.MANUTENCAO) {
                throw new RegraDeNegocioException(
                        "A situação do equipamento só pode ser alterada para Disponível ou Em manutenção.");
            }
            if (equipamento.getStatus() == StatusEquipamento.ALUGADO) {
                throw new EquipamentoIndisponivelException(
                        "Este equipamento está alugado no momento. Registre a devolução antes de alterar a situação dele.");
            }
            equipamento.setStatus(request.status());
        }

        equipamento.setNome(request.nome().trim());
        equipamento.setModelo(limpar(request.modelo()));
        equipamento.setNumeroPatrimonio(patrimonio);
        equipamento.setCategoria(request.categoria());
        equipamento.setValorDiaria(request.valorDiaria());
        equipamento.setDataAtualizacao(LocalDateTime.now());

        return new EquipamentoAtualizacaoResponse(
                equipamento.getId(), equipamento.getNome(), "Equipamento atualizado com sucesso!");
    }

    @Transactional
    public EquipamentoExclusaoResponse excluir(Long locadoraId, Long id) {
        Equipamento equipamento = buscarEntidade(locadoraId, id);

        if (equipamento.getStatus() == StatusEquipamento.ALUGADO) {
            throw new EquipamentoIndisponivelException(
                    "Este equipamento está alugado no momento. Registre a devolução antes de excluí-lo.");
        }

        equipamento.setStatus(StatusEquipamento.CANCELADO);
        equipamento.setDataAtualizacao(LocalDateTime.now());

        return new EquipamentoExclusaoResponse(equipamento.getId(), "Equipamento excluído com sucesso!");
    }

    private Equipamento buscarEntidade(Long locadoraId, Long id) {
        return equipamentoRepository.findByIdAndLocadoraId(id, locadoraId)
                .filter(e -> e.getStatus() != StatusEquipamento.CANCELADO)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Não encontramos este equipamento. Confira os dados e tente novamente."));
    }

    private String termo(String nome) {
        return nome == null ? "" : nome.trim();
    }

    private String limpar(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }

    private static final int TAMANHO_MAXIMO_PAGINA = 50;
    private static final Set<String> CAMPOS_ORDENAVEIS = Set.of(
            "nome", "modelo", "categoria", "status", "valorDiaria", "codigoSequencial", "dataCadastro");

    @Transactional(readOnly = true)
    public PaginaResponse<EquipamentoListagemResponse> listar(Long locadoraId,
                                                              String nome,
                                                              CategoriaEquipamento categoria,
                                                              StatusEquipamento status,
                                                              Pageable pageable) {
        // locadoraId SEMPRE primeiro; excluídos nunca entram; os filtros opcionais vêm depois
        Specification<Equipamento> filtro = EquipamentoSpecifications.daLocadora(locadoraId)
                .and(EquipamentoSpecifications.naoCancelado());

        if (nome != null && !nome.isBlank()) {
            filtro = filtro.and(EquipamentoSpecifications.comNome(nome));
        }
        if (categoria != null) {
            filtro = filtro.and(EquipamentoSpecifications.comCategoria(categoria));
        }
        if (status != null) {
            filtro = filtro.and(EquipamentoSpecifications.comStatus(status));
        }

        Page<Equipamento> pagina = equipamentoRepository.findAll(filtro, sanitizar(pageable));
        return PaginaResponse.de(pagina.map(EquipamentoListagemResponse::de));
    }

    // Só deixa ordenar por campos conhecidos e limita o tamanho da página
    private Pageable sanitizar(Pageable pageable) {
        for (Sort.Order ordem : pageable.getSort()) {
            if (!CAMPOS_ORDENAVEIS.contains(ordem.getProperty())) {
                throw new RegraDeNegocioException("Não é possível ordenar por \"" + ordem.getProperty()
                        + "\". Use: nome, modelo, categoria, status, valorDiaria, codigoSequencial ou dataCadastro.");
            }
        }
        int tamanho = Math.min(pageable.getPageSize(), TAMANHO_MAXIMO_PAGINA);
        // "id" como desempate mantém a ordem estável entre as páginas
        return PageRequest.of(pageable.getPageNumber(), tamanho, pageable.getSort().and(Sort.by("id")));
    }
}