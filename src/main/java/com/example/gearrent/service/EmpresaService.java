package com.example.gearrent.service;

import com.example.gearrent.DTO.EmpresaRequest;
import com.example.gearrent.DTO.EmpresaResponse;
import com.example.gearrent.entities.Empresa;
import com.example.gearrent.repository.EmpresaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class EmpresaService {


    private final EmpresaRepository empresaRepository;

    public EmpresaService(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    public Optional<EmpresaResponse> atualizarPorCnpj(String cnpj, EmpresaRequest request) {
        return empresaRepository.findByCnpj(cnpj).map(empresa -> {
            empresa.setRazaoSocial(request.nome());
            empresa.setCnpj(request.cnpj());
            empresa.setEmail(request.email());
            empresa.setTelefone(request.telefone());

            empresaRepository.save(empresa);

            return new EmpresaResponse(empresa.getId(), "Cadastro de Empresa Atualizado");
        });
    }

    @Transactional
    public void deleteLogico(Long id) {
        Empresa empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Empresa não encontrada com o ID: " + id));

        if (!Boolean.TRUE.equals(empresa.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Esta empresa já está desativada.");
        }

        empresa.setStatus(false);
        empresaRepository.save(empresa);
    }
}

