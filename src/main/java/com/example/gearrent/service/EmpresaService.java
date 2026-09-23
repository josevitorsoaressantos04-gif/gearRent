package com.example.gearrent.service;

import com.example.gearrent.DTO.EmpresaRequest;
import com.example.gearrent.DTO.EmpresaResponse;
import com.example.gearrent.entities.Empresa;
import com.example.gearrent.repository.EmpresaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class EmpresaService {


    private final EmpresaRepository empresaRepository;

    public EmpresaService(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    public Optional<EmpresaResponse> atualizarPorCnpj(String cnpj, EmpresaRequest request) {
        return empresaRepository.findByCnpj(cnpj).map(empresa -> {
            empresa.setNome(request.nome());
            empresa.setCnpj(request.cnpj());
            empresa.setEmail(request.email());
            empresa.setTelefone(request.telefone());

            empresaRepository.save(empresa);

            return new EmpresaResponse(empresa.getId(), "Cadastro de Empresa Atualizado");
        });
    }

    public void deleteLogico(Long id){
        Empresa empresa = empresaRepository.findById(id).orElse(ResponseEntity.notFound().build());
        empresa.setStatus(false);
        empresaRepository.save(empresa);
    }
}

