package org.example.nursingtrainingbackend.modules.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.modules.category.entity.Category;
import org.example.nursingtrainingbackend.modules.category.mapper.CategoryMapper;
import org.example.nursingtrainingbackend.modules.course.entity.Course;
import org.example.nursingtrainingbackend.modules.course.entity.CourseDepartment;
import org.example.nursingtrainingbackend.modules.course.entity.CoursePoint;
import org.example.nursingtrainingbackend.modules.course.mapper.CourseDepartmentMapper;
import org.example.nursingtrainingbackend.modules.course.mapper.CourseMapper;
import org.example.nursingtrainingbackend.modules.course.mapper.CoursePointMapper;
import org.example.nursingtrainingbackend.modules.learning.dto.LearnerPageQuery;
import org.example.nursingtrainingbackend.modules.learning.entity.UserCoursePointProgress;
import org.example.nursingtrainingbackend.modules.learning.entity.UserCourseProgress;
import org.example.nursingtrainingbackend.modules.learning.entity.UserCourseResourceProgress;
import org.example.nursingtrainingbackend.modules.learning.mapper.UserCoursePointProgressMapper;
import org.example.nursingtrainingbackend.modules.learning.mapper.UserCourseProgressMapper;
import org.example.nursingtrainingbackend.modules.learning.mapper.UserCourseResourceProgressMapper;
import org.example.nursingtrainingbackend.modules.learning.service.LearnerHomeService;
import org.example.nursingtrainingbackend.modules.learning.vo.*;
import org.example.nursingtrainingbackend.modules.user.entity.User;
import org.example.nursingtrainingbackend.modules.user.mapper.UserMapper;
import org.example.nursingtrainingbackend.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学员端首页服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearnerHomeServiceImpl implements LearnerHomeService {

    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final CategoryMapper categoryMapper;
    private final CourseDepartmentMapper courseDepartmentMapper;
    private final CoursePointMapper coursePointMapper;
    private final UserCourseProgressMapper userCourseProgressMapper;
    private final UserCoursePointProgressMapper userCoursePointProgressMapper;
    private final UserCourseResourceProgressMapper userCourseResourceProgressMapper;

    @Override
    public HomePageVO getHomePage() {
        Long userId = SecurityUtils.currentUserId();
        
        // 1. 验证学员状态
        User user = validateLearner(userId);
        
        // 2. 获取学员可见的课程ID列表
        List<Long> visibleCourseIds = getVisibleCourseIds(user.getDeptId());
        
        if (visibleCourseIds.isEmpty()) {
            return buildEmptyHomePage();
        }
        
        // 3. 构建首页数据
        HomePageVO homePageVO = new HomePageVO();
        
        // 3.1 课程状态统计
        homePageVO.setCourseStats(buildCourseStats(visibleCourseIds, userId));
        
        // 3.2 推荐课程（最多4条）
        homePageVO.setRecommendedCourses(getRecommendedCoursesForHome(visibleCourseIds, userId, 4));
        
        // 3.3 继续学习（最多4条）
        homePageVO.setContinueCourses(getContinueCoursesForHome(userId, 4));
        
        // 3.4 进度概览
        homePageVO.setProgressOverview(buildProgressOverview(visibleCourseIds, userId));
        
        // 3.5 最近学习记录（最多5条）
        homePageVO.setRecentRecords(getRecentRecordsForHome(userId, 5));
        
        // 3.6 学习日历
        homePageVO.setCalendar(buildCalendar(userId));
        
        return homePageVO;
    }

    @Override
    public PageResult<RecommendedCourseVO> getRecommendedCourses(LearnerPageQuery query) {
        Long userId = SecurityUtils.currentUserId();
        User user = validateLearner(userId);
        
        List<Long> visibleCourseIds = getVisibleCourseIds(user.getDeptId());
        if (visibleCourseIds.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L, query.getPage().longValue(), query.getSize().longValue(), 0L);
        }
        
        // 获取推荐课程（排除已完成的）
        List<Long> recommendedCourseIds = getRecommendedCourseIds(visibleCourseIds, userId);
        
        // 分页
        int fromIndex = (query.getPage() - 1) * query.getSize();
        int toIndex = Math.min(fromIndex + query.getSize(), recommendedCourseIds.size());
        
        if (fromIndex >= recommendedCourseIds.size()) {
            return new PageResult<>(Collections.emptyList(), (long) recommendedCourseIds.size(), 
                    query.getPage().longValue(), query.getSize().longValue(), 
                    (long) ((recommendedCourseIds.size() + query.getSize() - 1) / query.getSize()));
        }
        
        List<Long> pageCourseIds = recommendedCourseIds.subList(fromIndex, toIndex);
        List<RecommendedCourseVO> courses = buildRecommendedCourseVOs(pageCourseIds, userId);
        
        long totalPages = (recommendedCourseIds.size() + query.getSize() - 1) / query.getSize();
        return new PageResult<>(courses, (long) recommendedCourseIds.size(), 
                query.getPage().longValue(), query.getSize().longValue(), totalPages);
    }

    @Override
    public PageResult<ContinueCourseVO> getContinueCourses(LearnerPageQuery query) {
        Long userId = SecurityUtils.currentUserId();
        validateLearner(userId);
        
        // 查询学习中状态的课程进度
        LambdaQueryWrapper<UserCourseProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCourseProgress::getUserId, userId)
               .eq(UserCourseProgress::getStatus, 1) // LEARNING
               .orderByDesc(UserCourseProgress::getUpdatedAt);
        
        Page<UserCourseProgress> page = new Page<>(query.getPage(), query.getSize());
        Page<UserCourseProgress> progressPage = userCourseProgressMapper.selectPage(page, wrapper);
        
        List<ContinueCourseVO> courses = progressPage.getRecords().stream()
                .map(progress -> buildContinueCourseVO(progress))
                .filter(Objects::nonNull) // 过滤掉不可见的课程
                .collect(Collectors.toList());
        
        return new PageResult<>(courses, progressPage.getTotal(), 
                progressPage.getCurrent(), progressPage.getSize(), progressPage.getPages());
    }

    @Override
    public PageResult<LearningRecordVO> getRecentRecords(LearnerPageQuery query) {
        Long userId = SecurityUtils.currentUserId();
        validateLearner(userId);
        
        // 从三张进度表派生学习记录
        List<LearningRecordVO> allRecords = deriveLearningRecords(userId);
        
        // 按时间倒序排序
        allRecords.sort((r1, r2) -> r2.getOccurredAt().compareTo(r1.getOccurredAt()));
        
        // 分页
        int fromIndex = (query.getPage() - 1) * query.getSize();
        int toIndex = Math.min(fromIndex + query.getSize(), allRecords.size());
        
        if (fromIndex >= allRecords.size()) {
            return new PageResult<>(Collections.emptyList(), (long) allRecords.size(), 
                    query.getPage().longValue(), query.getSize().longValue(), 
                    (long) ((allRecords.size() + query.getSize() - 1) / query.getSize()));
        }
        
        List<LearningRecordVO> pageRecords = allRecords.subList(fromIndex, toIndex);
        long totalPages = (allRecords.size() + query.getSize() - 1) / query.getSize();
        
        return new PageResult<>(pageRecords, (long) allRecords.size(), 
                query.getPage().longValue(), query.getSize().longValue(), totalPages);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 验证学员状态
     */
    private User validateLearner(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.LEARNER_NOT_FOUND);
        }
        if (user.getRoleType() != 1 || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (user.getDeptId() == null) {
            throw new BusinessException(ErrorCode.LEARNER_DEPT_NOT_BINDIED);
        }
        return user;
    }

    /**
     * 获取学员可见的课程ID列表
     * 条件：course.status=1, course.deletedAt IS NULL, course_department.department_id=学员部门
     */
    private List<Long> getVisibleCourseIds(Long deptId) {
        // 查询该部门可学习的课程ID
        LambdaQueryWrapper<CourseDepartment> cdWrapper = new LambdaQueryWrapper<>();
        cdWrapper.eq(CourseDepartment::getDepartmentId, deptId);
        List<CourseDepartment> courseDepartments = courseDepartmentMapper.selectList(cdWrapper);
        
        if (courseDepartments.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Long> courseIds = courseDepartments.stream()
                .map(CourseDepartment::getCourseId)
                .collect(Collectors.toList());
        
        // 过滤出已发布且未删除的课程
        LambdaQueryWrapper<Course> courseWrapper = new LambdaQueryWrapper<>();
        courseWrapper.in(Course::getId, courseIds)
                     .eq(Course::getStatus, 1) // PUBLISHED
                     .isNull(Course::getDeletedAt);
        
        return courseMapper.selectList(courseWrapper).stream()
                .map(Course::getId)
                .collect(Collectors.toList());
    }

    /**
     * 构建课程状态统计
     */
    private CourseStatsVO buildCourseStats(List<Long> visibleCourseIds, Long userId) {
        CourseStatsVO stats = new CourseStatsVO();
        
        // 总数
        stats.setAllCount(visibleCourseIds.size());
        
        // 必修/选修数量
        LambdaQueryWrapper<CourseDepartment> cdWrapper = new LambdaQueryWrapper<>();
        cdWrapper.in(CourseDepartment::getCourseId, visibleCourseIds);
        List<CourseDepartment> courseDepts = courseDepartmentMapper.selectList(cdWrapper);
        
        Map<Long, Integer> courseTypeMap = courseDepts.stream()
                .collect(Collectors.toMap(CourseDepartment::getCourseId, CourseDepartment::getRequired, (v1, v2) -> v1));
        
        long requiredCount = courseTypeMap.values().stream().filter(v -> v == 1).count();
        long optionalCount = courseTypeMap.values().stream().filter(v -> v == 0).count();
        stats.setRequiredCount((int) requiredCount);
        stats.setOptionalCount((int) optionalCount);
        
        // 学习状态统计
        LambdaQueryWrapper<UserCourseProgress> progressWrapper = new LambdaQueryWrapper<>();
        progressWrapper.eq(UserCourseProgress::getUserId, userId)
                       .in(UserCourseProgress::getCourseId, visibleCourseIds);
        List<UserCourseProgress> progresses = userCourseProgressMapper.selectList(progressWrapper);
        
        Map<Long, Integer> statusMap = progresses.stream()
                .collect(Collectors.toMap(UserCourseProgress::getCourseId, UserCourseProgress::getStatus, (v1, v2) -> v1));
        
        long completedCount = statusMap.values().stream().filter(s -> s == 2).count();
        long learningCount = statusMap.values().stream().filter(s -> s == 1).count();
        long notStartedCount = visibleCourseIds.size() - completedCount - learningCount;
        
        stats.setCompletedCount((int) completedCount);
        stats.setLearningCount((int) learningCount);
        stats.setNotStartedCount((int) notStartedCount);
        
        // 推荐课程数量（未完成的可学习课程）
        List<Long> recommendedIds = getRecommendedCourseIds(visibleCourseIds, userId);
        stats.setRecommendedCount(recommendedIds.size());
        
        return stats;
    }

    /**
     * 获取推荐课程ID列表（排序：必修优先、未开始优先、更新时间倒序）
     */
    private List<Long> getRecommendedCourseIds(List<Long> visibleCourseIds, Long userId) {
        // 获取所有课程的进度状态
        LambdaQueryWrapper<UserCourseProgress> progressWrapper = new LambdaQueryWrapper<>();
        progressWrapper.eq(UserCourseProgress::getUserId, userId)
                       .in(UserCourseProgress::getCourseId, visibleCourseIds);
        List<UserCourseProgress> progresses = userCourseProgressMapper.selectList(progressWrapper);
        
        Map<Long, UserCourseProgress> progressMap = progresses.stream()
                .collect(Collectors.toMap(UserCourseProgress::getCourseId, p -> p, (p1, p2) -> p1));
        
        // 获取课程部门关系（判断必修/选修）
        LambdaQueryWrapper<CourseDepartment> cdWrapper = new LambdaQueryWrapper<>();
        cdWrapper.in(CourseDepartment::getCourseId, visibleCourseIds);
        List<CourseDepartment> courseDepts = courseDepartmentMapper.selectList(cdWrapper);
        
        Map<Long, Integer> courseTypeMap = courseDepts.stream()
                .collect(Collectors.toMap(CourseDepartment::getCourseId, CourseDepartment::getRequired, (v1, v2) -> v1));
        
        // 获取课程信息用于排序
        LambdaQueryWrapper<Course> courseWrapper = new LambdaQueryWrapper<>();
        courseWrapper.in(Course::getId, visibleCourseIds);
        List<Course> courses = courseMapper.selectList(courseWrapper);
        
        Map<Long, Course> courseMap = courses.stream()
                .collect(Collectors.toMap(Course::getId, c -> c));
        
        // 过滤并排序
        return visibleCourseIds.stream()
                .filter(courseId -> {
                    UserCourseProgress progress = progressMap.get(courseId);
                    // 排除已完成的课程
                    return progress == null || progress.getStatus() != 2;
                })
                .sorted((id1, id2) -> {
                    // 1. 必修优先
                    Integer type1 = courseTypeMap.getOrDefault(id1, 0);
                    Integer type2 = courseTypeMap.getOrDefault(id2, 0);
                    if (!type1.equals(type2)) {
                        return type2.compareTo(type1); // 1(必修)排在前面
                    }
                    
                    // 2. 未开始优先
                    UserCourseProgress p1 = progressMap.get(id1);
                    UserCourseProgress p2 = progressMap.get(id2);
                    int status1 = p1 == null ? 0 : p1.getStatus();
                    int status2 = p2 == null ? 0 : p2.getStatus();
                    if (status1 != status2) {
                        return Integer.compare(status1, status2); // 0(未开始)排在前面
                    }
                    
                    // 3. 更新时间倒序
                    Course c1 = courseMap.get(id1);
                    Course c2 = courseMap.get(id2);
                    LocalDateTime t1 = c1 != null ? c1.getUpdatedAt() : LocalDateTime.MIN;
                    LocalDateTime t2 = c2 != null ? c2.getUpdatedAt() : LocalDateTime.MIN;
                    int timeCompare = t2.compareTo(t1);
                    if (timeCompare != 0) {
                        return timeCompare;
                    }
                    
                    // 4. ID倒序
                    return Long.compare(id2, id1);
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取首页推荐课程（限制数量）
     */
    private List<RecommendedCourseVO> getRecommendedCoursesForHome(List<Long> visibleCourseIds, Long userId, int limit) {
        List<Long> recommendedIds = getRecommendedCourseIds(visibleCourseIds, userId);
        List<Long> limitedIds = recommendedIds.stream().limit(limit).collect(Collectors.toList());
        return buildRecommendedCourseVOs(limitedIds, userId);
    }

    /**
     * 构建推荐课程VO列表
     */
    private List<RecommendedCourseVO> buildRecommendedCourseVOs(List<Long> courseIds, Long userId) {
        if (courseIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 批量获取课程信息
        LambdaQueryWrapper<Course> courseWrapper = new LambdaQueryWrapper<>();
        courseWrapper.in(Course::getId, courseIds);
        List<Course> courses = courseMapper.selectList(courseWrapper);
        Map<Long, Course> courseMap = courses.stream()
                .collect(Collectors.toMap(Course::getId, c -> c));
        
        // 批量获取类别信息
        Set<Long> categoryIds = courses.stream()
                .map(Course::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        final Map<Long, String> categoryNameMap;
        if (!categoryIds.isEmpty()) {
            LambdaQueryWrapper<Category> catWrapper = new LambdaQueryWrapper<>();
            catWrapper.in(Category::getId, categoryIds);
            categoryNameMap = categoryMapper.selectList(catWrapper).stream()
                    .collect(Collectors.toMap(Category::getId, Category::getName));
        } else {
            categoryNameMap = Collections.emptyMap();
        }
        
        // 批量获取课程部门关系
        LambdaQueryWrapper<CourseDepartment> cdWrapper = new LambdaQueryWrapper<>();
        cdWrapper.in(CourseDepartment::getCourseId, courseIds);
        Map<Long, Integer> courseTypeMap = courseDepartmentMapper.selectList(cdWrapper).stream()
                .collect(Collectors.toMap(CourseDepartment::getCourseId, CourseDepartment::getRequired, (v1, v2) -> v1));
        
        // 批量获取进度
        LambdaQueryWrapper<UserCourseProgress> progressWrapper = new LambdaQueryWrapper<>();
        progressWrapper.eq(UserCourseProgress::getUserId, userId)
                       .in(UserCourseProgress::getCourseId, courseIds);
        Map<Long, UserCourseProgress> progressMap = userCourseProgressMapper.selectList(progressWrapper).stream()
                .collect(Collectors.toMap(UserCourseProgress::getCourseId, p -> p, (p1, p2) -> p1));
        
        // 构建VO
        return courseIds.stream()
                .map(courseId -> {
                    Course course = courseMap.get(courseId);
                    if (course == null) return null;
                    
                    RecommendedCourseVO vo = new RecommendedCourseVO();
                    vo.setCourseId(courseId);
                    vo.setTitle(course.getTitle());
                    vo.setCoverUrl(course.getCoverUrl());
                    // TODO: 讲师姓名需要从其他地方获取，暂时设为null
                    vo.setInstructorName(null);
                    vo.setCategoryName(categoryNameMap.get(course.getCategoryId()));
                    
                    Integer required = courseTypeMap.get(courseId);
                    vo.setCourseType(required != null && required == 1 ? "REQUIRED" : "OPTIONAL");
                    
                    UserCourseProgress progress = progressMap.get(courseId);
                    if (progress == null) {
                        vo.setLearningStatus("NOT_STARTED");
                        vo.setProgressPercent(BigDecimal.ZERO);
                        vo.setButtonText("开始学习");
                        // 获取第一个课程点作为lastPointId
                        vo.setLastPointId(getFirstCoursePointId(courseId));
                    } else if (progress.getStatus() == 1) {
                        vo.setLearningStatus("LEARNING");
                        vo.setProgressPercent(progress.getProgressPercent() != null ? progress.getProgressPercent() : BigDecimal.ZERO);
                        vo.setButtonText("继续学习");
                        vo.setLastPointId(progress.getLastPointId() != null ? progress.getLastPointId() : getFirstCoursePointId(courseId));
                    } else {
                        vo.setLearningStatus("COMPLETED");
                        vo.setProgressPercent(progress.getProgressPercent() != null ? progress.getProgressPercent() : BigDecimal.valueOf(100));
                        vo.setButtonText("复习课程");
                        vo.setLastPointId(progress.getLastPointId());
                    }
                    
                    return vo;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 获取首页继续学习课程（限制数量）
     */
    private List<ContinueCourseVO> getContinueCoursesForHome(Long userId, int limit) {
        LambdaQueryWrapper<UserCourseProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCourseProgress::getUserId, userId)
               .eq(UserCourseProgress::getStatus, 1) // LEARNING
               .orderByDesc(UserCourseProgress::getUpdatedAt)
               .last("LIMIT " + limit);
        
        List<UserCourseProgress> progresses = userCourseProgressMapper.selectList(wrapper);
        
        return progresses.stream()
                .map(this::buildContinueCourseVO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 构建继续学习课程VO
     */
    private ContinueCourseVO buildContinueCourseVO(UserCourseProgress progress) {
        Course course = courseMapper.selectById(progress.getCourseId());
        if (course == null || course.getStatus() != 1 || course.getDeletedAt() != null) {
            return null; // 课程不可见
        }
        
        ContinueCourseVO vo = new ContinueCourseVO();
        vo.setCourseId(progress.getCourseId());
        vo.setTitle(course.getTitle());
        vo.setCoverUrl(course.getCoverUrl());
        vo.setInstructorName(null); // TODO
        
        // 获取类别名称
        if (course.getCategoryId() != null) {
            Category category = categoryMapper.selectById(course.getCategoryId());
            vo.setCategoryName(category != null ? category.getName() : null);
        }
        
        // 获取课程类型
        LambdaQueryWrapper<CourseDepartment> cdWrapper = new LambdaQueryWrapper<>();
        cdWrapper.eq(CourseDepartment::getCourseId, progress.getCourseId());
        CourseDepartment cd = courseDepartmentMapper.selectOne(cdWrapper);
        vo.setCourseType(cd != null && cd.getRequired() == 1 ? "REQUIRED" : "OPTIONAL");
        
        vo.setLearningStatus("LEARNING");
        vo.setProgressPercent(progress.getProgressPercent() != null ? progress.getProgressPercent() : BigDecimal.ZERO);
        vo.setButtonText("继续学习");
        
        // 处理lastPointId
        Long lastPointId = progress.getLastPointId();
        if (lastPointId != null) {
            // 验证课程点是否有效
            CoursePoint point = coursePointMapper.selectById(lastPointId);
            if (point == null || point.getDeletedAt() != null || point.getStatus() != 1) {
                lastPointId = getFirstCoursePointId(progress.getCourseId());
            }
        } else {
            lastPointId = getFirstCoursePointId(progress.getCourseId());
        }
        vo.setLastPointId(lastPointId);
        
        // 获取课程点名称
        if (lastPointId != null) {
            CoursePoint point = coursePointMapper.selectById(lastPointId);
            vo.setLastPointTitle(point != null ? point.getTitle() : null);
        }
        
        vo.setLastLearnedAt(progress.getUpdatedAt());
        
        return vo;
    }

    /**
     * 获取课程的第一个有效课程点ID
     */
    private Long getFirstCoursePointId(Long courseId) {
        LambdaQueryWrapper<CoursePoint> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CoursePoint::getCourseId, courseId)
               .eq(CoursePoint::getStatus, 1) // 启用
               .isNull(CoursePoint::getDeletedAt)
               .orderByAsc(CoursePoint::getSort)
               .last("LIMIT 1");
        
        CoursePoint point = coursePointMapper.selectOne(wrapper);
        return point != null ? point.getId() : null;
    }

    /**
     * 构建进度概览
     */
    private ProgressOverviewVO buildProgressOverview(List<Long> visibleCourseIds, Long userId) {
        ProgressOverviewVO overview = new ProgressOverviewVO();
        overview.setTotalCount(visibleCourseIds.size());
        
        if (visibleCourseIds.isEmpty()) {
            overview.setCompletedCount(0);
            overview.setLearningCount(0);
            overview.setNotStartedCount(0);
            overview.setOverallProgressPercent(BigDecimal.ZERO);
            return overview;
        }
        
        LambdaQueryWrapper<UserCourseProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCourseProgress::getUserId, userId)
               .in(UserCourseProgress::getCourseId, visibleCourseIds);
        List<UserCourseProgress> progresses = userCourseProgressMapper.selectList(wrapper);
        
        Map<Long, Integer> statusMap = progresses.stream()
                .collect(Collectors.toMap(UserCourseProgress::getCourseId, UserCourseProgress::getStatus, (v1, v2) -> v1));
        
        int completedCount = (int) statusMap.values().stream().filter(s -> s == 2).count();
        int learningCount = (int) statusMap.values().stream().filter(s -> s == 1).count();
        int notStartedCount = visibleCourseIds.size() - completedCount - learningCount;
        
        overview.setCompletedCount(completedCount);
        overview.setLearningCount(learningCount);
        overview.setNotStartedCount(notStartedCount);
        
        // 计算总体完成率
        BigDecimal percent = BigDecimal.valueOf(completedCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(visibleCourseIds.size()), 2, RoundingMode.HALF_UP);
        overview.setOverallProgressPercent(percent);
        
        return overview;
    }

    /**
     * 获取首页最近学习记录（限制数量）
     */
    private List<LearningRecordVO> getRecentRecordsForHome(Long userId, int limit) {
        List<LearningRecordVO> allRecords = deriveLearningRecords(userId);
        allRecords.sort((r1, r2) -> r2.getOccurredAt().compareTo(r1.getOccurredAt()));
        return allRecords.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * 从三张进度表派生学习记录
     */
    private List<LearningRecordVO> deriveLearningRecords(Long userId) {
        List<LearningRecordVO> records = new ArrayList<>();
        
        // 1. 从user_course_progress派生课程级记录
        LambdaQueryWrapper<UserCourseProgress> courseWrapper = new LambdaQueryWrapper<>();
        courseWrapper.eq(UserCourseProgress::getUserId, userId);
        List<UserCourseProgress> courseProgresses = userCourseProgressMapper.selectList(courseWrapper);
        
        for (UserCourseProgress progress : courseProgresses) {
            LearningRecordVO record = new LearningRecordVO();
            record.setCourseId(progress.getCourseId());
            
            Course course = courseMapper.selectById(progress.getCourseId());
            record.setCourseTitle(course != null ? course.getTitle() : "未知课程");
            
            if (progress.getStatus() == 2) {
                // 已完成
                record.setRecordType("COURSE_COMPLETED");
                record.setTitle("完成了《" + record.getCourseTitle() + "》");
                record.setOccurredAt(progress.getCompletedAt() != null ? progress.getCompletedAt() : progress.getUpdatedAt());
            } else if (progress.getStatus() == 1) {
                // 学习中
                record.setRecordType("COURSE_LEARNING");
                record.setTitle("继续学习了《" + record.getCourseTitle() + "》");
                record.setOccurredAt(progress.getUpdatedAt());
            } else {
                continue; // 未开始的不显示
            }
            
            record.setCoursePointId(progress.getLastPointId());
            records.add(record);
        }
        
        // 2. 从user_course_point_progress派生课程点级记录
        LambdaQueryWrapper<UserCoursePointProgress> pointWrapper = new LambdaQueryWrapper<>();
        pointWrapper.eq(UserCoursePointProgress::getUserId, userId);
        List<UserCoursePointProgress> pointProgresses = userCoursePointProgressMapper.selectList(pointWrapper);
        
        for (UserCoursePointProgress progress : pointProgresses) {
            if (progress.getStatus() == 2) {
                // 只记录完成的课程点
                LearningRecordVO record = new LearningRecordVO();
                record.setRecordType("POINT_COMPLETED");
                record.setCourseId(progress.getCourseId());
                
                Course course = courseMapper.selectById(progress.getCourseId());
                record.setCourseTitle(course != null ? course.getTitle() : "未知课程");
                
                record.setCoursePointId(progress.getCoursePointId());
                CoursePoint point = coursePointMapper.selectById(progress.getCoursePointId());
                record.setCoursePointTitle(point != null ? point.getTitle() : "未知课程点");
                
                record.setTitle("完成了课程点《" + record.getCoursePointTitle() + "》");
                record.setOccurredAt(progress.getCompletedAt() != null ? progress.getCompletedAt() : progress.getUpdatedAt());
                
                records.add(record);
            }
        }
        
        // 3. 从user_course_resource_progress派生课件级记录
        LambdaQueryWrapper<UserCourseResourceProgress> resourceWrapper = new LambdaQueryWrapper<>();
        resourceWrapper.eq(UserCourseResourceProgress::getUserId, userId);
        List<UserCourseResourceProgress> resourceProgresses = userCourseResourceProgressMapper.selectList(resourceWrapper);
        
        for (UserCourseResourceProgress progress : resourceProgresses) {
            if (progress.getStatus() == 2) {
                // 只记录完成的课件
                LearningRecordVO record = new LearningRecordVO();
                record.setRecordType("RESOURCE_COMPLETED");
                record.setCourseId(progress.getCourseId());
                
                Course course = courseMapper.selectById(progress.getCourseId());
                record.setCourseTitle(course != null ? course.getTitle() : "未知课程");
                
                record.setCoursePointId(progress.getCoursePointId());
                CoursePoint point = coursePointMapper.selectById(progress.getCoursePointId());
                record.setCoursePointTitle(point != null ? point.getTitle() : null);
                
                // 根据resourceType获取课件名称
                String resourceTypeName = getResourceTypeName(progress.getResourceType());
                record.setResourceType(resourceTypeName);
                record.setResourceId(progress.getResourceId());
                // TODO: 根据resourceType和resourceId获取课件标题
                record.setResourceTitle(null);
                
                record.setTitle("完成了《" + record.getCourseTitle() + "》的" + resourceTypeName);
                record.setOccurredAt(progress.getCompletedAt() != null ? progress.getCompletedAt() : progress.getUpdatedAt());
                
                records.add(record);
            }
        }
        
        return records;
    }

    /**
     * 获取资源类型名称
     */
    private String getResourceTypeName(Integer resourceType) {
        if (resourceType == null) return null;
        return switch (resourceType) {
            case 1 -> "ARTICLE";
            case 2 -> "VIDEO";
            case 3 -> "PPT";
            default -> null;
        };
    }

    /**
     * 构建学习日历
     */
    private CalendarVO buildCalendar(Long userId) {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        
        CalendarVO calendar = new CalendarVO();
        calendar.setYear(currentMonth.getYear());
        calendar.setMonth(currentMonth.getMonthValue());
        calendar.setToday(today);
        
        // 获取该月所有有学习行为的日期
        Set<LocalDate> learningDates = getLearningDatesInMonth(userId, currentMonth);
        
        // 生成日历天数
        List<CalendarDayVO> days = new ArrayList<>();
        LocalDate firstDay = currentMonth.atDay(1);
        LocalDate lastDay = currentMonth.atEndOfMonth();
        
        for (LocalDate date = firstDay; !date.isAfter(lastDay); date = date.plusDays(1)) {
            CalendarDayVO day = new CalendarDayVO();
            day.setDate(date);
            day.setDayOfMonth(date.getDayOfMonth());
            day.setCurrentMonth(true);
            day.setToday(date.equals(today));
            day.setHasLearning(learningDates.contains(date));
            days.add(day);
        }
        
        calendar.setDays(days);
        return calendar;
    }

    /**
     * 获取某月所有有学习行为的日期
     */
    private Set<LocalDate> getLearningDatesInMonth(Long userId, YearMonth month) {
        Set<LocalDate> dates = new HashSet<>();
        
        LocalDateTime startOfMonth = month.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = month.atEndOfMonth().atTime(23, 59, 59);
        
        // 1. user_course_progress
        LambdaQueryWrapper<UserCourseProgress> courseWrapper = new LambdaQueryWrapper<>();
        courseWrapper.eq(UserCourseProgress::getUserId, userId)
                     .and(w -> w.between(UserCourseProgress::getUpdatedAt, startOfMonth, endOfMonth)
                                .or()
                                .between(UserCourseProgress::getCompletedAt, startOfMonth, endOfMonth));
        userCourseProgressMapper.selectList(courseWrapper).forEach(p -> {
            if (p.getUpdatedAt() != null) dates.add(p.getUpdatedAt().toLocalDate());
            if (p.getCompletedAt() != null) dates.add(p.getCompletedAt().toLocalDate());
        });
        
        // 2. user_course_point_progress
        LambdaQueryWrapper<UserCoursePointProgress> pointWrapper = new LambdaQueryWrapper<>();
        pointWrapper.eq(UserCoursePointProgress::getUserId, userId)
                    .and(w -> w.between(UserCoursePointProgress::getUpdatedAt, startOfMonth, endOfMonth)
                               .or()
                               .between(UserCoursePointProgress::getCompletedAt, startOfMonth, endOfMonth));
        userCoursePointProgressMapper.selectList(pointWrapper).forEach(p -> {
            if (p.getUpdatedAt() != null) dates.add(p.getUpdatedAt().toLocalDate());
            if (p.getCompletedAt() != null) dates.add(p.getCompletedAt().toLocalDate());
        });
        
        // 3. user_course_resource_progress
        LambdaQueryWrapper<UserCourseResourceProgress> resourceWrapper = new LambdaQueryWrapper<>();
        resourceWrapper.eq(UserCourseResourceProgress::getUserId, userId)
                       .and(w -> w.between(UserCourseResourceProgress::getUpdatedAt, startOfMonth, endOfMonth)
                                  .or()
                                  .between(UserCourseResourceProgress::getCompletedAt, startOfMonth, endOfMonth));
        userCourseResourceProgressMapper.selectList(resourceWrapper).forEach(p -> {
            if (p.getUpdatedAt() != null) dates.add(p.getUpdatedAt().toLocalDate());
            if (p.getCompletedAt() != null) dates.add(p.getCompletedAt().toLocalDate());
        });
        
        return dates;
    }

    /**
     * 构建空的首页数据
     */
    private HomePageVO buildEmptyHomePage() {
        HomePageVO vo = new HomePageVO();
        
        CourseStatsVO stats = new CourseStatsVO();
        stats.setAllCount(0);
        stats.setRecommendedCount(0);
        stats.setRequiredCount(0);
        stats.setOptionalCount(0);
        stats.setCompletedCount(0);
        stats.setLearningCount(0);
        stats.setNotStartedCount(0);
        vo.setCourseStats(stats);
        
        vo.setRecommendedCourses(Collections.emptyList());
        vo.setContinueCourses(Collections.emptyList());
        
        ProgressOverviewVO overview = new ProgressOverviewVO();
        overview.setTotalCount(0);
        overview.setCompletedCount(0);
        overview.setLearningCount(0);
        overview.setNotStartedCount(0);
        overview.setOverallProgressPercent(BigDecimal.ZERO);
        vo.setProgressOverview(overview);
        
        vo.setRecentRecords(Collections.emptyList());
        vo.setCalendar(buildCalendar(SecurityUtils.currentUserId()));
        
        return vo;
    }
}
