package com.example.gearrent.domain.entities;

import com.example.gearrent.domain.enums.StatusLocacao;
import com.example.gearrent.domain.exception.RegraDeNegocioException;
import com.example.gearrent.domain.exception.TransicaoStatusInvalidaException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocacaoCancellationTest {

    @Test
    void cancelaLocacaoAntesDaDataDeRetirada() {
        Locacao locacao = locacao(StatusLocacao.EM_ANDAMENTO, LocalDateTime.of(2026, 11, 12, 0, 0));

        locacao.cancelar(LocalDateTime.of(2026, 11, 11, 12, 0));

        assertEquals(StatusLocacao.CANCELADA, locacao.getStatus());
    }

    @Test
    void rejeitaCancelamentoNaDataDaRetirada() {
        Locacao locacao = locacao(StatusLocacao.EM_ANDAMENTO, LocalDateTime.of(2026, 11, 12, 0, 0));

        assertThrows(RegraDeNegocioException.class,
                () -> locacao.cancelar(LocalDateTime.of(2026, 11, 12, 0, 0)));
        assertEquals(StatusLocacao.EM_ANDAMENTO, locacao.getStatus());
    }

    @Test
    void rejeitaCancelamentoQuandoLocacaoNaoEstaEmAndamento() {
        Locacao locacao = locacao(StatusLocacao.ATRASADA, LocalDateTime.of(2026, 11, 12, 0, 0));

        assertThrows(TransicaoStatusInvalidaException.class,
                () -> locacao.cancelar(LocalDateTime.of(2026, 11, 11, 12, 0)));
    }

    private Locacao locacao(StatusLocacao status, LocalDateTime dataRetirada) {
        Locacao locacao = new Locacao();
        locacao.setStatus(status);
        locacao.setDataRetirada(dataRetirada);
        return locacao;
    }
}
