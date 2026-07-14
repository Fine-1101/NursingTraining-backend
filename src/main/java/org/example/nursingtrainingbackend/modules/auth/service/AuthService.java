package org.example.nursingtrainingbackend.modules.auth.service;

import org.example.nursingtrainingbackend.modules.auth.dto.LoginRequest;
import org.example.nursingtrainingbackend.modules.auth.dto.RegisterRequest;
import org.example.nursingtrainingbackend.modules.auth.vo.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    LoginResponse register(RegisterRequest request);
}
