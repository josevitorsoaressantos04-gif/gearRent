package com.example.gearrent;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Libera todas as rotas da API (/clientes, /equipamentos, /contratos)
                .allowedOrigins("http://localhost:5173", "http://localhost:3000") // Servidores de dev do React
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS") // Verbos HTTP permitidos
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}