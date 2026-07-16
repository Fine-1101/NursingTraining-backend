package org.example.nursingtrainingbackend.modules.system.service.impl;

import org.example.nursingtrainingbackend.modules.learning.mapper.UserCourseProgressMapper;
import org.example.nursingtrainingbackend.modules.dashboard.service.DashboardService;
import org.example.nursingtrainingbackend.modules.system.mapper.SettingsStudentMapper;
import org.example.nursingtrainingbackend.modules.system.vo.CourseProgressItemVO;
import org.example.nursingtrainingbackend.modules.system.vo.StudentDetailVO;
import org.example.nursingtrainingbackend.modules.user.entity.User;
import org.example.nursingtrainingbackend.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SettingsStudentServiceImplTests {

    @Test
    void studentDetailIncludesNotStartedCoursesInAverageProgress() {
        SettingsStudentMapper settingsStudentMapper = mock(SettingsStudentMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        SettingsStudentServiceImpl service = new SettingsStudentServiceImpl(
                settingsStudentMapper,
                mock(UserCourseProgressMapper.class),
                userMapper,
                mock(StringRedisTemplate.class),
                mock(ObjectMapper.class),
                mock(DashboardService.class));

        User student = new User();
        student.setId(7L);
        student.setRoleType(1);
        student.setDeptId(3L);
        when(userMapper.selectById(7L)).thenReturn(student);
        when(settingsStudentMapper.selectStudentCourseProgress(7L, 3L)).thenReturn(List.of(
                courseProgress(new BigDecimal("100.00")),
                courseProgress(BigDecimal.ZERO)));

        StudentDetailVO result = service.getStudentDetail(7L);

        assertEquals(2, result.getCourseCount());
        assertEquals(new BigDecimal("50.00"), result.getAverageProgressPercent());
    }

    @Test
    void completingProgressEvictsDashboardCache() {
        SettingsStudentMapper settingsStudentMapper = mock(SettingsStudentMapper.class);
        UserCourseProgressMapper progressMapper = mock(UserCourseProgressMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        DashboardService dashboardService = mock(DashboardService.class);
        SettingsStudentServiceImpl service = new SettingsStudentServiceImpl(
                settingsStudentMapper, progressMapper, userMapper,
                mock(StringRedisTemplate.class), mock(ObjectMapper.class), dashboardService);

        User student = student(7L, 3L);
        when(userMapper.selectById(7L)).thenReturn(student);
        when(settingsStudentMapper.selectPublishedCourseInDept(11L, 3L)).thenReturn(1);
        when(progressMapper.selectOne(any())).thenReturn(null);

        service.completeProgress(7L, 11L);

        verify(dashboardService).evictDashboardCache();
    }

    @Test
    void resettingProgressEvictsDashboardCache() {
        SettingsStudentMapper settingsStudentMapper = mock(SettingsStudentMapper.class);
        UserCourseProgressMapper progressMapper = mock(UserCourseProgressMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        DashboardService dashboardService = mock(DashboardService.class);
        SettingsStudentServiceImpl service = new SettingsStudentServiceImpl(
                settingsStudentMapper, progressMapper, userMapper,
                mock(StringRedisTemplate.class), mock(ObjectMapper.class), dashboardService);

        when(userMapper.selectById(7L)).thenReturn(student(7L, 3L));
        when(settingsStudentMapper.selectPublishedCourseInDept(11L, 3L)).thenReturn(1);
        when(progressMapper.selectOne(any())).thenReturn(new org.example.nursingtrainingbackend.modules.learning.entity.UserCourseProgress());

        service.resetProgress(7L, 11L);

        verify(dashboardService).evictDashboardCache();
    }

    private static User student(long id, long departmentId) {
        User student = new User();
        student.setId(id);
        student.setRoleType(1);
        student.setDeptId(departmentId);
        return student;
    }

    private static CourseProgressItemVO courseProgress(BigDecimal progress) {
        CourseProgressItemVO item = new CourseProgressItemVO();
        item.setProgressPercent(progress);
        return item;
    }
}
