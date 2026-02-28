package com.magicfield.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        // permitir cookies / auth headers
        config.setAllowCredentials(true);

        // ORIGENES PERMITIDOS
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:3000",
                "https://magicfield-frontend.vercel.app",
                "https://*.vercel.app"   // preview deployments
        ));

        // HEADERS
        config.setAllowedHeaders(List.of("*"));

        // METODOS HTTP
        config.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "OPTIONS"
        ));

        // CACHE preflight 1 hora
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }
}