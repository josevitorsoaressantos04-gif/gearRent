package com.example.gearrent.application.DTO;

import java.time.LocalDateTime;

public record AvatarResponse(
        String mensagem,
        LocalDateTime avatarAtualizadoEm) {
}