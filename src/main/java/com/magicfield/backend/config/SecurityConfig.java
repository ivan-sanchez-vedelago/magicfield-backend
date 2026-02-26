package com.magicfield.backend.config;

import com.magicfield.backend.security.FirebaseAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final FirebaseAuthenticationFilter firebaseAuthenticationFilter;

    public SecurityConfig(FirebaseAuthenticationFilter firebaseAuthenticationFilter) {
        this.firebaseAuthenticationFilter = firebaseAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            // 🔥 MUY IMPORTANTE PARA CORS
            .cors(cors -> {})

            // sin csrf para api stateless
            .csrf(csrf -> csrf.disable())

            // sin sesiones
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // permisos
            .authorizeHttpRequests(auth -> auth
                // permitir preflight CORS
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()

                // endpoints públicos
                .requestMatchers("/api/public/**", "/health", "/api/products/**").permitAll()

                // checkout también público (o va a pedir auth)
                .requestMatchers("/api/orders/checkout").permitAll()

                .anyRequest().authenticated()
            )

            // firebase auth filter
            .addFilterBefore(firebaseAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
