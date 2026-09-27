package com.example.gearrent.service;

import com.example.gearrent.DTO.ContratoPutRequest;
import com.example.gearrent.DTO.ContratoRequest;
import com.example.gearrent.DTO.ContratoResponse;
import com.example.gearrent.DTO.MensagemResponse;
import com.example.gearrent.entities.Cliente;
import com.example.gearrent.entities.Contrato;
import com.example.gearrent.entities.Equipamento;
import com.example.gearrent.exception.RegraNegocioException;
import com.example.gearrent.repository.ClienteRepository;
import com.example.gearrent.repository.ContratoRepository;
import com.example.gearrent.repository.EquipamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContratoService {

    private final ContratoRepository contratoRepository;
    private final ClienteRepository clienteRepository;
    private final EquipamentoRepository equipamentoRepository;

    public ContratoService(ContratoRepository contratoRepository,
                           ClienteRepository clienteRepository,
                           EquipamentoRepository equipamentoRepository) {
        this.contratoRepository = contratoRepository;
        this.clienteRepository = clienteRepository;
        this.equipamentoRepository = equipamentoRepository;
    }

    @Transactional(readOnly = true)
    public List<ContratoResponse> listarTodos() {
        return contratoRepository.findAll().stream()
                .map(this::mapearParaResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ContratoResponse buscarPorId(Long id) {
        return mapearParaResponse(buscarEntidadePorId(id));
    }

    @Transactional
    public MensagemResponse criarContrato(ContratoRequest request) {
        if (request.dataDevolucaoPrevista().isBefore(request.dataRetirada())) {
            throw new RegraNegocioException("A data de devolução não pode ser anterior à retirada.");
        }

        // Prevenção de Overbooking bloqueando a criação se as datas conflitarem
        if (contratoRepository.isEquipamentosOcupadosNoPeriodo(
                request.equipamentoIds(), request.dataRetirada(), request.dataDevolucaoPrevista())) {
            throw new RegraNegocioException("Um ou mais equipamentos selecionados já estão alugados neste período.");
        }

        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new RegraNegocioException("Cliente não encontrado."));

        List<Equipamento> equipamentos = equipamentoRepository.findAllById(request.equipamentoIds());
        if (equipamentos.size() != request.equipamentoIds().size()) {
            throw new RegraNegocioException("Um ou mais equipamentos informados não existem no sistema.");
        }

        Contrato contrato = new Contrato();
        contrato.setCliente(cliente);
        contrato.setEquipamento(equipamentos);
        contrato.setDataRetirada(request.dataRetirada());
        contrato.setDataDevolucaoPrevista(request.dataDevolucaoPrevista());
        contrato.setValorAcordado(request.valorAcordado());
        contrato.setStatus(true);

        contratoRepository.save(contrato);
        return new MensagemResponse(contrato.getId(), "Contrato gerado e maquinário reservado com sucesso.");
    }

    @Transactional
    public MensagemResponse atualizarContrato(Long id, ContratoPutRequest request) {
        Contrato contrato = buscarEntidadePorId(id);

        if (!contrato.getStatus()) {
            throw new RegraNegocioException("Não é possível realizar aditivos em um contrato inativo/cancelado.");
        }

        if (request.dataDevolucaoPrevista().isBefore(contrato.getDataRetirada())) {
            throw new RegraNegocioException("A nova data de devolução não pode ser anterior à data de retirada.");
        }

        contrato.setDataDevolucaoPrevista(request.dataDevolucaoPrevista());
        contratoRepository.save(contrato);

        return new MensagemResponse(contrato.getId(), "Aditivo de prazo registrado com sucesso.");
    }

    @Transactional
    public void deleteLogico(Long id) {
        Contrato contrato = buscarEntidadePorId(id);

        if (!Boolean.TRUE.equals(contrato.getStatus())) {
            throw new RegraNegocioException("Este contrato já está cancelado/desativado.");
        }

        contrato.setStatus(false);
        contratoRepository.save(contrato);
    }

    private Contrato buscarEntidadePorId(Long id) {
        return contratoRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Contrato não encontrado com o ID: " + id));
    }

    private ContratoResponse mapearParaResponse(Contrato c) {
        return new ContratoResponse(
                c.getId(), c.getCliente().getId(), c.getDataRetirada(),
                c.getDataDevolucaoPrevista(), c.getValorAcordado(), c.getStatus()
        );
    }
}