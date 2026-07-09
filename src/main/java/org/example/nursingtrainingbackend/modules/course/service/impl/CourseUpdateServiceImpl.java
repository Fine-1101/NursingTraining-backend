package org.example.nursingtrainingbackend.modules.course.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.modules.course.dto.CreateCourseInitial;
import org.example.nursingtrainingbackend.modules.course.dto.DepartmentDTO;
import org.example.nursingtrainingbackend.modules.course.entity.Course;
import org.example.nursingtrainingbackend.modules.course.entity.CourseDepartment;
import org.example.nursingtrainingbackend.modules.course.entity.CourseTag;
import org.example.nursingtrainingbackend.modules.course.mapper.CourseDepartmentMapper;
import org.example.nursingtrainingbackend.modules.course.mapper.CourseMapper;
import org.example.nursingtrainingbackend.modules.course.mapper.CourseTagMapper;
import org.example.nursingtrainingbackend.modules.course.service.CourseUpdateService;
import org.example.nursingtrainingbackend.modules.course.vo.CourseUpdateBasicVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CourseUpdateServiceImpl implements CourseUpdateService {

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private CourseDepartmentMapper courseDepartmentMapper;

    @Autowired
    private CourseTagMapper courseTagMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseUpdateBasicVO updateCourseBasic(Long courseId, CreateCourseInitial dto) {
        // 锁定课程行并校验存在
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
        // 仅草稿状态可编辑
        if (!Integer.valueOf(0).equals(course.getStatus())) {
            throw new BusinessException(ErrorCode.COURSE_STATUS_INVALID);
        }

        // 更新课程主表
        course.setTitle(dto.getTitle().trim());
        course.setSummary(dto.getSummary().trim());
        course.setLearningObjective(dto.getLearningObjective().trim());
        course.setCategoryId(dto.getCategoryId());
        course.setCoverUrl(dto.getCoverUrl());
        if (dto.getStartAt() != null && !dto.getStartAt().isBlank()) {
            course.setStartAt(LocalDateTime.parse(dto.getStartAt()));
        } else {
            course.setStartAt(null);
        }
        course.setUpdatedAt(LocalDateTime.now());
        courseMapper.updateById(course);

        // 完整替换标签关系：先删后插
        courseTagMapper.delete(Wrappers.<CourseTag>lambdaQuery()
                .eq(CourseTag::getCourseId, courseId));
        if (dto.getTagIds() != null) {
            for (Long tagId : dto.getTagIds()) {
                CourseTag courseTag = new CourseTag();
                courseTag.setCourseId(courseId);
                courseTag.setTagId(tagId);
                courseTag.setCreatedAt(LocalDateTime.now());
                courseTagMapper.insert(courseTag);
            }
        }

        // 完整替换部门关系：先删后插
        courseDepartmentMapper.delete(Wrappers.<CourseDepartment>lambdaQuery()
                .eq(CourseDepartment::getCourseId, courseId));
        for (DepartmentDTO dept : dto.getDepartments()) {
            CourseDepartment courseDepartment = new CourseDepartment();
            courseDepartment.setCourseId(courseId);
            courseDepartment.setDepartmentId(dept.getId());
            courseDepartment.setRequired(Boolean.TRUE.equals(dept.getRequired()) ? 1 : 0);
            courseDepartment.setCreatedAt(LocalDateTime.now());
            courseDepartmentMapper.insert(courseDepartment);
        }

        // 构造响应
        CourseUpdateBasicVO vo = new CourseUpdateBasicVO();
        vo.setCourseId(courseId);
        vo.setStatus("DRAFT");
        vo.setCurrentStep(2);
        vo.setUpdatedAt(course.getUpdatedAt());
        return vo;
    }

    @Override
    public Void NewCourse(Long courseId) {
        return null;
    }
}