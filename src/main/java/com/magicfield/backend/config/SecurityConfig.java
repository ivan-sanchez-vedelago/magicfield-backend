package com.magicfield.backend.config;

import com.magicfield.backend.security.FirebaseAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final FirebaseAuthenticationFilter firebaseAuthenticationFilter;

    public SecurityConfig(FirebaseAuthenticationFilter firebaseAuthenticationFilter) {
        this.firebaseAuthenticationFilter = firebaseAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {

        http
            // 🔥 usar configuración CORS global
            .cors(cors -> cors.configurationSource(corsConfigurationSource))

            // API stateless
            .csrf(csrf -> csrf.disable())

            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .authorizeHttpRequests(auth -> auth

                // preflight CORS
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // públicos
                .requestMatchers("/health").permitAll()
                .requestMatchers("/api/products/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/orders/checkout").permitAll()

                // resto protegido
                .anyRequest().authenticated()
            )

            // auth firebase
            .addFilterBefore(
                firebaseAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}