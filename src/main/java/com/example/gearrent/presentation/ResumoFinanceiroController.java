package com.example.gearrent.presentation;

import com.example.gearrent.application.DTO.ResumoFinanceiroResponse;
import com.example.gearrent.application.service.ResumoFinanceiroService;
import com.example.gearrent.infra.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/financeiro")
@Tag(name = "Financeiro", description = "Resumo de receita e valores em aberto da sua locadora")
@SecurityRequirement(name = "bearerAuth")
public class ResumoFinanceiroController {

    private final ResumoFinanceiroService resumoFinanceiroService;

    public ResumoFinanceiroController(ResumoFinanceiroService resumoFinanceiroService) {
        this.resumoFinanceiroService = resumoFinanceiroService;
    }

    @GetMapping("/resumo")
    @Operation(summary = "Resumo financeiro por período",
            description = "Receita (locações concluídas, por mês da devolução) e valores em aberto de hoje. "
                    + "Sem datas, usa do dia 1 do mês atual até hoje. Máximo de 24 meses.")
    @ApiResponse(responseCode = "200", description = "Resumo gerado")
    @ApiResponse(responseCode = "422", description = "Período inválido")
    public ResumoFinanceiroResponse resumo(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {

        return resumoFinanceiroService.gerar(usuario.locadoraId(), dataInicio, dataFim);
    }
}