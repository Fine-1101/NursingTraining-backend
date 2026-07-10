package org.example.nursingtrainingbackend.modules.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.result.Result;
import org.example.nursingtrainingbackend.modules.auth.dto.LoginRequest;
import org.example.nursingtrainingbackend.modules.auth.dto.RegisterRequest;
import org.example.nursingtrainingbackend.modules.auth.service.AuthService;
import org.example.nursingtrainingbackend.modules.auth.vo.LoginResponse;
import org.example.nursingtrainingbackend.modules.auth.vo.UserInfo;
import org.example.nursingtrainingbackend.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @GetMapping("/me")
    public Result<UserInfo> me(@AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(UserInfo.from(user));
    }
}
