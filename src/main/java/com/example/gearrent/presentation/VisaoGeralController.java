package com.example.gearrent.presentation;

import com.example.gearrent.application.DTO.*;
import com.example.gearrent.application.service.VisaoGeralQueryService;
import com.example.gearrent.infra.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/visao-geral")
@RequiredArgsConstructor
@Tag(name = "Visão Geral", description = "Consultas otimizadas para o dashboard (CQRS - somente leitura)")
public class VisaoGeralController {

    private final VisaoGeralQueryService queryService;

    @Operation(
            summary = "KPIs consolidados da locadora logada",
            description = "Retorna os indicadores do topo do dashboard: total de locações, em andamento, "
                    + "atrasadas, faturamento total, receita do mês acumulada e valores em aberto. "
                    + "Os dados são restritos à locadora do usuário autenticado."
    )
    @GetMapping("/resumo")
    public ResponseEntity<VisaoGeralResumoDTO> resumo(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(queryService.buscarResumo(usuario.locadoraId()));
    }

    @Operation(
            summary = "Opções agregadas para popular os dropdowns de filtro",
            description = "Retorna, em uma única chamada, as categorias e modelos de equipamentos cadastrados "
                    + "e a lista enxuta de clientes (id, código sequencial e nome) da locadora do usuário autenticado."
    )
    @GetMapping("/filtros-opcoes")
    public ResponseEntity<OpcoesFiltrosDTO> filtrosOpcoes(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(queryService.buscarOpcoesFiltros(usuario.locadoraId()));
    }

    @Operation(
            summary = "Listagem sintética de locações com filtros combinados",
            description = "Retorna a tabela do dashboard de forma paginada. Todos os filtros são opcionais e "
                    + "combinados entre si: status, clienteId, categoria, modelo, numeroPatrimonio e período "
                    + "(dataInicio e dataFim, aplicados sobre a data de retirada). "
                    + "Ordenação permitida: dataRetirada, dataPrevista, status e valorFinalCobrado "
                    + "(padrão: dataRetirada decrescente). Tamanho máximo da página: 50."
    )
    @GetMapping("/locacoes")
    public ResponseEntity<Page<LocacaoListagemSinteticaDTO>> locacoes(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @ParameterObject FiltroLocacaoDTO filtro,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(queryService.listarLocacoes(usuario.locadoraId(), filtro, pageable));
    }
}