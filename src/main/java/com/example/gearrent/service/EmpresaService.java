package com.example.gearrent.service;

import com.example.gearrent.DTO.EmpresaPutRequest;
import com.example.gearrent.DTO.EmpresaRequest;
import com.example.gearrent.DTO.EmpresaResponse;
import com.example.gearrent.DTO.MensagemResponse;
import com.example.gearrent.entities.Empresa;
import com.example.gearrent.exception.RegraNegocioException;
import com.example.gearrent.repository.EmpresaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepository;

    public EmpresaService(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    @Transactional(readOnly = true)
    public List<EmpresaResponse> listarTodas() {
        return empresaRepository.findAll().stream()
                .map(this::mapearParaResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmpresaResponse buscarPorId(Long id) {
        Empresa empresa = buscarEntidadePorId(id);
        return mapearParaResponse(empresa);
    }

    @Transactional
    public MensagemResponse criarEmpresa(EmpresaRequest request) {
        if (empresaRepository.existsByCnpj(request.cnpjLimpo())) {
            throw new RegraNegocioException("O CNPJ informado já está cadastrado no sistema.");
        }
        if (empresaRepository.existsByEmail(request.email())) {
            throw new RegraNegocioException("Este e-mail já está em uso por outra empresa.");
        }

        Empresa empresa = new Empresa();
        empresa.setNome(request.nome());
        empresa.setCnpj(request.cnpjLimpo()); // Salva apenas os 14 números lógicos
        empresa.setEmail(request.email().toLowerCase().trim());
        empresa.setTelefone(request.telefoneLimpo());
        empresa.setStatus(true); // Consistência de nomenclatura com as demais entidades

        empresaRepository.save(empresa);
        return new MensagemResponse(empresa.getId(), "Empresa cadastrada com sucesso.");
    }

    @Transactional
    public MensagemResponse atualizarEmpresa(Long id, EmpresaPutRequest request) {
        Empresa empresa = buscarEntidadePorId(id);

        if (!empresa.getStatus()) {
            throw new RegraNegocioException("Não é possível alterar os dados de uma empresa inativa.");
        }

        if (!empresa.getEmail().equalsIgnoreCase(request.email()) && empresaRepository.existsByEmail(request.email())) {
            throw new RegraNegocioException("O e-mail informado já está em uso por outra empresa.");
        }

        empresa.setNome(request.nome());
        empresa.setEmail(request.email().toLowerCase().trim());
        empresa.setTelefone(request.telefoneLimpo());
        // O CNPJ permanece blindado, não sendo atualizado aqui

        empresaRepository.save(empresa);
        return new MensagemResponse(empresa.getId(), "Cadastro corporativo atualizado com sucesso.");
    }

    @Transactional
    public void inativarEmpresa(Long id) {
        Empresa empresa = buscarEntidadePorId(id);

        if (Boolean.FALSE.equals(empresa.getStatus())) {
            throw new RegraNegocioException("Esta empresa já se encontra desativada.");
        }

        empresa.setStatus(false);
        empresaRepository.save(empresa);
    }

    private Empresa buscarEntidadePorId(Long id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Empresa não encontrada com o ID: " + id));
    }

    private EmpresaResponse mapearParaResponse(Empresa empresa) {
        return new EmpresaResponse(
                empresa.getId(),
                empresa.getNome(),
                empresa.getCnpj(),
                empresa.getTelefone(),
                empresa.getEmail(),
                empresa.getStatus()
        );
    }
}