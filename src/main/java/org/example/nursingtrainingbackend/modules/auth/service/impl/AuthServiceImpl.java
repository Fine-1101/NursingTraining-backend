package org.example.nursingtrainingbackend.modules.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.modules.auth.dto.LoginRequest;
import org.example.nursingtrainingbackend.modules.auth.service.AuthService;
import org.example.nursingtrainingbackend.modules.auth.vo.LoginResponse;
import org.example.nursingtrainingbackend.modules.auth.vo.UserInfo;
import org.example.nursingtrainingbackend.modules.user.entity.User;
import org.example.nursingtrainingbackend.modules.user.mapper.UserMapper;
import org.example.nursingtrainingbackend.security.AuthenticatedUser;
import org.example.nursingtrainingbackend.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, request.username()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getUsername(), user.getRealName(), String.valueOf(user.getRoleType()));
        return new LoginResponse("Bearer", jwtService.createToken(principal), jwtService.expirationSeconds(), UserInfo.from(principal));
    }
}
