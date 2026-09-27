package com.example.gearrent.service;

import com.example.gearrent.DTO.*;
import com.example.gearrent.entities.Equipamento;
import com.example.gearrent.entities.enums.StatusEquipamento;
import com.example.gearrent.exception.RegraNegocioException;
import com.example.gearrent.repository.EquipamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EquipamentoService {

    private final EquipamentoRepository equipamentoRepository;

    public EquipamentoService(EquipamentoRepository equipamentoRepository) {
        this.equipamentoRepository = equipamentoRepository;
    }

    @Transactional(readOnly = true)
    public List<EquipamentoResponse> listarTodos() {
        return equipamentoRepository.findAll().stream()
                .map(this::mapearParaResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EquipamentoResponse buscarPorId(Long id) {
        Equipamento equipamento = buscarEntidadePorId(id);
        return mapearParaResponse(equipamento);
    }

    @Transactional
    public MensagemResponse criarEquipamento(EquipamentoRequest request) {
        if (equipamentoRepository.existsByNumeroPatrimonio(request.numeroPatrimonio())) {
            throw new RegraNegocioException("Já existe um equipamento cadastrado com este Número de Patrimônio.");
        }

        Equipamento equipamento = new Equipamento();
        equipamento.setNome(request.nome());
        equipamento.setNumeroPatrimonio(request.numeroPatrimonio());
        equipamento.setModelo(request.modelo());
        equipamento.setValorDiariaBase(request.valorDiariaBase());
        equipamento.setStatus(StatusEquipamento.DISPONIVEL); // Inicialização obrigatória
        equipamento.setAtivo(true);

        equipamentoRepository.save(equipamento);
        return new MensagemResponse(equipamento.getId(), "Equipamento cadastrado com sucesso.");
    }

    @Transactional
    public MensagemResponse atualizarEquipamento(Long id, EquipamentoPutRequest request) {
        Equipamento equipamento = buscarEntidadePorId(id);

        if (!equipamento.getAtivo()) {
            throw new RegraNegocioException("Não é possível alterar um equipamento inativo.");
        }

        equipamento.setNome(request.nome());
        equipamento.setModelo(request.modelo());
        equipamento.setValorDiariaBase(request.valorDiariaBase());

        equipamentoRepository.save(equipamento);
        return new MensagemResponse(equipamento.getId(), "Equipamento atualizado com sucesso.");
    }

    @Transactional
    public void inativarEquipamento(Long id) {
        Equipamento equipamento = buscarEntidadePorId(id);

        if (!equipamento.getAtivo()) {
            throw new RegraNegocioException("Este equipamento já está inativo.");
        }

        equipamento.setAtivo(false);
        equipamentoRepository.save(equipamento);
    }

    private Equipamento buscarEntidadePorId(Long id) {
        return equipamentoRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Equipamento não encontrado com o ID: " + id));
    }

    private EquipamentoResponse mapearParaResponse(Equipamento eq) {
        return new EquipamentoResponse(
                eq.getId(), eq.getNome(), eq.getNumeroPatrimonio(),
                eq.getModelo(), eq.getValorDiariaBase(),
                eq.getStatus().name(), eq.getAtivo()
        );
    }
}