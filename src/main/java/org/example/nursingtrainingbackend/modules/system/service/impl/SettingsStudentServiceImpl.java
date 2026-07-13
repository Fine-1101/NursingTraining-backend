package org.example.nursingtrainingbackend.modules.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.modules.learning.entity.UserCourseProgress;
import org.example.nursingtrainingbackend.modules.learning.mapper.UserCourseProgressMapper;
import org.example.nursingtrainingbackend.modules.system.dto.StudentQueryDTO;
import org.example.nursingtrainingbackend.modules.system.dto.StudentUpdateDTO;
//import org.example.nursingtrainingbackend.modules.system.entity.UserCourseProgress;
import org.example.nursingtrainingbackend.modules.system.mapper.SettingsStudentMapper;
//import org.example.nursingtrainingbackend.modules.system.mapper.UserCourseProgressMapper;
import org.example.nursingtrainingbackend.modules.system.service.SettingsStudentService;
import org.example.nursingtrainingbackend.modules.system.vo.*;
import org.example.nursingtrainingbackend.modules.user.entity.User;
import org.example.nursingtrainingbackend.modules.user.mapper.UserMapper;
import org.example.nursingtrainingbackend.security.SecurityUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsStudentServiceImpl implements SettingsStudentService {

    private static final String DIST_CACHE_PREFIX = "nursing:admin:settings:student_department_distribution:v1:";
    private static final long CACHE_TTL_MINUTES = 5;

    private final SettingsStudentMapper settingsStudentMapper;
    private final UserCourseProgressMapper userCourseProgressMapper;
    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;
    @Override
    public CurrentUserVO getCurrentUser() {
        Long currentUserId = SecurityUtils.currentUserId();
        User admin = userMapper.selectById(currentUserId);
        if (admin == null || admin.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.ADMIN_NOT_FOUND);
        }
        if (admin.getStatus() != null && admin.getStatus() == 0) {
            throw new BusinessException(ErrorCode.ADMIN_NOT_FOUND);
        }

        CurrentUserVO vo = new CurrentUserVO();
        vo.setUserId(admin.getId());
        vo.setAvatarUrl(admin.getAvatarUrl());
        vo.setUsername(admin.getUsername());
        vo.setRealName(admin.getRealName());
        vo.setPhone(admin.getPhone());
        vo.setDepartmentId(admin.getDeptId());
        vo.setRoleType("ADMIN");
        vo.setStatus(admin.getStatus() != null && admin.getStatus() == 1 ? "ENABLED" : "DISABLED");
        vo.setCreatedAt(admin.getCreatedAt());
        vo.setLastLoginAt(admin.getLastLoginAt());

        if (admin.getDeptId() != null) {
            String deptName = settingsStudentMapper.selectDepartmentNameById(admin.getDeptId());
            vo.setDepartmentName(deptName);
        }
        return vo;
    }

    @Override
    public PageResult<StudentListItemVO> queryStudents(StudentQueryDTO query) {
        var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<StudentListItemVO>(
                query.getPage(), query.getSize());
        var result = settingsStudentMapper.selectStudentPage(page, query.getKeyword(), query.getDepartmentId());
        // 后处理：手机号脱敏 + 状态转换
        for (StudentListItemVO item : result.getRecords()) {
            item.setMaskedPhone(maskPhone(item.getMaskedPhone()));
            item.setStatus("ENABLED".equals(item.getStatus()) || "1".equals(item.getStatus())
                    ? "ENABLED" : "DISABLED");
        }
        return new PageResult<>(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize(), result.getPages());
    }

    @Override
    public StudentDetailVO getStudentDetail(Long studentId) {
        User student = getStudentOrThrow(studentId);
        return buildStudentDetail(student);
    }

    @Override
    @Transactional
    public StudentDetailVO updateStudent(Long studentId, StudentUpdateDTO dto) {
        User student = getStudentOrThrow(studentId);

        // 检查工号唯一
        Long sameUsername = userMapper.selectCount(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getUsername, dto.getUsername())
                        .ne(User::getId, studentId));
        if (sameUsername != null && sameUsername > 0) {
            throw new BusinessException(ErrorCode.STUDENT_USERNAME_EXISTS);
        }

        // 检查科室
        if (dto.getDepartmentId() != null) {
            Integer deptStatus = settingsStudentMapper.selectDepartmentStatusById(dto.getDepartmentId());
            if (deptStatus == null || deptStatus != 1) {
                throw new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND_OR_DISABLED);
            }
        }

        student.setRealName(dto.getRealName());
        student.setUsername(dto.getUsername());
        student.setPhone(dto.getPhone());
        student.setAvatarUrl(dto.getAvatarUrl());
        student.setAvatarObjectKey(dto.getAvatarObjectKey());
        student.setDeptId(dto.getDepartmentId());
        student.setStatus("ENABLED".equals(dto.getStatus()) ? 1 : 0);
        userMapper.updateById(student);

        // 清除科室分布缓存
        clearDistributionCache();

        return buildStudentDetail(userMapper.selectById(studentId));
    }

    @Override
    @Transactional
    public StudentDeleteVO deleteStudent(Long studentId) {
        User student = getStudentOrThrow(studentId);
        // 软删除：设置 deleted_at
        LocalDateTime now = LocalDateTime.now();
        student.setDeletedAt(now);
        userMapper.updateById(student);
        clearDistributionCache();

        StudentDeleteVO vo = new StudentDeleteVO();
        vo.setStudentId(studentId);
        vo.setDeleted(true);
        vo.setDeletedAt(now);
        return vo;
    }

    @Override
    public DepartmentDistributionVO getDepartmentDistribution(boolean activeOnly) {
        String cacheKey = DIST_CACHE_PREFIX + activeOnly;
        // 尝试从 Redis 获取缓存
        String cachedJson = null;
        try {
            cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null && !cachedJson.isBlank()) {
                return objectMapper.readValue(cachedJson, DepartmentDistributionVO.class);
            }
        } catch (Exception e) {
            log.warn("读取科室分布缓存失败, key={}", cacheKey, e);
        }

        List<DepartmentDistributionItemVO> rawItems =
                settingsStudentMapper.selectDepartmentDistribution(activeOnly);

        int total = rawItems.stream().mapToInt(DepartmentDistributionItemVO::getStudentCount).sum();
        for (DepartmentDistributionItemVO item : rawItems) {
            if (total > 0) {
                BigDecimal pct = BigDecimal.valueOf(item.getStudentCount())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
                item.setPercent(pct);
            } else {
                item.setPercent(BigDecimal.ZERO);
            }
            if (item.getDepartmentName() == null || item.getDepartmentName().isBlank()) {
                item.setDepartmentName("未分配科室");
            }
        }

        DepartmentDistributionVO vo = new DepartmentDistributionVO();
        vo.setTotal(total);
        vo.setItems(rawItems);
        // 将结果写入 Redis 缓存
        try {
            String json = objectMapper.writeValueAsString(vo);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入科室分布缓存失败, key={}", cacheKey, e);
        }
        return vo;
    }

    @Override
    public CourseProgressVO getCourseProgress(Long studentId) {
        User student = getStudentOrThrow(studentId);

        // 学员无科室时返回空列表
        if (student.getDeptId() == null) {
            CourseProgressVO vo = new CourseProgressVO();
            vo.setStudentId(studentId);
            vo.setRealName(student.getRealName());
            vo.setAverageProgressPercent(BigDecimal.ZERO);
            vo.setCourseCount(0);
            vo.setItems(new ArrayList<>());
            return vo;
        }

        // 查询该学员可学习的已发布课程
        List<CourseProgressItemVO> items = settingsStudentMapper.selectStudentCourseProgress(
                studentId, student.getDeptId());

        BigDecimal totalProgress = BigDecimal.ZERO;
        for (CourseProgressItemVO item : items) {
            totalProgress = totalProgress.add(item.getProgressPercent());
        }
        BigDecimal avgProgress = items.isEmpty()
                ? BigDecimal.ZERO
                : totalProgress.divide(BigDecimal.valueOf(items.size()), 2, RoundingMode.HALF_UP);

        CourseProgressVO vo = new CourseProgressVO();
        vo.setStudentId(studentId);
        vo.setRealName(student.getRealName());
        vo.setAverageProgressPercent(avgProgress);
        vo.setCourseCount(items.size());
        vo.setItems(items);
        return vo;
    }

    @Override
    @Transactional
    public ProgressCompleteVO completeProgress(Long studentId, Long courseId) {
        User student = getStudentOrThrow(studentId);
        validateCourseForStudent(courseId, student.getDeptId());

        LocalDateTime now = LocalDateTime.now();
        UserCourseProgress progress = userCourseProgressMapper.selectOne(
                Wrappers.<UserCourseProgress>lambdaQuery()
                        .eq(UserCourseProgress::getUserId, studentId)
                        .eq(UserCourseProgress::getCourseId, courseId));

        if (progress == null) {
            progress = new UserCourseProgress();
            progress.setUserId(studentId);
            progress.setCourseId(courseId);
            progress.setProgressPercent(new BigDecimal("100.00"));
            progress.setStatus(2);
            progress.setStartedAt(now);
            progress.setCompletedAt(now);
            userCourseProgressMapper.insert(progress);
        } else {
            progress.setProgressPercent(new BigDecimal("100.00"));
            progress.setStatus(2);
            progress.setCompletedAt(now);
            if (progress.getStartedAt() == null) {
                progress.setStartedAt(now);
            }
            userCourseProgressMapper.updateById(progress);
        }

        ProgressCompleteVO vo = new ProgressCompleteVO();
        vo.setStudentId(studentId);
        vo.setCourseId(courseId);
        vo.setLearningStatus("COMPLETED");
        vo.setProgressPercent(new BigDecimal("100.00"));
        vo.setCompletedAt(now);
        vo.setUpdatedAt(now);
        return vo;
    }

    @Override
    @Transactional
    public ProgressResetVO resetProgress(Long studentId, Long courseId) {
        User student = getStudentOrThrow(studentId);
        validateCourseForStudent(courseId, student.getDeptId());

        LocalDateTime now = LocalDateTime.now();
        UserCourseProgress progress = userCourseProgressMapper.selectOne(
                Wrappers.<UserCourseProgress>lambdaQuery()
                        .eq(UserCourseProgress::getUserId, studentId)
                        .eq(UserCourseProgress::getCourseId, courseId));

        if (progress == null) {
            progress = new UserCourseProgress();
            progress.setUserId(studentId);
            progress.setCourseId(courseId);
            progress.setProgressPercent(BigDecimal.ZERO);
            progress.setStatus(0);
            progress.setStartedAt(null);
            progress.setCompletedAt(null);
            progress.setLastPointId(null);
            userCourseProgressMapper.insert(progress);
        } else {
            progress.setProgressPercent(BigDecimal.ZERO);
            progress.setStatus(0);
            progress.setStartedAt(null);
            progress.setCompletedAt(null);
            progress.setLastPointId(null);
            userCourseProgressMapper.updateById(progress);
        }

        ProgressResetVO vo = new ProgressResetVO();
        vo.setStudentId(studentId);
        vo.setCourseId(courseId);
        vo.setLearningStatus("NOT_STARTED");
        vo.setProgressPercent(BigDecimal.ZERO);
        vo.setLastPointId(null);
        vo.setCompletedAt(null);
        vo.setUpdatedAt(now);
        return vo;
    }

    @Override
    public List<DepartmentOptionVO> getDepartmentOptions() {
        return settingsStudentMapper.selectDepartmentOptions();
    }

    // ==================== 私有方法 ====================

    private User getStudentOrThrow(Long studentId) {
        if (studentId == null || studentId <= 0) {
            throw new BusinessException(ErrorCode.STUDENT_ID_INVALID);
        }
        User student = userMapper.selectById(studentId);
        if (student == null || student.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.STUDENT_NOT_FOUND);
        }
        if (student.getRoleType() == null || student.getRoleType() != 1) {
            throw new BusinessException(ErrorCode.NOT_STUDENT_ROLE);
        }
        return student;
    }

    private StudentDetailVO buildStudentDetail(User student) {
        StudentDetailVO vo = new StudentDetailVO();
        vo.setStudentId(student.getId());
        vo.setAvatarUrl(student.getAvatarUrl());
        vo.setRealName(student.getRealName());
        vo.setUsername(student.getUsername());
        vo.setPhone(student.getPhone());
        vo.setDepartmentId(student.getDeptId());
        vo.setRoleType("STUDENT");
        vo.setStatus(student.getStatus() != null && student.getStatus() == 1 ? "ENABLED" : "DISABLED");
        vo.setCreatedAt(student.getCreatedAt());
        vo.setLastLoginAt(student.getLastLoginAt());

        if (student.getDeptId() != null) {
            vo.setDepartmentName(settingsStudentMapper.selectDepartmentNameById(student.getDeptId()));
        }

        // 计算课程数和平均进度
        List<CourseProgressItemVO> items = student.getDeptId() != null
                ? settingsStudentMapper.selectStudentCourseProgress(student.getId(), student.getDeptId())
                : new ArrayList<>();
        vo.setCourseCount(items.size());
        if (items.isEmpty()) {
            vo.setAverageProgressPercent(BigDecimal.ZERO);
        } else {
            BigDecimal total = items.stream()
                    .map(CourseProgressItemVO::getProgressPercent)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            vo.setAverageProgressPercent(total.divide(BigDecimal.valueOf(items.size()), 2, RoundingMode.HALF_UP));
        }
        return vo;
    }

    private void validateCourseForStudent(Long courseId, Long deptId) {
        Integer courseStatus = settingsStudentMapper.selectPublishedCourseInDept(courseId, deptId);
        if (courseStatus == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_AVAILABLE_FOR_STUDENT);
        }
    }

    private void clearDistributionCache() {
        try {
            redisTemplate.delete(DIST_CACHE_PREFIX + "true");
            redisTemplate.delete(DIST_CACHE_PREFIX + "false");
        } catch (Exception e) {
            log.warn("Failed to clear distribution cache", e);
        }
    }

    /**
     * 手机号脱敏：13812345678 -> 138****5678
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
