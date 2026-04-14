package com.magicfield.backend.service;

import com.magicfield.backend.dto.AuthResponse;
import com.magicfield.backend.dto.LoginRequest;
import com.magicfield.backend.dto.RegisterRequest;
import com.magicfield.backend.entity.User;

import java.util.UUID;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse register(RegisterRequest request);
    User getUserById(UUID userId);
    User getUserByEmail(String email);
    void deleteUser(UUID userId);}
