package com.example.gearrent.infra.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    public JwtFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String cabecalho = request.getHeader("Authorization");

        if (cabecalho != null && cabecalho.startsWith("Bearer ")) {
            String token = cabecalho.substring(7).trim();

            tokenService.validar(token).ifPresentOrElse(
                    usuario -> {
                        var autenticacao = new UsernamePasswordAuthenticationToken(
                                usuario, null, List.of(new SimpleGrantedAuthority("ROLE_USUARIO")));
                        SecurityContextHolder.getContext().setAuthentication(autenticacao);
                    },
                    () -> logger.warn("Token rejeitado (inválido ou expirado) em "
                            + request.getMethod() + " " + request.getRequestURI())
            );
        }

        // Nunca barra aqui: a SecurityConfig decide se a rota exige autenticação
        filterChain.doFilter(request, response);
    }
}