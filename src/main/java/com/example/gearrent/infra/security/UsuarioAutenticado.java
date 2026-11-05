package com.example.gearrent.infra.security;

import com.example.gearrent.domain.enums.PapelUsuario;

public record UsuarioAutenticado(Long id, Long locadoraId, PapelUsuario role) {
}