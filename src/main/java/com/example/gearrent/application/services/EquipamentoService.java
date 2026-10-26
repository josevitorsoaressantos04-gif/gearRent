package com.example.gearrent.application.services;

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
import com.example.gearrent.domain.exceptions.EquipamentoIndisponivelException;
import com.example.gearrent.domain.exceptions.PatrimonioDuplicadoException;
import com.example.gearrent.domain.exceptions.RecursoNaoEncontradoException;
import com.example.gearrent.domain.exceptions.RegraDeNegocioException;
import com.example.gearrent.domain.repository.EquipamentoRepository;
import com.example.gearrent.domain.repository.LocadoraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}