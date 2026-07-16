package org.example.nursingtrainingbackend.modules.course.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.example.nursingtrainingbackend.common.event.CacheEvictionEvent;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.modules.course.dto.CompletionRuleDTO;
import org.example.nursingtrainingbackend.modules.course.dto.CreateCourseInitial;
import org.example.nursingtrainingbackend.modules.course.dto.CreatePoint;
import org.example.nursingtrainingbackend.modules.course.dto.DepartmentDTO;
import org.example.nursingtrainingbackend.modules.course.dto.UpdateChapter;
import org.example.nursingtrainingbackend.modules.course.dto.UpdateChapterOrder;
import org.example.nursingtrainingbackend.modules.course.dto.UpdatePointOrder;
import org.example.nursingtrainingbackend.modules.course.entity.*;
import org.example.nursingtrainingbackend.modules.course.mapper.*;
import org.example.nursingtrainingbackend.modules.course.service.CourseCreateService;
import org.example.nursingtrainingbackend.modules.course.vo.*;
import org.example.nursingtrainingbackend.modules.courseware.article.entity.Article;
import org.example.nursingtrainingbackend.modules.courseware.article.mapper.ArticleMapper;
import org.example.nursingtrainingbackend.modules.courseware.ppt.entity.Ppt;
import org.example.nursingtrainingbackend.modules.courseware.ppt.mapper.PptMapper;
import org.example.nursingtrainingbackend.modules.courseware.video.entity.Video;
import org.example.nursingtrainingbackend.modules.courseware.video.mapper.VideoMapper;
import org.example.nursingtrainingbackend.modules.file.service.FileService;
import org.example.nursingtrainingbackend.modules.tag.entity.Tag;
import org.example.nursingtrainingbackend.modules.user.entity.Department;
import org.example.nursingtrainingbackend.modules.user.entity.User;
import org.example.nursingtrainingbackend.modules.user.mapper.DepartmentMapper;
import org.example.nursingtrainingbackend.modules.user.mapper.UserMapper;
import org.example.nursingtrainingbackend.security.SecurityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private CoursePointArticleMapper coursePointArticleMapper;
    @Autowired
    private CoursePointVideoMapper coursePointVideoMapper;
    @Autowired
    private CoursePointPptMapper coursePointPptMapper;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private PptMapper pptMapper;
    @Autowired
    private FileService fileService;
    private final ApplicationEventPublisher eventPublisher;

    public CourseCreateServiceImpl(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public List<InstructorOptionVO> getInstructorOptions(String keyword, Integer limit) {
        //选讲师应该要真实姓名，只查询 role_type=2 的讲师用户
        List<User> users = userMapper.selectList(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getRoleType, 2)
                        .like(StringUtils.hasText(keyword), User::getRealName, keyword)
        );

        return users.stream().map(user -> {
            InstructorOptionVO vo = new InstructorOptionVO();
            vo.setId(user.getId());
            vo.setRealName(user.getRealName());
            vo.setUsername(user.getUsername());
            // 查询真实部门名称
            String deptName = "";
            if (user.getDeptId() != null) {
                Department dept = departmentMapper.selectById(user.getDeptId());
                if (dept != null) deptName = dept.getName();
            }
            vo.setDepartmentName(deptName);
            return vo;
        }).toList();

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
    public CreateCourseInitialVO createCourseInitial(CreateCourseInitial createCourseInitial) {
        Course course = new Course();
        BeanUtils.copyProperties(createCourseInitial, course);
        // BeanUtils 无法将 String startAt 自动转为 LocalDateTime，需手动解析
        if (createCourseInitial.getStartAt() != null && !createCourseInitial.getStartAt().isBlank()) {
            course.setStartAt(OffsetDateTime.parse(createCourseInitial.getStartAt()).toLocalDateTime());
        }
        course.setCreatedAt(LocalDateTime.now());
        course.setCreatedBy(SecurityUtils.currentUserId());
        // 先插入课程，获取自增 ID
        courseMapper.insert(course);
        
        // 标记课程封面文件已使用
        if (createCourseInitial.getCoverUrl() != null && !createCourseInitial.getCoverUrl().isBlank()) {
            String coverKey = extractObjectKey(createCourseInitial.getCoverUrl());
            fileService.markFileUsed(coverKey, "COURSE_COVER", course.getId());
        }

        if (createCourseInitial.getDepartments() != null) {
            for (DepartmentDTO departmentDTO : createCourseInitial.getDepartments()) {
                CourseDepartment courseDepartment = new CourseDepartment();
                courseDepartment.setCourseId(course.getId());
                courseDepartment.setDepartmentId(departmentDTO.getDepartmentId());
                courseDepartment.setRequired(Boolean.TRUE.equals(departmentDTO.getRequired()) ? 1 : 0);
                courseDepartment.setCreatedAt(LocalDateTime.now());
                courseDepartmentMapper.insert(courseDepartment);
            }
        }
        if(createCourseInitial.getTagIds()!=null){
        for(Long tagId:createCourseInitial.getTagIds()){
            CourseTag courseTag = new CourseTag();
            courseTag.setCourseId(course.getId());
            courseTag.setTagId(tagId);
            courseTag.setCreatedAt(LocalDateTime.now());
            courseTagMapper.insert(courseTag);
        }
        }
        CreateCourseInitialVO vo = new CreateCourseInitialVO();
        vo.setCourseId(course.getId());
        vo.setCreatedAt(course.getCreatedAt());
        //eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.DASHBOARD));
        return vo;
    }

    @Override
    public CreateChapterVO createChapter(Long courseId, String title) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
// 2. 仅草稿可操作
        if (!Integer.valueOf(0).equals(course.getStatus())) {
            throw new BusinessException(ErrorCode.COURSE_STATUS_INVALID);
        }
        CourseChapter courseChapter = new CourseChapter();
        courseChapter.setCourseId(courseId);
        courseChapter.setTitle(title);
        courseChapter.setSort(courseMapper.selectMax() + 1);
        courseChapter.setCreatedAt(LocalDateTime.now());
        courseChapterMapper.insert(courseChapter);
        CreateChapterVO vo = new CreateChapterVO();
        BeanUtils.copyProperties(courseChapter, vo);
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.COURSE_STUDY));
        return vo;


    }

    @Override
    @Transactional
    public UpdateChapterVO updateChapter(Long courseId, Long chapterId, UpdateChapter updateChapter) {
        // 1. 校验课程存在且为草稿
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
        if (!Integer.valueOf(0).equals(course.getStatus())) {
            throw new BusinessException(ErrorCode.COURSE_STATUS_INVALID);
        }

        // 2. 校验章节存在且属于当前课程
        CourseChapter chapter = courseChapterMapper.selectById(chapterId);
        if (chapter == null || !chapter.getCourseId().equals(courseId)) {
            throw new BusinessException(ErrorCode.COURSE_CHAPTER_NOT_FOUND);
        }

        // 3. 更新章节标题
        chapter.setTitle(updateChapter.getTitle());
        chapter.setUpdatedAt(LocalDateTime.now());
        courseChapterMapper.updateById(chapter);

        // 4. 构造响应
        UpdateChapterVO vo = new UpdateChapterVO();
        vo.setId(chapter.getId());
        vo.setTitle(chapter.getTitle());
        vo.setSort(chapter.getSort());
        vo.setUpdatedAt(chapter.getUpdatedAt());
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.COURSE_STUDY));
        return vo;
    }

    @Override
    @Transactional
    public CreatePointVO createPoint(Long courseId, Long chapterId, CreatePoint createPoint) {
        // 1. 锁定课程，校验存在且为草稿
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
        if (!Integer.valueOf(0).equals(course.getStatus())) {
            throw new BusinessException(ErrorCode.COURSE_STATUS_INVALID);
        }

        // 2. 校验章节存在且属于当前课程
        CourseChapter chapter = courseChapterMapper.selectById(chapterId);
        if (chapter == null || !chapter.getCourseId().equals(courseId)) {
            throw new BusinessException(ErrorCode.COURSE_CHAPTER_NOT_FOUND);
        }

        // 3. 去重
        List<Long> articleIds = distinct(createPoint.getArticleIds());
        List<Long> videoIds = distinct(createPoint.getVideoIds());
        List<Long> pptIds = distinct(createPoint.getPptIds());

        // 4. 至少关联一个有效课件
        if (articleIds.isEmpty() && videoIds.isEmpty() && pptIds.isEmpty()) {
            throw new BusinessException(ErrorCode.COURSE_POINT_NO_MEDIA);
        }

        // 5. 校验课件存在、已发布且未删除
        validateArticles(articleIds);
        validateVideos(videoIds);
        validatePpts(pptIds);

        // 6. 创建课程点，追加到章节末尾
        CoursePoint point = new CoursePoint();
        point.setCourseId(courseId);
        point.setChapterId(chapterId);
        point.setTitle(createPoint.getTitle());
        point.setDescription(createPoint.getDescription());
        point.setRequired(Boolean.TRUE.equals(createPoint.getRequired()) ? 1 : 0);
        point.setSort(nextSort(chapterId));
        point.setCreatedAt(LocalDateTime.now());
        coursePointMapper.insert(point);

        // 7. 写入三张关系表
        int articleCount = insertArticleJunctions(point.getId(), articleIds);
        int videoCount = insertVideoJunctions(point.getId(), videoIds);
        int pptCount = insertPptJunctions(point.getId(), pptIds);

        // 8. 构造响应
        CreatePointVO vo = new CreatePointVO();
        vo.setTitle(point.getTitle());
        vo.setRequired(Boolean.TRUE.equals(createPoint.getRequired()));
        vo.setSort(point.getSort());
        vo.setArticleCount(articleCount);
        vo.setVideoCount(videoCount);
        vo.setPptCount(pptCount);
        vo.setResourceCount(articleCount + videoCount + pptCount);
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.COURSE_STUDY));
        return vo;
    }

    @Override
    @Transactional
    public UpdatePointVO updatePoint(Long courseId, Long chapterId, Long pointId, CreatePoint createPoint) {
        // 1. 校验课程存在且为草稿
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
        if (!Integer.valueOf(0).equals(course.getStatus())) {
            throw new BusinessException(ErrorCode.COURSE_STATUS_INVALID);
        }

        // 2. 校验章节存在且属于当前课程
        CourseChapter chapter = courseChapterMapper.selectById(chapterId);
        if (chapter == null || !chapter.getCourseId().equals(courseId)) {
            throw new BusinessException(ErrorCode.COURSE_CHAPTER_NOT_FOUND);
        }

        // 3. 校验课程点存在且属于当前章节（不能移动到其他章节）
        CoursePoint point = coursePointMapper.selectById(pointId);
        if (point == null || !point.getChapterId().equals(chapterId)) {
            throw new BusinessException(ErrorCode.COURSE_POINT_NOT_FOUND);
        }

        // 4. 去重
        List<Long> articleIds = distinct(createPoint.getArticleIds());
        List<Long> videoIds = distinct(createPoint.getVideoIds());
        List<Long> pptIds = distinct(createPoint.getPptIds());

        // 5. 至少关联一个有效课件
        if (articleIds.isEmpty() && videoIds.isEmpty() && pptIds.isEmpty()) {
            throw new BusinessException(ErrorCode.COURSE_POINT_NO_MEDIA);
        }

        // 6. 校验课件存在、已发布且未删除
        validateArticles(articleIds);
        validateVideos(videoIds);
        validatePpts(pptIds);

        // 7. 更新课程点主表（排序不变）
        point.setTitle(createPoint.getTitle());
        point.setDescription(createPoint.getDescription());
        point.setRequired(Boolean.TRUE.equals(createPoint.getRequired()) ? 1 : 0);
        point.setUpdatedAt(LocalDateTime.now());
        coursePointMapper.updateById(point);

        // 8. 删除旧关系，批量插入新关系
        deleteOldJunctions(pointId);
        int articleCount = insertArticleJunctions(pointId, articleIds);
        int videoCount = insertVideoJunctions(pointId, videoIds);
        int pptCount = insertPptJunctions(pointId, pptIds);

        // 9. 构造响应
        UpdatePointVO vo = new UpdatePointVO();
        vo.setId(point.getId());
        vo.setTitle(point.getTitle());
        vo.setRequired(Boolean.TRUE.equals(createPoint.getRequired()));
        vo.setSort(point.getSort());
        vo.setArticleCount(articleCount);
        vo.setVideoCount(videoCount);
        vo.setPptCount(pptCount);
        vo.setResourceCount(articleCount + videoCount + pptCount);
        vo.setUpdatedAt(point.getUpdatedAt());
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.COURSE_STUDY));
        return vo;
    }

    @Override
    @Transactional
    public void deletePoint(Long courseId, Long chapterId, Long pointId) {
        // 1. 校验课程存在且为草稿
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
        if (!Integer.valueOf(0).equals(course.getStatus())) {
            throw new BusinessException(ErrorCode.COURSE_STATUS_INVALID);
        }

        // 2. 校验章节存在且属于当前课程
        CourseChapter chapter = courseChapterMapper.selectById(chapterId);
        if (chapter == null || !chapter.getCourseId().equals(courseId)) {
            throw new BusinessException(ErrorCode.COURSE_CHAPTER_NOT_FOUND);
        }

        // 3. 校验课程点存在且属于当前章节
        CoursePoint point = coursePointMapper.selectById(pointId);
        if (point == null || !point.getChapterId().equals(chapterId)) {
            throw new BusinessException(ErrorCode.COURSE_POINT_NOT_FOUND);
        }

        // 4. 删除三张课件关系表（只解除关系，不删除课件库资源）
        deleteOldJunctions(pointId);

        // 5. 软删除课程点
        coursePointMapper.deleteById(pointId);

        // 6. 重新编号该章节剩余课程点为连续 1..N
        List<CoursePoint> remainingPoints = coursePointMapper.selectList(
                Wrappers.<CoursePoint>lambdaQuery()
                        .eq(CoursePoint::getChapterId, chapterId)
                        .orderByAsc(CoursePoint::getSort));
        int newSort = 1;
        for (CoursePoint p : remainingPoints) {
            if (p.getSort() == null || p.getSort() != newSort) {
                p.setSort(newSort);
                coursePointMapper.updateById(p);
            }
            newSort++;
        }
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.COURSE_STUDY));
    }

    @Override
    @Transactional
    public void deleteChapter(Long courseId, Long chapterId) {
        // 1. 校验课程存在且为草稿
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
        if (!Integer.valueOf(0).equals(course.getStatus())) {
            throw new BusinessException(ErrorCode.COURSE_STATUS_INVALID);
        }

        // 2. 校验章节存在且属于当前课程
        CourseChapter chapter = courseChapterMapper.selectById(chapterId);
        if (chapter == null || !chapter.getCourseId().equals(courseId)) {
            throw new BusinessException(ErrorCode.COURSE_CHAPTER_NOT_FOUND);
        }

        // 3. 检查章节下是否存在课程点
        Long pointCount = coursePointMapper.selectCount(
                Wrappers.<CoursePoint>lambdaQuery().eq(CoursePoint::getChapterId, chapterId));
        if (pointCount != null && pointCount > 0) {
            throw new BusinessException(ErrorCode.COURSE_CHAPTER_HAS_POINTS);
        }

        // 4. 删除章节
        courseChapterMapper.deleteById(chapterId);

        // 5. 重新编号该课程剩余章节为连续 1..N
        List<CourseChapter> remainingChapters = courseChapterMapper.selectList(
                Wrappers.<CourseChapter>lambdaQuery()
                        .eq(CourseChapter::getCourseId, courseId)
                        .orderByAsc(CourseChapter::getSort));
        int newSort = 1;
        for (CourseChapter c : remainingChapters) {
            if (c.getSort() == null || c.getSort() != newSort) {
                c.setSort(newSort);
                courseChapterMapper.updateById(c);
            }
            newSort++;
        }
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.COURSE_STUDY));
    }

    @Override
    public CourseDetailVO getCourseDetail(Long courseId) {
        // 1. 查询课程基本信息
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }

        CourseDetailVO vo = new CourseDetailVO();
        vo.setCourseId(course.getId());
        vo.setTitle(course.getTitle());
        vo.setSummary(course.getSummary());
        vo.setLearningObjective(course.getLearningObjective());
        vo.setCategoryId(course.getCategoryId());
        vo.setCoverUrl(course.getCoverUrl());
        vo.setInstructorId(course.getInstructorId());
        if (course.getInstructorId() != null) {
            User instructor = userMapper.selectById(course.getInstructorId());
            if (instructor != null) {
                vo.setInstructorName(instructor.getRealName() != null ? instructor.getRealName() : instructor.getUsername());
                if (instructor.getDeptId() != null) {
                    Department dept = departmentMapper.selectById(instructor.getDeptId());
                    if (dept != null) vo.setInstructorDepartmentName(dept.getName());
                }
            }
        }
        vo.setStartAt(course.getStartAt());
        vo.setStatus(convertStatus(course.getStatus()));
        vo.setCompletionRule(convertCompletionRule(course.getCompletionRule()));
        vo.setCurrentStep(determineCurrentStep(course));

        // 2. 查询标签 IDs
        List<CourseTag> courseTags = courseTagMapper.selectList(
                Wrappers.<CourseTag>lambdaQuery().eq(CourseTag::getCourseId, courseId));
        vo.setTagIds(courseTags.stream().map(CourseTag::getTagId).toList());

        // 3. 查询部门配置（带部门名称）
        List<CourseDepartment> courseDepts = courseDepartmentMapper.selectList(
                Wrappers.<CourseDepartment>lambdaQuery().eq(CourseDepartment::getCourseId, courseId));
        List<CourseDetailVO.DepartmentVO> deptVOs = courseDepts.stream().map(cd -> {
            CourseDetailVO.DepartmentVO deptVO = new CourseDetailVO.DepartmentVO();
            deptVO.setDepartmentId(cd.getDepartmentId());
            deptVO.setRequired(cd.getRequired() != null && cd.getRequired() == 1);
            // 查询部门名称
            Department dept = departmentMapper.selectById(cd.getDepartmentId());
            deptVO.setDepartmentName(dept != null ? dept.getName() : null);
            return deptVO;
        }).toList();
        vo.setDepartments(deptVOs);

        // 4. 查询章节及课程点
        List<CourseChapter> chapters = courseChapterMapper.selectList(
                Wrappers.<CourseChapter>lambdaQuery()
                        .eq(CourseChapter::getCourseId, courseId)
                        .orderByAsc(CourseChapter::getSort)
                        .orderByAsc(CourseChapter::getId));

        List<CourseDetailVO.ChapterVO> chapterVOs = chapters.stream().map(chapter -> {
            CourseDetailVO.ChapterVO chapterVO = new CourseDetailVO.ChapterVO();
            chapterVO.setId(chapter.getId());
            chapterVO.setTitle(chapter.getTitle());
            chapterVO.setSort(chapter.getSort());

            // 查询该章节的课程点
            List<CoursePoint> points = coursePointMapper.selectList(
                    Wrappers.<CoursePoint>lambdaQuery()
                            .eq(CoursePoint::getChapterId, chapter.getId())
                            .orderByAsc(CoursePoint::getSort)
                            .orderByAsc(CoursePoint::getId));

            List<CourseDetailVO.PointVO> pointVOs = points.stream().map(point -> {
                CourseDetailVO.PointVO pointVO = new CourseDetailVO.PointVO();
                pointVO.setId(point.getId());
                pointVO.setTitle(point.getTitle());
                pointVO.setRequired(point.getRequired() != null && point.getRequired() == 1);
                pointVO.setSort(point.getSort());

                // 统计课件数量
                Long articleCount = coursePointArticleMapper.selectCount(
                        Wrappers.<CoursePointArticle>lambdaQuery().eq(CoursePointArticle::getCoursePointId, point.getId()));
                Long videoCount = coursePointVideoMapper.selectCount(
                        Wrappers.<CoursePointVideo>lambdaQuery().eq(CoursePointVideo::getCoursePointId, point.getId()));
                Long pptCount = coursePointPptMapper.selectCount(
                        Wrappers.<CoursePointPpt>lambdaQuery().eq(CoursePointPpt::getCoursePointId, point.getId()));

                pointVO.setArticleCount(articleCount.intValue());
                pointVO.setVideoCount(videoCount.intValue());
                pointVO.setPptCount(pptCount.intValue());
                return pointVO;
            }).toList();

            chapterVO.setPoints(pointVOs);
            return chapterVO;
        }).toList();
        vo.setChapters(chapterVOs);

        // 5. 计算规则统计
        vo.setRuleSummary(buildRuleSummary(vo));

        return vo;
    }

    private CourseDetailVO.RuleSummaryVO buildRuleSummary(CourseDetailVO vo) {
        CourseDetailVO.RuleSummaryVO summary = new CourseDetailVO.RuleSummaryVO();
        int requiredCount = 0;
        int optionalCount = 0;
        int totalArticles = 0;
        int totalVideos = 0;
        int totalPpts = 0;

        if (vo.getChapters() != null) {
            for (CourseDetailVO.ChapterVO chapter : vo.getChapters()) {
                if (chapter.getPoints() != null) {
                    for (CourseDetailVO.PointVO point : chapter.getPoints()) {
                        if (Boolean.TRUE.equals(point.getRequired())) {
                            requiredCount++;
                        } else {
                            optionalCount++;
                        }
                        totalArticles += point.getArticleCount() != null ? point.getArticleCount() : 0;
                        totalVideos += point.getVideoCount() != null ? point.getVideoCount() : 0;
                        totalPpts += point.getPptCount() != null ? point.getPptCount() : 0;
                    }
                }
            }
        }

        summary.setRequiredPointCount(requiredCount);
        summary.setOptionalPointCount(optionalCount);
        summary.setArticleCount(totalArticles);
        summary.setVideoCount(totalVideos);
        summary.setPptCount(totalPpts);

        // 检查结构是否有效（至少有一个必修课程点）
        List<String> errors = new ArrayList<>();
        if (requiredCount == 0 && optionalCount == 0) {
            errors.add("课程至少需要包含一个课程点");
        }
        summary.setStructureValid(errors.isEmpty());
        summary.setValidationErrors(errors);

        return summary;
    }

    private String convertStatus(Integer status) {
        if (status == null) return "DRAFT";
        return switch (status) {
            case 0 -> "DRAFT";
            case 1 -> "PUBLISHED";
            case 2 -> "ARCHIVED";
            default -> "UNKNOWN";
        };
    }

    private String convertCompletionRule(Integer rule) {
        if (rule == null) return "ALL_REQUIRED_POINTS";
        return switch (rule) {
            case 1 -> "ALL_REQUIRED_POINTS";
            default -> "ALL_REQUIRED_POINTS";
        };
    }

    private Integer determineCurrentStep(Course course) {
        // 根据课程状态判断当前向导步骤
        if (course.getStatus() == null || course.getStatus() == 0) {
            // 草稿状态，检查进度
            Long chapterCount = courseChapterMapper.selectCount(
                    Wrappers.<CourseChapter>lambdaQuery().eq(CourseChapter::getCourseId, course.getId()));
            if (chapterCount == 0) return 1; // 第一步：基础信息
            return 2; // 第二步：章节结构
        }
        return 3; // 第三步：规则确认/已发布
    }

    @Override
    @Transactional
    public UpdateChapterOrderVO updateChapterOrder(Long courseId, UpdateChapterOrder updateChapterOrder) {
        // 1. 校验课程存在且为草稿
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
        if (!Integer.valueOf(0).equals(course.getStatus())) {
            throw new BusinessException(ErrorCode.COURSE_STATUS_INVALID);
        }

        List<Long> chapterIds = updateChapterOrder.getChapterIds();

        // 2. 查询课程下所有章节
        List<CourseChapter> existingChapters = courseChapterMapper.selectList(
                Wrappers.<CourseChapter>lambdaQuery().eq(CourseChapter::getCourseId, courseId));
        List<Long> existingIds = existingChapters.stream().map(CourseChapter::getId).toList();

        // 3. 校验请求中的 IDs 与数据库中的完全一致
        if (chapterIds.size() != existingIds.size() || !existingIds.containsAll(chapterIds)) {
            throw new BusinessException(ErrorCode.COURSE_SORT_DATA_INCOMPLETE);
        }

        // 4. 按请求顺序更新 sort 字段
        for (int i = 0; i < chapterIds.size(); i++) {
            CourseChapter chapter = courseChapterMapper.selectById(chapterIds.get(i));
            if (chapter != null) {
                chapter.setSort(i + 1);
                courseChapterMapper.updateById(chapter);
            }
        }

        // 5. 构造响应
        UpdateChapterOrderVO vo = new UpdateChapterOrderVO();
        vo.setChapterIds(chapterIds);
        vo.setAffectedCount(chapterIds.size());
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.COURSE_STUDY));
        return vo;
    }

    @Override
    @Transactional
    public UpdatePointOrderVO updatePointOrder(Long courseId, Long chapterId, UpdatePointOrder updatePointOrder) {
        // 1. 校验课程存在且为草稿
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
        if (!Integer.valueOf(0).equals(course.getStatus())) {
            throw new BusinessException(ErrorCode.COURSE_STATUS_INVALID);
        }

        // 2. 校验章节存在且属于当前课程
        CourseChapter chapter = courseChapterMapper.selectById(chapterId);
        if (chapter == null || !chapter.getCourseId().equals(courseId)) {
            throw new BusinessException(ErrorCode.COURSE_CHAPTER_NOT_FOUND);
        }

        List<Long> pointIds = updatePointOrder.getPointIds();

        // 3. 查询章节下所有课程点
        List<CoursePoint> existingPoints = coursePointMapper.selectList(
                Wrappers.<CoursePoint>lambdaQuery().eq(CoursePoint::getChapterId, chapterId));
        List<Long> existingIds = existingPoints.stream().map(CoursePoint::getId).toList();

        // 4. 校验请求中的 IDs 与数据库中的完全一致
        if (pointIds.size() != existingIds.size() || !existingIds.containsAll(pointIds)) {
            throw new BusinessException(ErrorCode.COURSE_SORT_DATA_INCOMPLETE);
        }

        // 5. 按请求顺序更新 sort 字段
        for (int i = 0; i < pointIds.size(); i++) {
            CoursePoint point = coursePointMapper.selectById(pointIds.get(i));
            if (point != null) {
                point.setSort(i + 1);
                coursePointMapper.updateById(point);
            }
        }

        // 6. 构造响应
        UpdatePointOrderVO vo = new UpdatePointOrderVO();
        vo.setPointIds(pointIds);
        vo.setAffectedCount(pointIds.size());
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.COURSE_STUDY));
        return vo;
    }

    @Override
    @Transactional
    public CompletionRuleVO updateCompletionRule(Long courseId, CompletionRuleDTO dto) {
        // 1. 校验课程存在且为草稿
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
        if (!Integer.valueOf(0).equals(course.getStatus())) {
            throw new BusinessException(ErrorCode.COURSE_STATUS_INVALID);
        }

        // 2. 校验完成规则（目前仅支持 ALL_REQUIRED_POINTS）
        if (!"ALL_REQUIRED_POINTS".equals(dto.getCompletionRule())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        // 3. 保存 completion_rule=1
        course.setCompletionRule(1);
        course.setUpdatedAt(LocalDateTime.now());
        courseMapper.updateById(course);

        // 4. 查询所有启用、未删除的课程点
        List<CoursePoint> points = coursePointMapper.selectList(
                Wrappers.<CoursePoint>lambdaQuery()
                        .eq(CoursePoint::getCourseId, courseId)
                        .eq(CoursePoint::getStatus, 1));

        int requiredPointCount = 0;
        int optionalPointCount = 0;
        List<Long> pointIds = new ArrayList<>();
        for (CoursePoint p : points) {
            if (p.getRequired() != null && p.getRequired() == 1) {
                requiredPointCount++;
            } else {
                optionalPointCount++;
            }
            pointIds.add(p.getId());
        }

        // 5. 统计课件数量（关系仍存在、课件已发布且未删除）
        int articleCount = 0;
        int videoCount = 0;
        int pptCount = 0;
        if (!pointIds.isEmpty()) {
            List<CoursePointArticle> cpas = coursePointArticleMapper.selectList(
                    Wrappers.<CoursePointArticle>lambdaQuery().in(CoursePointArticle::getCoursePointId, pointIds));
            for (CoursePointArticle cpa : cpas) {
                Article a = articleMapper.selectById(cpa.getArticleId());
                if (a != null && Integer.valueOf(1).equals(a.getStatus())) articleCount++;
            }

            List<CoursePointVideo> cpvs = coursePointVideoMapper.selectList(
                    Wrappers.<CoursePointVideo>lambdaQuery().in(CoursePointVideo::getCoursePointId, pointIds));
            for (CoursePointVideo cpv : cpvs) {
                Video v = videoMapper.selectById(cpv.getVideoId());
                if (v != null && Integer.valueOf(1).equals(v.getStatus())) videoCount++;
            }

            List<CoursePointPpt> cpps = coursePointPptMapper.selectList(
                    Wrappers.<CoursePointPpt>lambdaQuery().in(CoursePointPpt::getCoursePointId, pointIds));
            for (CoursePointPpt cpp : cpps) {
                Ppt p = pptMapper.selectById(cpp.getPptId());
                if (p != null && Integer.valueOf(1).equals(p.getStatus())) pptCount++;
            }
        }

        // 6. 查询部门信息
        List<CourseDepartment> courseDepts = courseDepartmentMapper.selectList(
                Wrappers.<CourseDepartment>lambdaQuery().eq(CourseDepartment::getCourseId, courseId));
        List<CompletionRuleVO.DepartmentVO> deptVOs = courseDepts.stream().map(cd -> {
            CompletionRuleVO.DepartmentVO deptVO = new CompletionRuleVO.DepartmentVO();
            deptVO.setDepartmentId(cd.getDepartmentId());
            deptVO.setRequired(cd.getRequired() != null && cd.getRequired() == 1);
            Department dept = departmentMapper.selectById(cd.getDepartmentId());
            deptVO.setDepartmentName(dept != null ? dept.getName() : null);
            return deptVO;
        }).toList();

        // 7. 结构校验
        List<String> errors = new ArrayList<>();
        Long chapterCount = courseChapterMapper.selectCount(
                Wrappers.<CourseChapter>lambdaQuery().eq(CourseChapter::getCourseId, courseId));
        if (chapterCount == 0) {
            errors.add("课程至少需要包含一个章节");
        }
        if (requiredPointCount == 0 && optionalPointCount == 0) {
            errors.add("课程至少需要包含一个课程点");
        }
        if (courseDepts.isEmpty()) {
            errors.add("课程至少需要配置一个发布部门");
        }

        // 8. 构造响应
        CompletionRuleVO vo = new CompletionRuleVO();
        vo.setCourseId(courseId);
        vo.setCompletionRule("ALL_REQUIRED_POINTS");
        vo.setRequiredPointCount(requiredPointCount);
        vo.setOptionalPointCount(optionalPointCount);
        vo.setArticleCount(articleCount);
        vo.setVideoCount(videoCount);
        vo.setPptCount(pptCount);
        vo.setDepartments(deptVOs);
        vo.setStructureValid(errors.isEmpty());
        vo.setValidationErrors(errors);
        vo.setCurrentStep(3);
        eventPublisher.publishEvent(new CacheEvictionEvent(this, CacheEvictionEvent.Scope.COURSE_STUDY));
        return vo;
    }

    // ---------- 辅助方法 ----------

    private List<Long> distinct(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return ids.stream().distinct().toList();
    }

    private int nextSort(Long chapterId) {
        CoursePoint last = coursePointMapper.selectOne(
                Wrappers.<CoursePoint>lambdaQuery()
                        .eq(CoursePoint::getChapterId, chapterId)
                        .orderByDesc(CoursePoint::getSort)
                        .last("LIMIT 1"));
        return (last == null || last.getSort() == null) ? 1 : last.getSort() + 1;
    }

    private void validateArticles(List<Long> ids) {
        if (ids.isEmpty()) return;
        List<Article> articles = articleMapper.selectBatchIds(ids);
        // 检查数量是否一致，防止部分 ID 不存在
        if (articles.size() != ids.size()) {
            throw new BusinessException(ErrorCode.COURSE_MEDIA_INVALID);
        }
        for (Article a : articles) {
            if (!Integer.valueOf(1).equals(a.getStatus())) {
                throw new BusinessException(ErrorCode.COURSE_MEDIA_INVALID);
            }
        }
    }

    private void validateVideos(List<Long> ids) {
        if (ids.isEmpty()) return;
        List<Video> videos = videoMapper.selectBatchIds(ids);
        if (videos.size() != ids.size()) {
            throw new BusinessException(ErrorCode.COURSE_MEDIA_INVALID);
        }
        for (Video v : videos) {
            // 必须已发布（status=1），即转码成功可播放状态
            if (!Integer.valueOf(1).equals(v.getStatus())) {
                throw new BusinessException(ErrorCode.COURSE_MEDIA_INVALID);
            }
        }
    }

    private void validatePpts(List<Long> ids) {
        if (ids.isEmpty()) return;
        List<Ppt> ppts = pptMapper.selectBatchIds(ids);
        if (ppts.size() != ids.size()) {
            throw new BusinessException(ErrorCode.COURSE_MEDIA_INVALID);
        }
        for (Ppt p : ppts) {
            if (!Integer.valueOf(1).equals(p.getStatus())) {
                throw new BusinessException(ErrorCode.COURSE_MEDIA_INVALID);
            }
        }
    }

    private void deleteOldJunctions(Long pointId) {
        coursePointArticleMapper.delete(
                Wrappers.<CoursePointArticle>lambdaQuery().eq(CoursePointArticle::getCoursePointId, pointId));
        coursePointVideoMapper.delete(
                Wrappers.<CoursePointVideo>lambdaQuery().eq(CoursePointVideo::getCoursePointId, pointId));
        coursePointPptMapper.delete(
                Wrappers.<CoursePointPpt>lambdaQuery().eq(CoursePointPpt::getCoursePointId, pointId));
    }

    private int insertArticleJunctions(Long pointId, List<Long> ids) {
        int count = 0;
        for (Long id : ids) {
            CoursePointArticle j = new CoursePointArticle();
            j.setCoursePointId(pointId);
            j.setArticleId(id);
            j.setCreatedAt(LocalDateTime.now());
            coursePointArticleMapper.insert(j);
            count++;
        }
        return count;
    }

    private int insertVideoJunctions(Long pointId, List<Long> ids) {
        int count = 0;
        for (Long id : ids) {
            CoursePointVideo j = new CoursePointVideo();
            j.setCoursePointId(pointId);
            j.setVideoId(id);
            j.setCreatedAt(LocalDateTime.now());
            coursePointVideoMapper.insert(j);
            count++;
        }
        return count;
    }

    private int insertPptJunctions(Long pointId, List<Long> ids) {
        int count = 0;
        for (Long id : ids) {
            CoursePointPpt j = new CoursePointPpt();
            j.setCoursePointId(pointId);
            j.setPptId(id);
            j.setCreatedAt(LocalDateTime.now());
            coursePointPptMapper.insert(j);
            count++;
        }
        return count;
    }
    
    /**
     * 从完整URL中提取OSS ObjectKey
     */
    private String extractObjectKey(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            if (path != null && path.length() > 1) {
                return path.startsWith("/") ? path.substring(1) : path;
            }
        } catch (URISyntaxException ignored) {
        }
        String trimmed = url.startsWith("/") ? url.substring(1) : url;
        return trimmed;
    }
}