package com.example.gearrent.application.DTO;

import com.example.gearrent.domain.enums.StatusLocacao;

import java.time.LocalDateTime;
import java.util.List;

public record ClienteHistoricoLocacaoDTO(
        Long idLocacao,
        Long codigoSequencialLocacao,
        List<String> equipamentos,   // nomes
        LocalDateTime dataRetirada,
        LocalDateTime dataPrevista,
        StatusLocacao status) {
}
