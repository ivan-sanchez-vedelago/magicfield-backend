package com.magicfield.backend.service.impl;

import com.magicfield.backend.dto.AuthResponse;
import com.magicfield.backend.dto.LoginRequest;
import com.magicfield.backend.dto.RegisterRequest;
import com.magicfield.backend.entity.User;
import com.magicfield.backend.repository.UserRepository;
import com.magicfield.backend.service.AuthService;
import com.magicfield.backend.utils.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new RuntimeException("Este usuario fue creado con Firebase. Use ese método para iniciar sesión.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Contraseña incorrecta");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getName(),
                user.getLastName(),
                user.getPhone(),
                token
        );
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("El email ya está registrado");
        }

        User user = new User(
                request.getEmail(),
                request.getName(),
                request.getLastName(),
                request.getPhone(),

                passwordEncoder.encode(request.getPassword()),
                "ROLE_USER"
        );

        user = userRepository.save(user);
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getName(),
                user.getLastName(),
                user.getPhone(),
                token
        );
    }

    @Override
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
}
