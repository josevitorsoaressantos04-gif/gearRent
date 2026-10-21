package com.example.gearrent.infra.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.emissor}")
    private String emissor;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String caminho = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || caminho.equals("/login")                    // ajuste para o seu caminho real
                || caminho.equals("/api/usuario/cadastro")
                || caminho.startsWith("/swagger-ui")
                || caminho.startsWith("/webjars")
                || caminho.startsWith("/swagger-resources")
                || caminho.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String cabecalho = request.getHeader("Authorization");

        if (cabecalho == null || !cabecalho.startsWith("Bearer ")) {
            responderNaoAutorizado(response);
            return;
        }

        String token = cabecalho.substring(7);

        try {
            DecodedJWT jwt = JWT.require(Algorithm.HMAC256(secret))
                    .withIssuer(emissor)
                    .build()
                    .verify(token);

            UsuarioAutenticado principal = new UsuarioAutenticado(
                    Long.valueOf(jwt.getSubject()),
                    jwt.getClaim("locadoraId").asLong());

            var autenticacao = new UsernamePasswordAuthenticationToken(
                    principal, null, List.of(new SimpleGrantedAuthority("ROLE_USUARIO")));

            SecurityContextHolder.getContext().setAuthentication(autenticacao);

        } catch (JWTVerificationException | NumberFormatException e) {
            responderNaoAutorizado(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void responderNaoAutorizado(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"status\":401,\"mensagens\":[\"Sua sessão expirou ou é inválida. Entre novamente.\"]}");
    }
}