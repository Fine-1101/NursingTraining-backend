package org.example.nursingtrainingbackend.modules.course.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.example.nursingtrainingbackend.common.event.CacheEvictionEvent;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.modules.category.entity.Category;
import org.example.nursingtrainingbackend.modules.category.mapper.CategoryMapper;
import org.example.nursingtrainingbackend.modules.course.dto.CourseStatusDTO;
import org.example.nursingtrainingbackend.modules.course.dto.CreateCourseInitial;
import org.example.nursingtrainingbackend.modules.course.dto.DepartmentDTO;
import org.example.nursingtrainingbackend.modules.course.entity.*;
import org.example.nursingtrainingbackend.modules.course.mapper.*;
import org.example.nursingtrainingbackend.modules.course.service.CourseUpdateService;
import org.example.nursingtrainingbackend.modules.course.vo.CourseStatusVO;
import org.example.nursingtrainingbackend.modules.course.vo.CourseUpdateBasicVO;
import org.example.nursingtrainingbackend.modules.courseware.article.entity.Article;
import org.example.nursingtrainingbackend.modules.courseware.article.mapper.ArticleMapper;
import org.example.nursingtrainingbackend.modules.courseware.ppt.entity.Ppt;
import org.example.nursingtrainingbackend.modules.courseware.ppt.mapper.PptMapper;
import org.example.nursingtrainingbackend.modules.courseware.video.entity.Video;
import org.example.nursingtrainingbackend.modules.courseware.video.mapper.VideoMapper;
import org.example.nursingtrainingbackend.modules.tag.entity.Tag;
import org.example.nursingtrainingbackend.modules.tag.mapper.TagMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

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
    @Autowired
    private CoursePointArticleMapper coursePointArticleMapper;
    @Autowired
    private CoursePointVideoMapper coursePointVideoMapper;
    @Autowired
    private CoursePointPptMapper coursePointPptMapper;
    @Autowired
    private TagMapper tagMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private PptMapper pptMapper;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

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
        course.setInstructorId(dto.getInstructorId());
        if (dto.getStartAt() != null && !dto.getStartAt().isBlank()) {
            course.setStartAt(OffsetDateTime.parse(dto.getStartAt()).toLocalDateTime());
        } else {
            course.setStartAt(null);
        }
        course.setUpdatedAt(LocalDateTime.now());
        courseMapper.updateById(course);

        // 课程基础信息变更，清除课程结构缓存
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.COURSE_STUDY));

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
            courseDepartment.setDepartmentId(dept.getDepartmentId());
            courseDepartment.setRequired(Boolean.TRUE.equals(dept.getRequired()) ? 1 : 0);
            courseDepartment.setCreatedAt(LocalDateTime.now());
            courseDepartmentMapper.insert(courseDepartment);
        }
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.DEPARTMENT_VISIBLE_COURSES));
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.LEARNER_HOME));

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseStatusVO updateCourseStatus(Long courseId, CourseStatusDTO dto) {
        // 1. 校验课程存在
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }

        String targetStatus = dto.getStatus();
        Integer currentStatus = course.getStatus();
        LocalDateTime now = LocalDateTime.now();

        // 2. 解析目标状态
        Integer targetCode = parseStatus(targetStatus);
        if (targetCode == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        // 3. 重复状态检查
        if (targetCode.equals(currentStatus)) {
            throw new BusinessException(ErrorCode.COURSE_STATUS_INVALID);
        }

        // 4. 状态流转校验与执行
        switch (targetStatus) {
            case "PUBLISHED" -> {
                // DRAFT→PUBLISHED 或 OFFLINE→PUBLISHED：必须完整校验
                if (currentStatus != 0 && currentStatus != 2) {
                    throw new BusinessException(ErrorCode.COURSE_STATUS_INVALID);
                }
                validateForPublish(course);
                course.setStatus(1);
                course.setPublishedAt(now);
            }
            case "OFFLINE" -> {
                // PUBLISHED→OFFLINE
                if (currentStatus != 1) {
                    throw new BusinessException(ErrorCode.COURSE_STATUS_INVALID);
                }
                course.setStatus(2);
            }
            case "DRAFT" -> {
                // PUBLISHED→DRAFT（撤回草稿）
                if (currentStatus != 1) {
                    throw new BusinessException(ErrorCode.COURSE_STATUS_INVALID);
                }
                course.setStatus(0);
                course.setPublishedAt(null);
            }
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        course.setUpdatedAt(now);
        courseMapper.updateById(course);

        // 课程状态变更，清除课程结构缓存
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.COURSE_STUDY));
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.DEPARTMENT_VISIBLE_COURSES));
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.LEARNER_HOME));

        // 5. 构造响应
        CourseStatusVO vo = new CourseStatusVO();
        vo.setCourseId(courseId);
        vo.setStatus(targetStatus);
        vo.setPublishedAt(course.getPublishedAt());
        vo.setUpdatedAt(now);
        return vo;
    }

    // ---------- 发布前完整校验 ----------

    private void validateForPublish(Course course) {
        Long courseId = course.getId();
        List<String> errors = new ArrayList<>();

        // 1. 基础信息完整
        if (isBlank(course.getTitle())) errors.add("课程标题不能为空");
        if (isBlank(course.getSummary())) errors.add("课程简介不能为空");
        if (isBlank(course.getCoverUrl())) errors.add("封面地址不能为空");

        // 2. 类别有效
        if (course.getCategoryId() == null) {
            errors.add("课程分类不能为空");
        } else {
            Category category = categoryMapper.selectById(course.getCategoryId());
            if (category == null || !Integer.valueOf(1).equals(category.getStatus())) {
                errors.add("课程分类不存在或已停用");
            }
        }

        // 4. 标签 0~3，全部启用且未删除
        List<CourseTag> courseTags = courseTagMapper.selectList(
                Wrappers.<CourseTag>lambdaQuery().eq(CourseTag::getCourseId, courseId));
        if (courseTags.size() > 3) {
            errors.add("标签最多选择3个");
        }
        for (CourseTag ct : courseTags) {
            Tag tag = tagMapper.selectById(ct.getTagId());
            if (tag == null || !Integer.valueOf(1).equals(tag.getStatus())) {
                errors.add("标签不存在、已停用或已删除");
                break;
            }
        }

        // 5. 至少一个启用部门
        List<CourseDepartment> courseDepts = courseDepartmentMapper.selectList(
                Wrappers.<CourseDepartment>lambdaQuery().eq(CourseDepartment::getCourseId, courseId));
        if (courseDepts.isEmpty()) {
            errors.add("课程至少需要配置一个发布部门");
        }

        // 6. 至少一个启用、未删除章节
        Long chapterCount = courseChapterMapper.selectCount(
                Wrappers.<CourseChapter>lambdaQuery()
                        .eq(CourseChapter::getCourseId, courseId)
                        .eq(CourseChapter::getStatus, 1));
        if (chapterCount == 0) {
            errors.add("课程至少需要包含一个启用章节");
        }

        // 7. 至少一个启用、未删除的必修课程点
        List<CoursePoint> allPoints = coursePointMapper.selectList(
                Wrappers.<CoursePoint>lambdaQuery()
                        .eq(CoursePoint::getCourseId, courseId)
                        .eq(CoursePoint::getStatus, 1));
        List<CoursePoint> requiredPoints = allPoints.stream()
                .filter(p -> p.getRequired() != null && p.getRequired() == 1)
                .toList();
        if (requiredPoints.isEmpty()) {
            errors.add("课程至少需要包含一个启用且必修的课程点");
        }

        // 8. 每个启用课程点至少关联一个已发布、未删除的课件
        for (CoursePoint point : allPoints) {
            boolean hasMedia = false;

            List<CoursePointArticle> cpas = coursePointArticleMapper.selectList(
                    Wrappers.<CoursePointArticle>lambdaQuery()
                            .eq(CoursePointArticle::getCoursePointId, point.getId()));
            for (CoursePointArticle cpa : cpas) {
                Article a = articleMapper.selectById(cpa.getArticleId());
                if (a != null && Integer.valueOf(1).equals(a.getStatus())) {
                    hasMedia = true;
                    break;
                }
            }

            if (!hasMedia) {
                List<CoursePointVideo> cpvs = coursePointVideoMapper.selectList(
                        Wrappers.<CoursePointVideo>lambdaQuery()
                                .eq(CoursePointVideo::getCoursePointId, point.getId()));
                for (CoursePointVideo cpv : cpvs) {
                    Video v = videoMapper.selectById(cpv.getVideoId());
                    if (v != null && Integer.valueOf(1).equals(v.getStatus())) {
                        hasMedia = true;
                        break;
                    }
                }
            }

            if (!hasMedia) {
                List<CoursePointPpt> cpps = coursePointPptMapper.selectList(
                        Wrappers.<CoursePointPpt>lambdaQuery()
                                .eq(CoursePointPpt::getCoursePointId, point.getId()));
                for (CoursePointPpt cpp : cpps) {
                    Ppt p = pptMapper.selectById(cpp.getPptId());
                    if (p != null && Integer.valueOf(1).equals(p.getStatus())) {
                        hasMedia = true;
                        break;
                    }
                }
            }

            if (!hasMedia) {
                errors.add("课程点[" + point.getTitle() + "]至少关联一个已发布课件");
            }
        }

        // 9. 完成规则必须为 ALL_REQUIRED_POINTS
        if (!Integer.valueOf(1).equals(course.getCompletionRule())) {
            errors.add("完成规则必须为完成全部必修课程点");
        }

        // 抛出校验错误
        if (!errors.isEmpty()) {
            throw new BusinessException(ErrorCode.COURSE_STRUCTURE_NOT_PUBLISHABLE);
        }
    }

    private Integer parseStatus(String status) {
        return switch (status) {
            case "DRAFT" -> 0;
            case "PUBLISHED" -> 1;
            case "OFFLINE" -> 2;
            default -> null;
        };
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
