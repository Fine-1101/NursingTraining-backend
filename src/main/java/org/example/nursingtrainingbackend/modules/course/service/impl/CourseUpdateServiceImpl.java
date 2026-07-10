package org.example.nursingtrainingbackend.modules.course.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.modules.course.dto.CompletionRuleDTO;
import org.example.nursingtrainingbackend.modules.course.dto.CreateCourseInitial;
import org.example.nursingtrainingbackend.modules.course.dto.DepartmentDTO;
import org.example.nursingtrainingbackend.modules.course.dto.UpdateCourseStatusDTO;
import org.example.nursingtrainingbackend.modules.course.entity.Course;
import org.example.nursingtrainingbackend.modules.course.entity.CourseChapter;
import org.example.nursingtrainingbackend.modules.course.entity.CourseDepartment;
import org.example.nursingtrainingbackend.modules.course.entity.CoursePoint;
import org.example.nursingtrainingbackend.modules.course.entity.CourseTag;
import org.example.nursingtrainingbackend.modules.course.mapper.CourseChapterMapper;
import org.example.nursingtrainingbackend.modules.course.mapper.CourseDepartmentMapper;
import org.example.nursingtrainingbackend.modules.course.mapper.CourseMapper;
import org.example.nursingtrainingbackend.modules.course.mapper.CoursePointMapper;
import org.example.nursingtrainingbackend.modules.course.mapper.CourseTagMapper;
import org.example.nursingtrainingbackend.modules.course.service.CourseUpdateService;
import org.example.nursingtrainingbackend.modules.course.vo.CompletionRuleVO;
import org.example.nursingtrainingbackend.modules.course.vo.CourseUpdateBasicVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
public class CourseUpdateServiceImpl implements CourseUpdateService {

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private CourseDepartmentMapper courseDepartmentMapper;

    @Autowired
    private CourseTagMapper courseTagMapper;

    @Autowired
    private CourseChapterMapper courseChapterMapper;

    @Autowired
    private CoursePointMapper coursePointMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseUpdateBasicVO updateCourseBasic(Long courseId, CreateCourseInitial dto) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
        if (!Integer.valueOf(0).equals(course.getStatus())) {
            throw new BusinessException(ErrorCode.COURSE_STATUS_INVALID);
        }

        course.setTitle(dto.getTitle().trim());
        course.setSummary(dto.getSummary().trim());
        course.setLearningObjective(dto.getLearningObjective().trim());
        course.setCategoryId(dto.getCategoryId());
        course.setCoverUrl(dto.getCoverUrl());

        if (dto.getStartAt() != null && !dto.getStartAt().isBlank()) {
            try {
                OffsetDateTime odt = OffsetDateTime.parse(dto.getStartAt());
                course.setStartAt(odt.toLocalDateTime());
            } catch (Exception e1) {
                try {
                    course.setStartAt(LocalDateTime.parse(dto.getStartAt()));
                } catch (Exception e2) {
                    log.warn("无法解析startAt格式: {}", dto.getStartAt());
                }
            }
        } else {
            course.setStartAt(null);
        }

        course.setUpdatedAt(LocalDateTime.now());
        courseMapper.updateById(course);

        courseTagMapper.delete(Wrappers.<CourseTag>lambdaQuery()
                .eq(CourseTag::getCourseId, courseId));
        if (dto.getTagIds() != null) {
            for (Long tagId : dto.getTagIds()) {
                CourseTag courseTag = new CourseTag();
                courseTag.setCourseId(courseId);
                courseTag.setTagId(tagId);
                courseTagMapper.insert(courseTag);
            }
        }

        courseDepartmentMapper.delete(Wrappers.<CourseDepartment>lambdaQuery()
                .eq(CourseDepartment::getCourseId, courseId));
        for (DepartmentDTO dept : dto.getDepartments()) {
            CourseDepartment courseDepartment = new CourseDepartment();
            courseDepartment.setCourseId(courseId);
            courseDepartment.setDepartmentId(dept.getDepartmentId());
            courseDepartment.setRequired(Boolean.TRUE.equals(dept.getRequired()) ? 1 : 0);
            courseDepartmentMapper.insert(courseDepartment);
        }

        CourseUpdateBasicVO vo = new CourseUpdateBasicVO();
        vo.setCourseId(courseId);
        vo.setStatus("DRAFT");
        vo.setCurrentStep(2);
        vo.setUpdatedAt(course.getUpdatedAt());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompletionRuleVO saveCompletionRule(Long courseId, CompletionRuleDTO dto) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }

        // Store completion rule (1 = ALL_REQUIRED_POINTS)
        course.setCompletionRule(1);
        course.setUpdatedAt(LocalDateTime.now());
        courseMapper.updateById(course);

        return buildCompletionRuleVO(courseId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCourseStatus(Long courseId, UpdateCourseStatusDTO dto) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }

        if ("PUBLISHED".equals(dto.getStatus())) {
            course.setStatus(1);
        } else {
            course.setStatus(0);
        }
        course.setUpdatedAt(LocalDateTime.now());
        if (course.getStatus() == 1 && course.getPublishedAt() == null) {
            course.setPublishedAt(LocalDateTime.now());
        }
        courseMapper.updateById(course);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCourseDraft(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
        // Delete associated data
        coursePointMapper.delete(Wrappers.<CoursePoint>lambdaQuery().eq(CoursePoint::getCourseId, courseId));
        courseChapterMapper.delete(Wrappers.<CourseChapter>lambdaQuery().eq(CourseChapter::getCourseId, courseId));
        courseDepartmentMapper.delete(Wrappers.<CourseDepartment>lambdaQuery().eq(CourseDepartment::getCourseId, courseId));
        courseTagMapper.delete(Wrappers.<CourseTag>lambdaQuery().eq(CourseTag::getCourseId, courseId));
        // Delete course (soft delete via @TableLogic)
        courseMapper.deleteById(courseId);
    }

    private CompletionRuleVO buildCompletionRuleVO(Long courseId) {
        List<CoursePoint> points = coursePointMapper.selectList(
                Wrappers.<CoursePoint>lambdaQuery().eq(CoursePoint::getCourseId, courseId));

        int requiredCount = 0;
        int optionalCount = 0;
        for (CoursePoint p : points) {
            if (p.getRequired() != null && p.getRequired() == 1) {
                requiredCount++;
            } else {
                optionalCount++;
            }
        }

        // Check structure validity: at least one chapter with at least one point
        Long chapterCount = courseChapterMapper.selectCount(
                Wrappers.<CourseChapter>lambdaQuery().eq(CourseChapter::getCourseId, courseId));
        boolean structureValid = chapterCount > 0 && !points.isEmpty();

        CompletionRuleVO vo = new CompletionRuleVO();
        vo.setCompletionRule("ALL_REQUIRED_POINTS");
        vo.setRequiredPointCount(requiredCount);
        vo.setOptionalPointCount(optionalCount);
        vo.setStructureValid(structureValid);
        return vo;
    }
}
