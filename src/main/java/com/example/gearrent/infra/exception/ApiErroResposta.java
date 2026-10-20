package com.example.gearrent.infra.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ApiErroResposta(
        int status,
        LocalDateTime dataHora,
        List<String> mensagens
) {
    public static ApiErroResposta de(HttpStatus status, String... mensagem){
        return new ApiErroResposta(status.value(), LocalDateTime.now(), List.of(mensagem));
    }

    public static ApiErroResposta de(HttpStatus status, List<String> mensagem){
        return new ApiErroResposta(status.value(), LocalDateTime.now(), mensagem);
    }
}
