package org.example.nursingtrainingbackend.security;

public record AuthenticatedUser(Long id, String username, String nickname, String role) {

}

