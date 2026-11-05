package com.example.gearrent.application.DTO;

import java.time.LocalDateTime;

// Uso interno: a imagem pronta para ser devolvida pelo controller
public record AvatarArquivo(
        byte[] conteudo,
        String contentType,
        LocalDateTime atualizadoEm) {
}