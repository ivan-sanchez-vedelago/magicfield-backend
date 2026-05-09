package com.magicfield.backend.config;

import com.magicfield.backend.security.FirebaseAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
                // Preflight CORS
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Públicos
                .requestMatchers("/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/orders/checkout").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/orders/user/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/orders/*/finalize").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/orders/*/cancel").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/sales-audit/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/sales-audit").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/banners").permitAll()

                // Auth endpoints (public access for login/register, profile validated in controller)
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/api/auth/account").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/profile").permitAll()

                // Privados (requieren autenticación)
                .requestMatchers(HttpMethod.POST, "/api/products").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.PUT, "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.PATCH, "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/api/images/**").permitAll()
                .requestMatchers("/api/banners/**").permitAll()
                
                .requestMatchers("/api/public/**").permitAll()
                .anyRequest().authenticated()
            )
            // Auth firebase
            .addFilterBefore(
                firebaseAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}