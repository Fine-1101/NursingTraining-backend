package org.example.nursingtrainingbackend.modules.auth.vo;

import org.example.nursingtrainingbackend.security.AuthenticatedUser;

public record UserInfo(Long id, String username, String nickname, String role) {
    public static UserInfo from(AuthenticatedUser user) {
        return new UserInfo(user.id(), user.username(), user.nickname(), user.role());
    }
}
