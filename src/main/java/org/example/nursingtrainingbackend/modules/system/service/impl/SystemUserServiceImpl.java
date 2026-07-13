package org.example.nursingtrainingbackend.modules.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.modules.system.dto.SystemUserCreateDTO;
import org.example.nursingtrainingbackend.modules.system.dto.SystemUserQueryDTO;
import org.example.nursingtrainingbackend.modules.system.dto.SystemUserStatusDTO;
import org.example.nursingtrainingbackend.modules.system.dto.SystemUserUpdateDTO;
import org.example.nursingtrainingbackend.modules.system.mapper.SystemUserMapper;
import org.example.nursingtrainingbackend.modules.system.service.SystemUserService;
import org.example.nursingtrainingbackend.modules.system.vo.SystemUserItemVO;
import org.example.nursingtrainingbackend.modules.user.entity.User;
import org.example.nursingtrainingbackend.modules.user.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemUserServiceImpl implements SystemUserService {

    private final SystemUserMapper systemUserMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<SystemUserItemVO> queryPage(SystemUserQueryDTO query) {
        IPage<SystemUserItemVO> page = new Page<>(query.getPage(), query.getSize());
        IPage<SystemUserItemVO> result = systemUserMapper.selectPageUsers(
                page,
                query.getKeyword(),
                query.getStatus(),
                query.getRoleType()
        );
        return new PageResult<>(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize(), result.getPages());
    }

    @Override
    public SystemUserItemVO getDetail(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return toItemVO(user);
    }

    @Override
    @Transactional
    public SystemUserItemVO createUser(SystemUserCreateDTO dto) {
        // 检查用户名是否已存在
        Long exists = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, dto.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setDeptId(dto.getDeptId());
        user.setRoleType(dto.getRoleType());
        user.setStatus(dto.getStatus());
        userMapper.insert(user);
        return toItemVO(userMapper.selectById(user.getId()));
    }

    @Override
    @Transactional
    public SystemUserItemVO updateUser(Long id, SystemUserUpdateDTO dto) {
        User existing = userMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        existing.setRealName(dto.getRealName());
        existing.setPhone(dto.getPhone());
        existing.setDeptId(dto.getDeptId());
        existing.setRoleType(dto.getRoleType());
        userMapper.updateById(existing);
        return toItemVO(userMapper.selectById(id));
    }

    @Override
    @Transactional
    public void updateStatus(Long id, SystemUserStatusDTO dto) {
        User existing = userMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .eq(User::getId, id)
                .set(User::getStatus, dto.getStatus()));
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User existing = userMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        userMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User existing = userMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .eq(User::getId, id)
                .set(User::getPassword, passwordEncoder.encode(newPassword)));
    }

    private SystemUserItemVO toItemVO(User user) {
        SystemUserItemVO vo = new SystemUserItemVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setPhone(user.getPhone());
        vo.setRoleType(user.getRoleType());
        vo.setStatus(user.getStatus());
        vo.setLastLoginAt(user.getLastLoginAt());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());
        return vo;
    }
}
