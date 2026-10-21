package com.example.gearrent.infra.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.gearrent.domain.entities.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Component
public class TokenService {

    private static final String EMISSOR = "gearrent";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiracao-minutos}")
    private long expiracaoMinutos;

    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();
        return JWT.create()
                .withIssuer(EMISSOR)
                .withSubject(usuario.getId().toString())
                .withClaim("locadoraId", usuario.getLocadora().getId())
                .withIssuedAt(agora)
                .withExpiresAt(agora.plus(expiracaoMinutos, ChronoUnit.MINUTES))
                .sign(Algorithm.HMAC256(secret));
    }

    public Optional<DecodedJWT> validar(String token) {
        try {
            return Optional.of(JWT.require(Algorithm.HMAC256(secret))
                    .withIssuer(EMISSOR)
                    .build()
                    .verify(token));
        } catch (JWTVerificationException e) {
            return Optional.empty();
        }
    }
}