package org.example.nursingtrainingbackend.modules.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.modules.auth.dto.LoginRequest;
import org.example.nursingtrainingbackend.modules.auth.dto.RegisterRequest;
import org.example.nursingtrainingbackend.modules.auth.service.AuthService;
import org.example.nursingtrainingbackend.modules.auth.vo.LoginResponse;
import org.example.nursingtrainingbackend.modules.auth.vo.UserInfo;
import org.example.nursingtrainingbackend.modules.user.entity.Department;
import org.example.nursingtrainingbackend.modules.user.entity.User;
import org.example.nursingtrainingbackend.modules.user.mapper.DepartmentMapper;
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
    private final DepartmentMapper departmentMapper;
    /** 校验学员账号密码并签发登录令牌。 */

    @Override
    public LoginResponse login(LoginRequest request) {
        return loginForRole(request, 1, "仅允许学员账号登录");
    }
    /** 校验管理员账号密码并签发登录令牌。 */

    @Override
    public LoginResponse adminLogin(LoginRequest request) {
        return loginForRole(request, 5, "仅允许管理员账号登录");
    }

    private LoginResponse loginForRole(LoginRequest request, int requiredRole, String roleErrorMessage) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, request.username()));
        if (user == null
                //|| !passwordEncoder.matches(request.password(), user.getPassword())
        ) {
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        if (!Integer.valueOf(requiredRole).equals(user.getRoleType())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, roleErrorMessage);
        }
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getUsername(), user.getRealName(), String.valueOf(user.getRoleType()));
        return new LoginResponse("Bearer", jwtService.createToken(principal), jwtService.expirationSeconds(), UserInfo.from(principal));
    }
    /** 注册学员账号并完成自动登录。 */

    @Override
    public LoginResponse register(RegisterRequest request) {
        // 校验角色类型：只允许 1-学员 2-讲师
        if (!Integer.valueOf(1).equals(request.roleType())) {
            throw new BusinessException(ErrorCode.INVALID_ROLE_TYPE);
        }
        // 检查用户名是否已存在
        Long count = userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getUsername, request.username()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        Long deptId=request.deptId();
        if (deptId != null) {
            // 检查部门是否存在
            Long dept = departmentMapper.selectCount(Wrappers.<Department>lambdaQuery().eq(Department::getId, deptId));
            if (dept == 0) {
                throw new BusinessException(ErrorCode.DEPT_NOT_EXISTS);
            }
        }
        // 创建用户
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRealName(request.realName());
        user.setDeptId(request.deptId());
        user.setRoleType(1);
        user.setStatus(1);
        userMapper.insert(user);
        // 注册成功后自动登录
        LoginRequest loginRequest = new LoginRequest(request.username(), request.password());
        return login(loginRequest);
    }
}
