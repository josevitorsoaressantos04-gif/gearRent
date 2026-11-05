package com.example.gearrent.presentation;

import com.example.gearrent.application.DTO.LocadoraAtualizacaoRequest;
import com.example.gearrent.application.DTO.LocadoraConsultaResponse;
import com.example.gearrent.application.service.LocadoraService;
// import do UsuarioAutenticado: use o pacote onde ele está no seu projeto
import com.example.gearrent.infra.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/locadora")
@Tag(name = "Locadora", description = "Dados cadastrais da sua base")
@SecurityRequirement(name = "bearerAuth")
public class LocadoraController {

    private final LocadoraService locadoraService;

    public LocadoraController(LocadoraService locadoraService) {
        this.locadoraService = locadoraService;
    }

    @GetMapping
    @Operation(summary = "Consultar dados da minha base")
    public LocadoraConsultaResponse consultar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return locadoraService.consultar(usuario.locadoraId());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualizar dados da minha base",
            description = "Altera nome fantasia, telefone e endereço. CNPJ não pode ser alterado.")
    @ApiResponse(responseCode = "200", description = "Dados atualizados")
    @ApiResponse(responseCode = "403", description = "Somente administradores")
    public LocadoraConsultaResponse atualizar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                              @Valid @RequestBody LocadoraAtualizacaoRequest request) {
        return locadoraService.atualizar(usuario.locadoraId(), request);
    }
}