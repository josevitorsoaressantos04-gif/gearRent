package com.example.gearrent.infra.security;

import com.example.gearrent.infra.uptime.HealthRateLimitFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Clock;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // ativa o @PreAuthorize
public class SecurityConfig {

    @Value("${app.uptimerobot.health.rate-limit.requisicoes-por-minuto:10}")
    private int healthRateLimitPorMinuto;

    @Value("${app.uptimerobot.health.rate-limit.max-ips-cache:10000}")
    private int healthRateLimitMaxIps;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           TokenService tokenService,
                                           Clock clock) throws Exception {
        HealthRateLimitFilter healthRateLimitFilter =
                new HealthRateLimitFilter(clock, healthRateLimitPorMinuto, healthRateLimitMaxIps);

        http
                .cors(Customizer.withDefaults())      // <- nova linha
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ÚNICO lugar com a lista de rotas públicas
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/**", "/api/usuarios/cadastro").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/cep/**", "/api/health", "/api/health/**", "/api/ping").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                                "/webjars/**", "/swagger-resources/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(erro -> erro
                        .authenticationEntryPoint((req, res, ex) -> {
                            res.setStatus(401);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"status\":401,\"mensagens\":[\"Sua sessão expirou ou é inválida. Entre novamente.\"]}");
                        })
                        .accessDeniedHandler((req, res, ex) -> {
                            res.setStatus(403);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"status\":403,\"mensagens\":[\"Você não tem permissão para fazer isso.\"]}");
                        }))
                .addFilterBefore(new JwtFilter(tokenService), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(healthRateLimitFilter, JwtFilter.class);

        return http.build();
    }
}