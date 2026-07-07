package org.example.nursingtrainingbackend.modules.auth.vo;

public record LoginResponse(String tokenType, String accessToken, long expiresIn, UserInfo user) {}
