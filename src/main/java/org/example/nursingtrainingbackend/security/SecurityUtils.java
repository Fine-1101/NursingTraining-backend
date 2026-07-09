package org.example.nursingtrainingbackend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * 获取当前登录用户信息
     */
    public static AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser)) {
            throw new IllegalStateException("当前用户未登录或认证信息无效");
        }
        return (AuthenticatedUser) authentication.getPrincipal();
    }

    /**
     * 安全获取当前登录用户，未登录返回 empty
     */
    public static Optional<AuthenticatedUser> currentUserOptional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    /** 获取当前用户ID */
    public static Long currentUserId() {
        return currentUser().id();
    }

    /** 获取当前用户登录名 */
    public static String currentUsername() {
        return currentUser().username();
    }
}
