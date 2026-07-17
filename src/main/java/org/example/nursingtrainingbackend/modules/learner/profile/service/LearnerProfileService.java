package org.example.nursingtrainingbackend.modules.learner.profile.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.modules.learner.profile.dto.LearnerPasswordUpdateRequest;
import org.example.nursingtrainingbackend.modules.learner.profile.dto.LearnerProfileUpdateRequest;
import org.example.nursingtrainingbackend.modules.learner.profile.vo.LearnerDepartmentOptionVO;
import org.example.nursingtrainingbackend.modules.learner.profile.vo.LearnerProfileVO;
import org.example.nursingtrainingbackend.modules.user.entity.Department;
import org.example.nursingtrainingbackend.modules.user.entity.User;
import org.example.nursingtrainingbackend.modules.user.mapper.DepartmentMapper;
import org.example.nursingtrainingbackend.modules.user.mapper.UserMapper;
import org.example.nursingtrainingbackend.security.SecurityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LearnerProfileService {

    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;
    private final PasswordEncoder passwordEncoder;

    public LearnerProfileVO getProfile() {
        return toVO(getCurrentLearner());
    }

    @Transactional
    public LearnerProfileVO updateProfile(LearnerProfileUpdateRequest request) {
        User user = getCurrentLearner();
        Department department = getEnabledDepartment(request.deptId());

        user.setRealName(request.realName().trim());
        user.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
        user.setDeptId(department.getId());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        user.setDeptId(department.getId());
        return toVO(user, department);
    }

    @Transactional
    public void updatePassword(LearnerPasswordUpdateRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "两次输入的新密码不一致");
        }
        User user = getCurrentLearner();
        userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .eq(User::getId, user.getId())
                .set(User::getPassword, passwordEncoder.encode(request.newPassword()))
                .set(User::getUpdatedAt, LocalDateTime.now()));
    }

    public List<LearnerDepartmentOptionVO> getDepartments() {
        return departmentMapper.selectList(Wrappers.<Department>lambdaQuery()
                        .eq(Department::getStatus, 1)
                        .orderByAsc(Department::getId))
                .stream()
                .map(dept -> LearnerDepartmentOptionVO.builder()
                        .deptId(dept.getId())
                        .departmentName(dept.getName())
                        .build())
                .toList();
    }

    private User getCurrentLearner() {
        Long userId = SecurityUtils.currentUserId();
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(user.getRoleType())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        return user;
    }

    private Department getEnabledDepartment(Long deptId) {
        Department department = departmentMapper.selectById(deptId);
        if (department == null || !Integer.valueOf(1).equals(department.getStatus())) {
            throw new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND_OR_DISABLED);
        }
        return department;
    }

    private LearnerProfileVO toVO(User user) {
        Department department = user.getDeptId() == null ? null : departmentMapper.selectById(user.getDeptId());
        return toVO(user, department);
    }

    private LearnerProfileVO toVO(User user, Department department) {
        return LearnerProfileVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .deptId(user.getDeptId())
                .departmentName(department != null ? department.getName() : null)
                .roleType(user.getRoleType())
                .status(user.getStatus())
                .lastLoginAt(user.getLastLoginAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
