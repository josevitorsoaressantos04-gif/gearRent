package com.example.gearrent.presentation;

import com.example.gearrent.application.DTO.EnderecoDTO;
import com.example.gearrent.application.service.ViaCepService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller que expõe a rota GET /api/cep/{cep} para o frontend preencher
 * automaticamente os campos de endereço (logradouro, bairro, cidade, UF e complemento)
 * a partir do EnderecoDTO.
 */
@RestController
@RequestMapping("/api/cep")
@Tag(name = "CEP", description = "Consulta pública de endereço via integração com o ViaCEP")
public class CepController {

    private final ViaCepService viaCepService;

    public CepController(ViaCepService viaCepService) {
        this.viaCepService = viaCepService;
    }

    @GetMapping("/{cep}")
    @Operation(
            summary = "Consultar endereço por CEP",
            description = "Consulta o serviço ViaCEP informando um CEP de 8 números (com ou sem máscara) "
                    + "e retorna os dados disponíveis preenchidos no EnderecoDTO (logradouro, complemento, bairro, cidade, estado e cep)."
    )
    @ApiResponse(responseCode = "200", description = "Endereço encontrado e retornado no formato EnderecoDTO")
    @ApiResponse(responseCode = "404", description = "CEP não encontrado na base do ViaCEP")
    @ApiResponse(responseCode = "422", description = "CEP informado não possui 8 dígitos numéricos ou falha temporária na consulta")
    public ResponseEntity<EnderecoDTO> consultar(
            @Parameter(description = "CEP com 8 números (ex.: 01001-000 ou 01001000)", example = "01001-000")
            @PathVariable String cep) {
        return ResponseEntity.ok(viaCepService.consultarPorCep(cep));
    }
}
