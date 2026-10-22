package com.example.gearrent.infra.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.gearrent.domain.entities.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class TokenService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.emissor}")
    private String emissor;

    @Value("${jwt.expiracao-minutos}")
    private long expiracaoMinutos;

    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();
        return JWT.create()
                .withIssuer(emissor)
                .withSubject(usuario.getId().toString())
                .withClaim("locadoraId", usuario.getLocadora().getId())
                .withIssuedAt(agora)
                .withExpiresAt(agora.plus(expiracaoMinutos, ChronoUnit.MINUTES))
                .sign(Algorithm.HMAC256(secret));
    }

    public Optional<UsuarioAutenticado> validar(String token) {
        try {
            DecodedJWT jwt = JWT.require(Algorithm.HMAC256(secret))
                    .withIssuer(emissor)
                    .build()
                    .verify(token);

            Long locadoraId = jwt.getClaim("locadoraId").asLong();
            if (locadoraId == null) {
                return Optional.empty();
            }
            return Optional.of(new UsuarioAutenticado(Long.valueOf(jwt.getSubject()), locadoraId));

        } catch (JWTVerificationException | NumberFormatException e) {
            return Optional.empty();
        }
    }
}