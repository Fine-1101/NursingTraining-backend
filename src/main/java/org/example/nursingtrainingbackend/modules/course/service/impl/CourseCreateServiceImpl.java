package org.example.nursingtrainingbackend.modules.course.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.modules.category.entity.Category;
import org.example.nursingtrainingbackend.modules.category.mapper.CategoryMapper;
import org.example.nursingtrainingbackend.modules.course.dto.CreateCourseInitial;
import org.example.nursingtrainingbackend.modules.course.dto.CreatePointDTO;
import org.example.nursingtrainingbackend.modules.course.dto.DepartmentDTO;
import org.example.nursingtrainingbackend.modules.course.entity.*;
import org.example.nursingtrainingbackend.modules.course.mapper.*;
import org.example.nursingtrainingbackend.modules.course.service.CourseCreateService;
import org.example.nursingtrainingbackend.modules.course.vo.*;
import org.example.nursingtrainingbackend.modules.user.entity.Department;
import org.example.nursingtrainingbackend.modules.user.entity.User;
import org.example.nursingtrainingbackend.modules.user.mapper.DepartmentMapper;
import org.example.nursingtrainingbackend.modules.user.mapper.UserMapper;
import org.example.nursingtrainingbackend.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CourseCreateServiceImpl implements CourseCreateService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DepartmentMapper departmentMapper;
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
    private CoursePointMediaMapper coursePointMediaMapper;
    @Autowired
    private CategoryMapper categoryMapper;

    private static final int ROLE_TYPE_INSTRUCTOR = 3;

    @Override
    public List<InstructorOptionVO> getInstructorOptions(String keyword, Integer limit) {
        List<User> users = userMapper.selectList(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getRoleType, ROLE_TYPE_INSTRUCTOR)
                        .eq(User::getStatus, 1)
                        .like(StringUtils.hasText(keyword), User::getRealName, keyword)
                        .last("LIMIT " + (limit != null ? limit : 10))
        );

        var deptIds = users.stream().map(User::getDeptId).filter(java.util.Objects::nonNull).distinct().toList();
        var deptMap = new HashMap<Long, String>();
        if (!deptIds.isEmpty()) {
            departmentMapper.selectBatchIds(deptIds).forEach(dept -> deptMap.put(dept.getId(), dept.getName()));
        }

        return users.stream().map(user -> {
            InstructorOptionVO vo = new InstructorOptionVO();
            vo.setId(user.getId());
            vo.setRealname(user.getRealName());
            vo.setUsername(user.getUsername());
            vo.setDepartmentName(user.getDeptId() != null ? deptMap.getOrDefault(user.getDeptId(), "") : "");
            return vo;
        }).toList();
    }

    @Override
    public PageResult<CourseListItemVO> getCourses(String keyword, String categoryId, String status, Integer page, Integer size) {
        int p = page != null ? page : 1;
        int s = size != null ? size : 20;

        Integer statusInt = null;
        if ("PUBLISHED".equals(status)) statusInt = 1;
        else if ("DRAFT".equals(status)) statusInt = 0;
        else if ("OFFLINE".equals(status)) statusInt = 2;

        // If keyword is present, find matching instructor IDs for OR condition
        List<Long> matchedInstructorIds = List.of();
        if (StringUtils.hasText(keyword)) {
            matchedInstructorIds = userMapper.selectList(
                    Wrappers.<User>lambdaQuery()
                            .like(User::getRealName, keyword)
                            .select(User::getId)
            ).stream().map(User::getId).toList();
        }

        final List<Long> instructorIdList = matchedInstructorIds;

        IPage<Course> coursePage = courseMapper.selectPage(
                new Page<>(p, s),
                Wrappers.<Course>lambdaQuery()
                        .and(StringUtils.hasText(keyword), w -> {
                            w.like(Course::getTitle, keyword)
                             .or()
                             .in(Course::getInstructorId, instructorIdList);
                        })
                        .eq(StringUtils.hasText(categoryId), Course::getCategoryId,
                                StringUtils.hasText(categoryId) ? Long.parseLong(categoryId) : null)
                        .eq(statusInt != null, Course::getStatus, statusInt)
                        .orderByDesc(Course::getUpdatedAt)
        );

        // Batch load instructor names
        var instructorIds = coursePage.getRecords().stream()
                .map(Course::getInstructorId).filter(java.util.Objects::nonNull).distinct().toList();
        var instructorMap = new HashMap<Long, String>();
        if (!instructorIds.isEmpty()) {
            userMapper.selectBatchIds(instructorIds).forEach(u ->
                    instructorMap.put(u.getId(), u.getRealName() != null ? u.getRealName() : u.getUsername()));
        }

        // Batch load category names
        var categoryIds = coursePage.getRecords().stream()
                .map(Course::getCategoryId).filter(java.util.Objects::nonNull).distinct().toList();
        var categoryMap = new HashMap<Long, String>();
        if (!categoryIds.isEmpty()) {
            categoryMapper.selectBatchIds(categoryIds).forEach(c -> categoryMap.put(c.getId(), c.getName()));
        }

        List<CourseListItemVO> records = coursePage.getRecords().stream().map(course -> {
            CourseListItemVO vo = new CourseListItemVO();
            vo.setId(course.getId());
            vo.setTitle(course.getTitle());
            vo.setSummary(course.getSummary());
            vo.setCoverUrl(course.getCoverUrl());
            if (course.getStatus() != null && course.getStatus() == 1) vo.setStatus("PUBLISHED");
            else if (course.getStatus() != null && course.getStatus() == 2) vo.setStatus("OFFLINE");
            else vo.setStatus("DRAFT");
            vo.setInstructorName(course.getInstructorId() != null ? instructorMap.getOrDefault(course.getInstructorId(), "") : "");
            vo.setCategoryName(course.getCategoryId() != null ? categoryMap.getOrDefault(course.getCategoryId(), "") : "");
            vo.setCreatedAt(course.getCreatedAt());
            vo.setUpdatedAt(course.getUpdatedAt() != null ? course.getUpdatedAt() : course.getCreatedAt());
            vo.setPublishedAt(course.getPublishedAt());
            Long chCount = courseChapterMapper.selectCount(
                    Wrappers.<CourseChapter>lambdaQuery().eq(CourseChapter::getCourseId, course.getId()));
            Long ptCount = coursePointMapper.selectCount(
                    Wrappers.<CoursePoint>lambdaQuery().eq(CoursePoint::getCourseId, course.getId()));
            vo.setChapterCount(chCount != null ? chCount.intValue() : 0);
            vo.setPointCount(ptCount != null ? ptCount.intValue() : 0);
            vo.setStudentCount(0);
            return vo;
        }).toList();

        return new PageResult<>(records, coursePage.getTotal(), coursePage.getCurrent(), coursePage.getSize(), coursePage.getPages());
    }

    @Override
    public CourseOverviewVO getCourseOverview() {
        Long total = courseMapper.selectCount(null);
        Long draft = courseMapper.selectCount(Wrappers.<Course>lambdaQuery().eq(Course::getStatus, 0));
        Long published = courseMapper.selectCount(Wrappers.<Course>lambdaQuery().eq(Course::getStatus, 1));
        Long offline = courseMapper.selectCount(Wrappers.<Course>lambdaQuery().eq(Course::getStatus, 2));

        CourseOverviewVO vo = new CourseOverviewVO();
        vo.setTotal(new CourseOverviewVO.StatItem(total != null ? total.intValue() : 0));
        vo.setDraft(new CourseOverviewVO.StatItem(draft != null ? draft.intValue() : 0));
        vo.setPublished(new CourseOverviewVO.StatItem(published != null ? published.intValue() : 0));
        vo.setOffline(new CourseOverviewVO.StatItem(offline != null ? offline.intValue() : 0));
        return vo;
    }

    @Override
    public List<DepartmentOptionVO> getDepartmentOptions() {
        List<Department> departments = departmentMapper.selectList(null);
        return departments.stream().map(department -> {
            DepartmentOptionVO vo = new DepartmentOptionVO();
            vo.setId(department.getId());
            vo.setName(department.getName());
            return vo;
        }).toList();
    }

    @Override
    @Transactional
    public CreateCourseInitialVO createCourseInitial(CreateCourseInitial dto) {
        try {
            Course course = new Course();
            course.setTitle(dto.getTitle());
            course.setSummary(dto.getSummary());
            course.setLearningObjective(dto.getLearningObjective());
            course.setCategoryId(dto.getCategoryId());
            course.setCoverUrl(dto.getCoverUrl());
            course.setInstructorId(dto.getInstructorId());

            if (StringUtils.hasText(dto.getStartAt())) {
                try {
                    OffsetDateTime odt = OffsetDateTime.parse(dto.getStartAt());
                    course.setStartAt(odt.toLocalDateTime());
                } catch (Exception e1) {
                    try {
                        course.setStartAt(LocalDateTime.parse(dto.getStartAt(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    } catch (Exception e2) {
                        try {
                            course.setStartAt(LocalDateTime.parse(dto.getStartAt(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                        } catch (Exception e3) {
                            log.warn("无法解析startAt格式: {}", dto.getStartAt());
                        }
                    }
                }
            }

            course.setStatus(0);
            course.setCreatedAt(LocalDateTime.now());
            course.setCreatedBy(SecurityUtils.currentUserId());

            courseMapper.insert(course);
            log.info("课程插入成功, courseId={}", course.getId());

            if (dto.getDepartments() != null) {
                for (DepartmentDTO departmentDTO : dto.getDepartments()) {
                    CourseDepartment courseDepartment = new CourseDepartment();
                    courseDepartment.setCourseId(course.getId());
                    courseDepartment.setDepartmentId(departmentDTO.getDepartmentId());
                    courseDepartment.setRequired(Boolean.TRUE.equals(departmentDTO.getRequired()) ? 1 : 0);
                    courseDepartmentMapper.insert(courseDepartment);
                }
            }

            if (dto.getTagIds() != null) {
                for (Long tagId : dto.getTagIds()) {
                    CourseTag courseTag = new CourseTag();
                    courseTag.setCourseId(course.getId());
                    courseTag.setTagId(tagId);
                    courseTagMapper.insert(courseTag);
                }
            }

            CreateCourseInitialVO vo = new CreateCourseInitialVO();
            vo.setCourseId(course.getId());
            vo.setCreatedAt(course.getCreatedAt());
            log.info("课程创建完成, 返回VO: courseId={}", vo.getCourseId());
            return vo;
        } catch (Exception e) {
            log.error("创建课程失败", e);
            throw e;
        }
    }

    @Override
    public CourseDetailVO getCourseDetail(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }

        CourseDetailVO vo = new CourseDetailVO();
        vo.setId(course.getId());
        vo.setTitle(course.getTitle());
        vo.setSummary(course.getSummary());
        vo.setLearningObjective(course.getLearningObjective());
        vo.setCategoryId(course.getCategoryId());
        vo.setCoverUrl(course.getCoverUrl());
        vo.setInstructorId(course.getInstructorId());
        if (course.getStartAt() != null) {
            vo.setStartAt(course.getStartAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        vo.setStatus(course.getStatus() == 1 ? "PUBLISHED" : "DRAFT");
        vo.setCurrentStep(2);
        vo.setCompletionRule("ALL_REQUIRED_POINTS");

        List<CourseTag> courseTags = courseTagMapper.selectList(
                Wrappers.<CourseTag>lambdaQuery().eq(CourseTag::getCourseId, courseId));
        vo.setTagIds(courseTags.stream().map(CourseTag::getTagId).toList());

        List<CourseDepartment> courseDepts = courseDepartmentMapper.selectList(
                Wrappers.<CourseDepartment>lambdaQuery().eq(CourseDepartment::getCourseId, courseId));
        List<CourseDetailVO.DepartmentItem> deptItems = courseDepts.stream().map(cd -> {
            CourseDetailVO.DepartmentItem item = new CourseDetailVO.DepartmentItem();
            item.setDepartmentId(cd.getDepartmentId());
            item.setRequired(cd.getRequired() != null && cd.getRequired() == 1);
            return item;
        }).toList();
        vo.setDepartments(deptItems);

        List<CourseChapter> chapters = courseChapterMapper.selectList(
                Wrappers.<CourseChapter>lambdaQuery()
                        .eq(CourseChapter::getCourseId, courseId)
                        .orderByAsc(CourseChapter::getSort));
        List<CoursePoint> allPoints = coursePointMapper.selectList(
                Wrappers.<CoursePoint>lambdaQuery().eq(CoursePoint::getCourseId, courseId));
        Map<Long, List<CoursePoint>> pointsByChapter = allPoints.stream()
                .collect(Collectors.groupingBy(CoursePoint::getChapterId));

        List<CourseDetailVO.ChapterItem> chapterItems = chapters.stream().map(ch -> {
            CourseDetailVO.ChapterItem ci = new CourseDetailVO.ChapterItem();
            ci.setId(ch.getId());
            ci.setTitle(ch.getTitle());
            ci.setSort(ch.getSort());
            List<CoursePoint> pts = pointsByChapter.getOrDefault(ch.getId(), List.of());
            List<CourseDetailVO.PointItem> pointItems = pts.stream().map(this::buildPointItem).toList();
            ci.setPoints(pointItems);
            return ci;
        }).toList();
        vo.setChapters(chapterItems);

        // Build ruleSummary
        int requiredCount = 0;
        int optionalCount = 0;
        for (CoursePoint p : allPoints) {
            if (p.getRequired() != null && p.getRequired() == 1) {
                requiredCount++;
            } else {
                optionalCount++;
            }
        }
        Map<String, Object> ruleSummaryMap = new HashMap<>();
        ruleSummaryMap.put("completionRule", "ALL_REQUIRED_POINTS");
        ruleSummaryMap.put("requiredPointCount", requiredCount);
        ruleSummaryMap.put("optionalPointCount", optionalCount);
        ruleSummaryMap.put("structureValid", !chapters.isEmpty() && !allPoints.isEmpty());
        vo.setRuleSummary(ruleSummaryMap);

        return vo;
    }

    @Override
    @Transactional
    public CourseDetailVO.ChapterItem createChapter(Long courseId, String title) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }

        Integer maxSort = courseChapterMapper.selectList(
                        Wrappers.<CourseChapter>lambdaQuery()
                                .eq(CourseChapter::getCourseId, courseId)
                                .orderByDesc(CourseChapter::getSort)
                                .last("LIMIT 1"))
                .stream().findFirst().map(CourseChapter::getSort).orElse(0);

        CourseChapter chapter = new CourseChapter();
        chapter.setCourseId(courseId);
        chapter.setTitle(title);
        chapter.setSort(maxSort + 1);
        chapter.setStatus(1);
        chapter.setCreatedAt(LocalDateTime.now());
        chapter.setUpdatedAt(LocalDateTime.now());
        courseChapterMapper.insert(chapter);

        CourseDetailVO.ChapterItem item = new CourseDetailVO.ChapterItem();
        item.setId(chapter.getId());
        item.setTitle(chapter.getTitle());
        item.setSort(chapter.getSort());
        item.setPoints(new ArrayList<>());
        return item;
    }

    @Override
    @Transactional
    public CourseDetailVO.ChapterItem updateChapter(Long courseId, Long chapterId, String title) {
        CourseChapter chapter = courseChapterMapper.selectById(chapterId);
        if (chapter == null || !chapter.getCourseId().equals(courseId)) {
            throw new BusinessException(ErrorCode.COURSE_CHAPTER_NOT_FOUND);
        }
        chapter.setTitle(title);
        chapter.setUpdatedAt(LocalDateTime.now());
        courseChapterMapper.updateById(chapter);

        CourseDetailVO.ChapterItem item = new CourseDetailVO.ChapterItem();
        item.setId(chapter.getId());
        item.setTitle(chapter.getTitle());
        item.setSort(chapter.getSort());
        return item;
    }

    @Override
    @Transactional
    public void deleteChapter(Long courseId, Long chapterId) {
        CourseChapter chapter = courseChapterMapper.selectById(chapterId);
        if (chapter == null || !chapter.getCourseId().equals(courseId)) {
            throw new BusinessException(ErrorCode.COURSE_CHAPTER_NOT_FOUND);
        }
        Long pointCount = coursePointMapper.selectCount(
                Wrappers.<CoursePoint>lambdaQuery()
                        .eq(CoursePoint::getChapterId, chapterId));
        if (pointCount > 0) {
            throw new BusinessException(ErrorCode.COURSE_CHAPTER_HAS_POINTS);
        }
        courseChapterMapper.deleteById(chapterId);
    }

    @Override
    @Transactional
    public void orderChapters(Long courseId, List<Long> chapterIds) {
        for (int i = 0; i < chapterIds.size(); i++) {
            CourseChapter chapter = courseChapterMapper.selectById(chapterIds.get(i));
            if (chapter != null && chapter.getCourseId().equals(courseId)) {
                chapter.setSort(i + 1);
                chapter.setUpdatedAt(LocalDateTime.now());
                courseChapterMapper.updateById(chapter);
            }
        }
    }

    @Override
    @Transactional
    public CourseDetailVO.PointItem createPoint(Long courseId, Long chapterId, CreatePointDTO dto) {
        CourseChapter chapter = courseChapterMapper.selectById(chapterId);
        if (chapter == null || !chapter.getCourseId().equals(courseId)) {
            throw new BusinessException(ErrorCode.COURSE_CHAPTER_NOT_FOUND);
        }

        Integer maxSort = coursePointMapper.selectList(
                        Wrappers.<CoursePoint>lambdaQuery()
                                .eq(CoursePoint::getChapterId, chapterId)
                                .orderByDesc(CoursePoint::getSort)
                                .last("LIMIT 1"))
                .stream().findFirst().map(CoursePoint::getSort).orElse(0);

        CoursePoint point = new CoursePoint();
        point.setCourseId(courseId);
        point.setChapterId(chapterId);
        point.setTitle(dto.getTitle());
        point.setDescription(dto.getDescription());
        point.setRequired(Boolean.TRUE.equals(dto.getRequired()) ? 1 : 0);
        point.setSort(maxSort + 1);
        point.setStatus(1);
        point.setCreatedAt(LocalDateTime.now());
        point.setUpdatedAt(LocalDateTime.now());
        coursePointMapper.insert(point);

        saveMediaAssociations(point.getId(), dto);
        return buildPointItem(point);
    }

    @Override
    public CourseDetailVO.PointItem getPoint(Long courseId, Long chapterId, Long pointId) {
        CoursePoint point = coursePointMapper.selectById(pointId);
        if (point == null || !point.getCourseId().equals(courseId) || !point.getChapterId().equals(chapterId)) {
            throw new BusinessException(ErrorCode.COURSE_POINT_NOT_FOUND);
        }
        return buildPointItem(point);
    }

    @Override
    @Transactional
    public CourseDetailVO.PointItem updatePoint(Long courseId, Long chapterId, Long pointId, CreatePointDTO dto) {
        CoursePoint point = coursePointMapper.selectById(pointId);
        if (point == null || !point.getCourseId().equals(courseId) || !point.getChapterId().equals(chapterId)) {
            throw new BusinessException(ErrorCode.COURSE_POINT_NOT_FOUND);
        }

        point.setTitle(dto.getTitle());
        point.setDescription(dto.getDescription());
        point.setRequired(Boolean.TRUE.equals(dto.getRequired()) ? 1 : 0);
        point.setUpdatedAt(LocalDateTime.now());
        coursePointMapper.updateById(point);

        coursePointMediaMapper.delete(
                Wrappers.<CoursePointMedia>lambdaQuery().eq(CoursePointMedia::getPointId, pointId));
        saveMediaAssociations(point.getId(), dto);

        return buildPointItem(point);
    }

    @Override
    @Transactional
    public void deletePoint(Long courseId, Long chapterId, Long pointId) {
        CoursePoint point = coursePointMapper.selectById(pointId);
        if (point == null || !point.getCourseId().equals(courseId) || !point.getChapterId().equals(chapterId)) {
            throw new BusinessException(ErrorCode.COURSE_POINT_NOT_FOUND);
        }
        coursePointMediaMapper.delete(
                Wrappers.<CoursePointMedia>lambdaQuery().eq(CoursePointMedia::getPointId, pointId));
        coursePointMapper.deleteById(pointId);
    }

    @Override
    @Transactional
    public void orderPoints(Long courseId, Long chapterId, List<Long> pointIds) {
        for (int i = 0; i < pointIds.size(); i++) {
            CoursePoint point = coursePointMapper.selectById(pointIds.get(i));
            if (point != null && point.getCourseId().equals(courseId) && point.getChapterId().equals(chapterId)) {
                point.setSort(i + 1);
                point.setUpdatedAt(LocalDateTime.now());
                coursePointMapper.updateById(point);
            }
        }
    }

    private void saveMediaAssociations(Long pointId, CreatePointDTO dto) {
        if (dto.getArticleIds() != null) {
            for (Long articleId : dto.getArticleIds()) {
                CoursePointMedia m = new CoursePointMedia();
                m.setPointId(pointId);
                m.setMediaType("ARTICLE");
                m.setMediaId(articleId);
                coursePointMediaMapper.insert(m);
            }
        }
        if (dto.getVideoIds() != null) {
            for (Long videoId : dto.getVideoIds()) {
                CoursePointMedia m = new CoursePointMedia();
                m.setPointId(pointId);
                m.setMediaType("VIDEO");
                m.setMediaId(videoId);
                coursePointMediaMapper.insert(m);
            }
        }
        if (dto.getPptIds() != null) {
            for (Long pptId : dto.getPptIds()) {
                CoursePointMedia m = new CoursePointMedia();
                m.setPointId(pointId);
                m.setMediaType("PPT");
                m.setMediaId(pptId);
                coursePointMediaMapper.insert(m);
            }
        }
    }

    private CourseDetailVO.PointItem buildPointItem(CoursePoint point) {
        List<CoursePointMedia> mediaList = coursePointMediaMapper.selectList(
                Wrappers.<CoursePointMedia>lambdaQuery().eq(CoursePointMedia::getPointId, point.getId()));
        int articleCount = (int) mediaList.stream().filter(m -> "ARTICLE".equals(m.getMediaType())).count();
        int videoCount = (int) mediaList.stream().filter(m -> "VIDEO".equals(m.getMediaType())).count();
        int pptCount = (int) mediaList.stream().filter(m -> "PPT".equals(m.getMediaType())).count();

        CourseDetailVO.PointItem pi = new CourseDetailVO.PointItem();
        pi.setId(point.getId());
        pi.setTitle(point.getTitle());
        pi.setDescription(point.getDescription());
        pi.setRequired(point.getRequired() != null && point.getRequired() == 1);
        pi.setSort(point.getSort());
        pi.setResourceCount(articleCount + videoCount + pptCount);
        pi.setArticleCount(articleCount);
        pi.setVideoCount(videoCount);
        pi.setPptCount(pptCount);
        return pi;
    }
}
