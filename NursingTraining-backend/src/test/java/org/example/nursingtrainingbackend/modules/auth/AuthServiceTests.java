package org.example.nursingtrainingbackend.modules.auth;

import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.config.properties.JwtProperties;
import org.example.nursingtrainingbackend.modules.auth.dto.LoginRequest;
import org.example.nursingtrainingbackend.modules.auth.service.impl.AuthServiceImpl;
import org.example.nursingtrainingbackend.modules.user.entity.User;
import org.example.nursingtrainingbackend.modules.user.mapper.UserMapper;
import org.example.nursingtrainingbackend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.time.Duration;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTests {
    private UserMapper mapper;
    private AuthServiceImpl service;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        mapper = mock(UserMapper.class);
        service = new AuthServiceImpl(mapper, encoder,
                new JwtService(new JwtProperties("test-key-longer-than-thirty-two-bytes-for-jwt", Duration.ofHours(1))));
    }

    @Test
    void logsInEnabledUser() {
        when(mapper.selectOne(any())).thenReturn(user(1, encoder.encode("secret")));
        var response = service.login(new LoginRequest("admin", "secret"));
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().username()).isEqualTo("admin");
    }

    @Test
    void rejectsWrongPasswordWithoutRevealingUserExistence() {
        when(mapper.selectOne(any())).thenReturn(user(1, encoder.encode("secret")));
        assertThatThrownBy(() -> service.login(new LoginRequest("admin", "wrong")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USERNAME_OR_PASSWORD_ERROR));
    }

    @Test
    void rejectsDisabledUser() {
        when(mapper.selectOne(any())).thenReturn(user(0, encoder.encode("secret")));
        assertThatThrownBy(() -> service.login(new LoginRequest("admin", "secret")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_DISABLED));
    }

    private User user(int status, String password) {
        User user = new User();
        user.setId(1L); user.setUsername("admin"); user.setRealName("管理员");
        user.setRoleType(1); user.setStatus(status); user.setPassword(password);
        return user;
    }
}
