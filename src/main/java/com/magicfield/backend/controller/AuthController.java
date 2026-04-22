package com.magicfield.backend.controller;

import com.magicfield.backend.dto.AuthResponse;
import com.magicfield.backend.dto.LoginRequest;
import com.magicfield.backend.dto.RegisterRequest;
import com.magicfield.backend.entity.User;
import com.magicfield.backend.service.AuthService;
import com.magicfield.backend.utils.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthService authService, JwtTokenProvider jwtTokenProvider) {
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        try {
            AuthResponse authResponse = authService.register(request);
            setAuthCookie(response, authResponse.getToken());
            return new ResponseEntity<>(authResponse, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        try {
            AuthResponse authResponse = authService.login(request);
            setAuthCookie(response, authResponse.getToken());
            return new ResponseEntity<>(authResponse, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<AuthResponse> getProfile(@CookieValue(name = "authToken", required = false) String token) {
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            String userId = jwtTokenProvider.getUserIdFromToken(token).toString();
            String email = jwtTokenProvider.getEmailFromToken(token);
            User user = authService.getUserByEmail(email);

            AuthResponse response = new AuthResponse(
                    userId,
                    user.getEmail(),
                    user.getName(),
                    user.getLastName(),
                    user.getPhone(),
                    null
            );

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        // Clear the auth cookie
        response.addHeader("Set-Cookie", "authToken=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=Strict");
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount(@CookieValue(name = "authToken", required = false) String token, HttpServletResponse response) {
        System.out.println("DELETE /api/auth/account - Token: " + (token != null ? "presente" : "ausente"));
        
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            System.err.println("Token inválido o ausente");
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            UUID userId = jwtTokenProvider.getUserIdFromToken(token);
            System.out.println("UserId extraído del token: " + userId);
            
            authService.deleteUser(userId);
            // Clear the auth cookie
            response.addHeader("Set-Cookie", "authToken=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=Strict");
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.err.println("Error al eliminar cuenta: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void setAuthCookie(HttpServletResponse response, String token) {
        // Set HttpOnly, Secure, SameSite cookie
        response.addHeader(
                "Set-Cookie",
                String.format(
                        "authToken=%s; Path=/; Max-Age=604800; HttpOnly; Secure; SameSite=Strict",
                        token
                )
        );
    }
}
