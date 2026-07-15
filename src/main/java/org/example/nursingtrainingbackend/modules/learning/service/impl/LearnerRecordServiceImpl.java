package org.example.nursingtrainingbackend.modules.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.modules.course.entity.Course;
import org.example.nursingtrainingbackend.modules.course.entity.CourseDepartment;
import org.example.nursingtrainingbackend.modules.course.entity.CoursePoint;
import org.example.nursingtrainingbackend.modules.course.mapper.CourseDepartmentMapper;
import org.example.nursingtrainingbackend.modules.course.mapper.CourseMapper;
import org.example.nursingtrainingbackend.modules.course.mapper.CoursePointMapper;
import org.example.nursingtrainingbackend.modules.learning.dto.RecordQuery;
import org.example.nursingtrainingbackend.modules.learning.entity.UserCoursePointProgress;
import org.example.nursingtrainingbackend.modules.learning.entity.UserCourseProgress;
import org.example.nursingtrainingbackend.modules.learning.entity.UserCourseResourceProgress;
import org.example.nursingtrainingbackend.modules.learning.entity.UserLearningRecord;
import org.example.nursingtrainingbackend.modules.learning.mapper.UserCoursePointProgressMapper;
import org.example.nursingtrainingbackend.modules.learning.mapper.UserCourseProgressMapper;
import org.example.nursingtrainingbackend.modules.learning.mapper.UserCourseResourceProgressMapper;
import org.example.nursingtrainingbackend.modules.learning.mapper.UserLearningRecordMapper;
import org.example.nursingtrainingbackend.modules.learning.service.LearnerRecordService;
import org.example.nursingtrainingbackend.modules.learning.vo.*;
import org.example.nursingtrainingbackend.modules.user.entity.User;
import org.example.nursingtrainingbackend.modules.user.mapper.UserMapper;
import org.example.nursingtrainingbackend.security.SecurityUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.example.nursingtrainingbackend.modules.learning.dto.TopCoursesQuery;
import org.example.nursingtrainingbackend.modules.courseware.article.entity.Article;
import org.example.nursingtrainingbackend.modules.courseware.article.mapper.ArticleMapper;
import org.example.nursingtrainingbackend.modules.courseware.video.entity.Video;
import org.example.nursingtrainingbackend.modules.courseware.video.mapper.VideoMapper;
import org.example.nursingtrainingbackend.modules.courseware.ppt.entity.Ppt;
import org.example.nursingtrainingbackend.modules.courseware.ppt.mapper.PptMapper;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearnerRecordServiceImpl implements LearnerRecordService {
    private static final String LEARNER_RECORD_STATS_CACHE_PREFIX = "nursing:learner:stats:";
    private static final String LEARNER_COURSE_RANK_CACHE_PREFIX = "nursing:rank:course:";
    private static final long LEARNER_RECORD_STATS_CACHE_TTL_SECONDS = 60; // 1分钟缓存


    private static final Set<String> VALID_RANGES = Set.of("TODAY", "LAST_7_DAYS", "LAST_30_DAYS");
    private static final Set<String> VALID_RECORD_TYPES = Set.of(
            "START_COURSE", "CONTINUE_LEARNING", "COMPLETE_RESOURCE",
            "COMPLETE_POINT", "COMPLETE_COURSE", "REVIEW_COURSE"
    );
    private static final Set<String> VALID_RESOURCE_TYPES = Set.of("ARTICLE", "VIDEO", "PPT");

    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final CourseDepartmentMapper courseDepartmentMapper;
    private final CoursePointMapper coursePointMapper;
    private final UserCourseProgressMapper userCourseProgressMapper;
    private final UserCoursePointProgressMapper userCoursePointProgressMapper;
    private final UserCourseResourceProgressMapper userCourseResourceProgressMapper;
    private final UserLearningRecordMapper userLearningRecordMapper;
    private final ArticleMapper articleMapper;
    private final VideoMapper videoMapper;
    private final PptMapper pptMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;



    // ==================== 1. 分页查询学习记录 ====================

    @Override
    public PageResult<LearningRecordVO> getRecords(RecordQuery query) {
        Long userId = SecurityUtils.currentUserId();
        validateLearner(userId);
        validateQueryParams(query);

        LambdaQueryWrapper<UserLearningRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserLearningRecord::getUserId, userId);

        applyRangeFilter(wrapper, query.getRange());
        applyActionTypeFilter(wrapper, query.getActionType());
        applyResourceTypeFilter(wrapper, query.getResourceType());

        wrapper.orderByDesc(UserLearningRecord::getCreatedAt)
                .orderByDesc(UserLearningRecord::getId);

        List<UserLearningRecord> dbRecords;
        try {
            dbRecords = userLearningRecordMapper.selectList(wrapper);
        } catch (Exception e) {
            log.error("学习记录查询失败, userId={}", userId, e);
            throw new BusinessException(ErrorCode.LEARNER_RECORD_QUERY_FAILED);
        }

        List<LearningRecordVO> voList = dbRecords.stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        // 去重：同一课程+同一动作类型+同一课件+同一时间（精确到秒）视为重复
        Set<String> seen = new java.util.LinkedHashSet<>();
        List<LearningRecordVO> deduped = new ArrayList<>();
        for (LearningRecordVO vo : voList) {
            String key = vo.getCourseId() + ":" + vo.getActionType() + ":" + vo.getResourceId() + ":" + vo.getOccurredAt();
            if (seen.add(key)) {
                deduped.add(vo);
            }
        }
        voList = deduped;

        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String kw = query.getKeyword().toLowerCase();
            voList = voList.stream()
                    .filter(vo -> matchKeyword(vo, kw))
                    .collect(Collectors.toList());
        }

        long total = voList.size();
        int fromIndex = (query.getPage() - 1) * query.getSize();
        int toIndex = Math.min(fromIndex + query.getSize(), voList.size());
        List<LearningRecordVO> pageRecords = (fromIndex >= voList.size())
                ? Collections.emptyList() : voList.subList(fromIndex, toIndex);

        long totalPages = (total + query.getSize() - 1) / query.getSize();
        return new PageResult<>(pageRecords, total, query.getPage().longValue(), query.getSize().longValue(), totalPages);
    }

    // ==================== 2. 学习进度概览 ====================

    @Override
    public RecordOverviewVO getOverview(String range) {

        Long userId = SecurityUtils.currentUserId();
        validateLearner(userId);
        String effectiveRange = (range != null && !range.isBlank()) ? range.toUpperCase() : "TODAY";
        validateRange(effectiveRange);
        String cacheKey = LEARNER_RECORD_STATS_CACHE_PREFIX + "overview:" + userId + ":" + effectiveRange;
        // 尝试从缓存获取
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null && !cachedJson.isBlank()) {
                return objectMapper.readValue(cachedJson, RecordOverviewVO.class);
            }
        } catch (Exception e) {
            log.warn("读取学员记录概览缓存失败, userId={}, range={}", userId, range, e);
        }


        try {
            LocalDateTime rangeStart = resolveRangeStart(effectiveRange);

            LambdaQueryWrapper<UserLearningRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserLearningRecord::getUserId, userId);
            if (rangeStart != null) {
                wrapper.ge(UserLearningRecord::getCreatedAt, rangeStart);
            }
            List<UserLearningRecord> records = userLearningRecordMapper.selectList(wrapper);

            RecordOverviewVO overview = new RecordOverviewVO();
            overview.setSummaryCards(buildSummaryCards(records, userId, rangeStart));
            overview.setResourceDistribution(buildResourceDistributionFromProgress(userId, rangeStart));
            overview.setFrequencyTrend(buildFrequencyTrend(records, effectiveRange));
            overview.setTopCourses(buildTopCourses(records, 8));

            // 写入缓存
            try {
                String json = objectMapper.writeValueAsString(overview);
                redisTemplate.opsForValue().set(cacheKey, json, LEARNER_RECORD_STATS_CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("写入学员记录概览缓存失败, userId={}, range={}", userId, effectiveRange, e);
            }

            return overview;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("学习记录概览查询失败, userId={}", userId, e);
            throw new BusinessException(ErrorCode.LEARNER_RECORD_QUERY_FAILED);
        }
    }

    // ==================== 3. 学习日历 ====================

    @Override
    public CalendarVO getCalendar(Integer year, Integer month) {
        Long userId = SecurityUtils.currentUserId();
        validateLearner(userId);

        LocalDate today = LocalDate.now();
        int y = (year != null && year > 2000) ? year : today.getYear();
        int m = (month != null && month >= 1 && month <= 12) ? month : today.getMonthValue();
        YearMonth yearMonth = YearMonth.of(y, m);

        CalendarVO calendar = new CalendarVO();
        calendar.setYear(yearMonth.getYear());
        calendar.setMonth(yearMonth.getMonthValue());
        calendar.setToday(today);

        Set<LocalDate> learningDates = getLearningDatesFromRecord(userId, yearMonth);

        List<CalendarDayVO> days = new ArrayList<>();
        LocalDate firstDay = yearMonth.atDay(1);
        LocalDate lastDay = yearMonth.atEndOfMonth();
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

    // ==================== 4. 单条记录详情 ====================

    @Override
    public List<LearningRecordVO> getRecordDetail(String id) {
        Long userId = SecurityUtils.currentUserId();
        validateLearner(userId);

        UserLearningRecord record = userLearningRecordMapper.selectById(parseRecordId(id));
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.LEARNER_RECORD_NOT_FOUND);
        }

        LambdaQueryWrapper<UserLearningRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserLearningRecord::getUserId, userId)
                .eq(UserLearningRecord::getCourseId, record.getCourseId())
                .orderByDesc(UserLearningRecord::getCreatedAt)
                .orderByDesc(UserLearningRecord::getId);

        return userLearningRecordMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    // ==================== 5. 手动标记完成 ====================

    @Override
    @Transactional
    public void markComplete(String id) {
        Long userId = SecurityUtils.currentUserId();
        validateLearner(userId);

        ParsedId parsed = parseId(id);
        if (!"RESOURCE".equals(parsed.type)) {
            throw new BusinessException(ErrorCode.LEARNER_RECORD_TYPE_NOT_SUPPORTED);
        }

        UserCourseResourceProgress resourceProgress = findResourceProgress(userId, parsed);
        if (resourceProgress == null) {
            throw new BusinessException(ErrorCode.LEARNER_RECORD_NOT_FOUND);
        }
        if (resourceProgress.getStatus() == 2) {
            throw new BusinessException(ErrorCode.LEARNER_RECORD_ALREADY_COMPLETED);
        }

        LocalDateTime now = LocalDateTime.now();
        resourceProgress.setStatus(2);
        resourceProgress.setProgressPercent(BigDecimal.valueOf(100));
        resourceProgress.setCompletedAt(now);
        resourceProgress.setUpdatedAt(now);
        userCourseResourceProgressMapper.updateById(resourceProgress);

        if (!hasResourceCompleteRecord(userId, resourceProgress.getCourseId(),
                resourceProgress.getResourceType(), resourceProgress.getResourceId())) {
            insertLearningRecord(userId, resourceProgress.getCourseId(),
                    resourceProgress.getCoursePointId(), 3,
                    resourceProgress.getResourceType(), resourceProgress.getResourceId(),
                    buildResourceCompleteTitle(resourceProgress), now);
        }

        recalculatePointProgress(userId, resourceProgress.getCourseId(),
                resourceProgress.getCoursePointId(), now);
        recalculateCourseProgress(userId, resourceProgress.getCourseId(), now);
    }

    private boolean hasResourceCompleteRecord(Long userId, Long courseId, Integer resourceType, Long resourceId) {
        LambdaQueryWrapper<UserLearningRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserLearningRecord::getUserId, userId)
                .eq(UserLearningRecord::getCourseId, courseId)
                .eq(UserLearningRecord::getActionType, 3)
                .eq(UserLearningRecord::getResourceType, resourceType)
                .eq(UserLearningRecord::getResourceId, resourceId);
        Long count = userLearningRecordMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    // ==================== 6. 重置进度 ====================

    @Override
    @Transactional
    public void resetProgress(String id) {
        Long userId = SecurityUtils.currentUserId();
        validateLearner(userId);

        ParsedId parsed = parseId(id);
        if (!"RESOURCE".equals(parsed.type)) {
            throw new BusinessException(ErrorCode.LEARNER_RECORD_TYPE_NOT_SUPPORTED);
        }

        UserCourseResourceProgress resourceProgress = findResourceProgress(userId, parsed);
        if (resourceProgress == null) {
            throw new BusinessException(ErrorCode.LEARNER_RECORD_NOT_FOUND);
        }

        Long courseId = resourceProgress.getCourseId();
        Long pointId = resourceProgress.getCoursePointId();

        userCourseResourceProgressMapper.deleteById(resourceProgress.getId());

        userLearningRecordMapper.delete(new LambdaQueryWrapper<UserLearningRecord>()
                .eq(UserLearningRecord::getUserId, userId)
                .eq(UserLearningRecord::getCourseId, courseId)
                .eq(UserLearningRecord::getResourceId, resourceProgress.getResourceId())
                .eq(UserLearningRecord::getActionType, 3));

        LocalDateTime now = LocalDateTime.now();
        recalculatePointProgressAfterReset(userId, courseId, pointId, now);
        recalculateCourseProgress(userId, courseId, now);
    }

    // ==================== 7. 学习统计 ====================

    @Override
    public RecordStatsVO getStats(String range) {
        Long userId = SecurityUtils.currentUserId();
        validateLearner(userId);

        try {
            String effectiveRange = (range != null && !range.isBlank()) ? range.toUpperCase() : "TODAY";
            validateRange(effectiveRange);

            LocalDateTime rangeStart = resolveRangeStart(effectiveRange);

            LambdaQueryWrapper<UserLearningRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserLearningRecord::getUserId, userId);
            if (rangeStart != null) {
                wrapper.ge(UserLearningRecord::getCreatedAt, rangeStart);
            }
            List<UserLearningRecord> records = userLearningRecordMapper.selectList(wrapper);

            RecordStatsVO stats = new RecordStatsVO();
            stats.setTotalRecords(records.size());
            stats.setCourseLearningCount((int) records.stream().filter(r -> r.getActionType() == 2).count());
            stats.setCourseCompletedCount((int) records.stream().filter(r -> r.getActionType() == 5).count());
            stats.setPointCompletedCount((int) records.stream().filter(r -> r.getActionType() == 4).count());
            stats.setResourceCompletedCount((int) records.stream().filter(r -> r.getActionType() == 3).count());

            Set<LocalDate> allLearningDates = getLearningDatesFromRecord(userId, null);
            stats.setTotalLearningDays(allLearningDates.size());
            stats.setConsecutiveDays(calculateConsecutiveDays(allLearningDates));

            stats.setResourceDistribution(buildResourceDistributionFromProgress(userId, rangeStart));
            stats.setFrequencyTrend(buildFrequencyTrend(records, effectiveRange));
            stats.setTopCourses(buildTopCourses(records, 5));

            return stats;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("学习记录统计失败, userId={}", userId, e);
            throw new BusinessException(ErrorCode.LEARNER_RECORD_STATS_FAILED);
        }
    }

    @Override
    public List<ResourceDistributionVO> getResourceDistribution(String range) {
        Long userId = SecurityUtils.currentUserId();
        validateLearner(userId);
        String effectiveRange = (range != null && !range.isBlank()) ? range.toUpperCase() : "TODAY";
        validateRange(effectiveRange);

        LambdaQueryWrapper<UserLearningRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserLearningRecord::getUserId, userId)
                .eq(UserLearningRecord::getActionType, 3);
        applyRangeFilter(wrapper, effectiveRange);

        List<UserLearningRecord> records = userLearningRecordMapper.selectList(wrapper);
        return buildResourceDistributionFromProgress(userId, resolveRangeStart(effectiveRange));
    }

    // ==================== 9. 频率趋势 ====================

    @Override
    public FrequencyTrendVO getFrequencyTrend(String range) {
        Long userId = SecurityUtils.currentUserId();
        validateLearner(userId);
        String effectiveRange = (range != null && !range.isBlank()) ? range.toUpperCase() : "TODAY";
        validateRange(effectiveRange);

        LocalDateTime rangeStart = resolveRangeStart(effectiveRange);
        LambdaQueryWrapper<UserLearningRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserLearningRecord::getUserId, userId);
        if (rangeStart != null) {
            wrapper.ge(UserLearningRecord::getCreatedAt, rangeStart);
        }
        List<UserLearningRecord> records = userLearningRecordMapper.selectList(wrapper);

        return buildFrequencyTrend(records, effectiveRange);
    }

    // ==================== 10. 学习最多课程 ====================

    @Override
    public PageResult<TopCourseVO> getTopCourses(TopCoursesQuery query) {
        Long userId = SecurityUtils.currentUserId();
        validateLearner(userId);

        // "学习最多的课程"不限制时间范围，始终展示全量课程排行
        String cacheKey = LEARNER_COURSE_RANK_CACHE_PREFIX + "top:" + userId + ":ALL:" + query.getPage() + ":" + query.getSize();

        LambdaQueryWrapper<UserLearningRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserLearningRecord::getUserId, userId);

        List<UserLearningRecord> records = userLearningRecordMapper.selectList(wrapper);

        Map<Long, List<UserLearningRecord>> byCourse = records.stream()
                .collect(Collectors.groupingBy(UserLearningRecord::getCourseId));

        List<TopCourseVO> allCourses = byCourse.entrySet().stream()
                .map(entry -> {
                    Long courseId = entry.getKey();
                    List<UserLearningRecord> courseRecords = entry.getValue();
                    Course course = courseMapper.selectById(courseId);

                    TopCourseVO vo = new TopCourseVO();
                    vo.setCourseId(courseId);
                    vo.setCourseTitle(course != null ? course.getTitle() : "未知课程");
                    vo.setRecordCount(courseRecords.size());
                    vo.setTotalDurationMinutes(0);
                    vo.setTotalDurationHours(BigDecimal.ZERO);
                    vo.setLastLearnedAt(courseRecords.stream()
                            .map(UserLearningRecord::getCreatedAt)
                            .filter(Objects::nonNull)
                            .max(LocalDateTime::compareTo)
                            .orElse(null));
                    return vo;
                })
                .sorted((a, b) -> {
                    int cmp = b.getRecordCount().compareTo(a.getRecordCount());
                    if (cmp != 0) return cmp;
                    cmp = b.getTotalDurationMinutes().compareTo(a.getTotalDurationMinutes());
                    if (cmp != 0) return cmp;
                    if (a.getLastLearnedAt() != null && b.getLastLearnedAt() != null) {
                        return b.getLastLearnedAt().compareTo(a.getLastLearnedAt());
                    }
                    return 0;
                })
                .collect(Collectors.toList());

        for (int i = 0; i < allCourses.size(); i++) {
            allCourses.get(i).setRank(i + 1);
        }

        if (!allCourses.isEmpty()) {
            int maxCount = allCourses.get(0).getRecordCount();
            for (TopCourseVO vo : allCourses) {
                if (maxCount > 0) {
                    vo.setBarPercent(BigDecimal.valueOf(vo.getRecordCount())
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(maxCount), 2, RoundingMode.HALF_UP));
                } else {
                    vo.setBarPercent(BigDecimal.ZERO);
                }
            }
        }

        long total = allCourses.size();
        int page = query.getPage();
        int size = query.getSize();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, allCourses.size());
        List<TopCourseVO> pageRecords = (fromIndex >= allCourses.size())
                ? Collections.emptyList() : allCourses.subList(fromIndex, toIndex);

        long totalPages = (total + size - 1) / size;
        PageResult<TopCourseVO> result = new PageResult<>(pageRecords, total, (long) page, (long) size, totalPages);
        // 写入缓存
        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(cacheKey, json, LEARNER_RECORD_STATS_CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("写入热门课程缓存失败, userId={}", userId, e);
        }

        return result;
    }
    // ==================== 私有辅助方法 ====================

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

    private List<Long> getVisibleCourseIds(Long deptId) {
        LambdaQueryWrapper<CourseDepartment> cdWrapper = new LambdaQueryWrapper<>();
        cdWrapper.eq(CourseDepartment::getDepartmentId, deptId);
        List<CourseDepartment> courseDepartments = courseDepartmentMapper.selectList(cdWrapper);
        if (courseDepartments.isEmpty()) return Collections.emptyList();

        List<Long> courseIds = courseDepartments.stream()
                .map(CourseDepartment::getCourseId).collect(Collectors.toList());

        LambdaQueryWrapper<Course> courseWrapper = new LambdaQueryWrapper<>();
        courseWrapper.in(Course::getId, courseIds)
                .eq(Course::getStatus, 1)
                .isNull(Course::getDeletedAt);

        return courseMapper.selectList(courseWrapper).stream()
                .map(Course::getId).collect(Collectors.toList());
    }

    private void validateQueryParams(RecordQuery query) {
        if (query.getRange() != null && !query.getRange().isBlank()) {
            validateRange(query.getRange().toUpperCase());
        }
        if (query.getActionType() != null && !query.getActionType().isBlank()) {
            String at = query.getActionType().toUpperCase();
            if (!"ALL".equals(at) && !VALID_RECORD_TYPES.contains(at)) {
                throw new BusinessException(ErrorCode.LEARNER_RECORD_TYPE_PARAM_INVALID);
            }
        }
        if (query.getResourceType() != null && !query.getResourceType().isBlank()) {
            String rt = query.getResourceType().toUpperCase();
            if (!"ALL".equals(rt) && !VALID_RESOURCE_TYPES.contains(rt)) {
                throw new BusinessException(ErrorCode.LEARNER_RECORD_RESOURCE_TYPE_INVALID);
            }
        }
    }

    private void validateRange(String range) {
        if (range != null && !range.isBlank() && !VALID_RANGES.contains(range.toUpperCase())) {
            throw new BusinessException(ErrorCode.LEARNER_RECORD_RANGE_INVALID);
        }
    }

    // ---------- 查询过滤器 ----------

    private void applyRangeFilter(LambdaQueryWrapper<UserLearningRecord> wrapper, String range) {
        LocalDateTime rangeStart = resolveRangeStart(range);
        if (rangeStart != null) {
            wrapper.ge(UserLearningRecord::getCreatedAt, rangeStart);
        }
    }

    private void applyActionTypeFilter(LambdaQueryWrapper<UserLearningRecord> wrapper, String actionType) {
        if (actionType == null || actionType.isBlank() || "ALL".equalsIgnoreCase(actionType)) return;
        Integer at = switch (actionType.toUpperCase()) {
            case "START_COURSE" -> 1;
            case "CONTINUE_LEARNING" -> 2;
            case "COMPLETE_RESOURCE" -> 3;
            case "COMPLETE_POINT" -> 4;
            case "COMPLETE_COURSE" -> 5;
            case "REVIEW_COURSE" -> 6;
            default -> null;
        };
        if (at != null) {
            wrapper.eq(UserLearningRecord::getActionType, at);
        }
    }

    private void applyResourceTypeFilter(LambdaQueryWrapper<UserLearningRecord> wrapper, String resourceType) {
        if (resourceType == null || resourceType.isBlank() || "ALL".equalsIgnoreCase(resourceType)) return;
        Integer rt = switch (resourceType.toUpperCase()) {
            case "ARTICLE" -> 1;
            case "VIDEO" -> 2;
            case "PPT" -> 3;
            default -> null;
        };
        if (rt != null) {
            wrapper.eq(UserLearningRecord::getResourceType, rt);
        }
    }


    private LocalDateTime resolveRangeStart(String range) {
        if (range == null || range.isBlank()) return null;
        LocalDate today = LocalDate.now();
        return switch (range.toUpperCase()) {
            case "TODAY" -> today.atStartOfDay();
            case "LAST_7_DAYS" -> today.minusDays(6).atStartOfDay();
            case "LAST_30_DAYS" -> today.minusDays(29).atStartOfDay();
            default -> null;
        };
    }

    // ---------- VO 转换 ----------

    private LearningRecordVO toVO(UserLearningRecord record) {
        LearningRecordVO vo = new LearningRecordVO();
        vo.setRecordId(record.getId());
        vo.setActionType(actionTypeName(record.getActionType()));
        vo.setActionName(actionDisplayName(record.getActionType()));
        vo.setTitle(record.getTitle());
        vo.setDescription(record.getDescription());
        vo.setCourseId(record.getCourseId());

        Course course = courseMapper.selectById(record.getCourseId());
        vo.setCourseTitle(course != null ? course.getTitle() : "未知课程");

        vo.setCoursePointId(record.getCoursePointId());
        if (record.getCoursePointId() != null) {
            CoursePoint point = coursePointMapper.selectById(record.getCoursePointId());
            vo.setCoursePointTitle(point != null ? point.getTitle() : null);
        }

        vo.setResourceType(resourceTypeName(record.getResourceType()));
        vo.setResourceTypeName(resourceTypeDisplayName(record.getResourceType()));
        vo.setResourceId(record.getResourceId());
        vo.setResourceTitle(lookupResourceTitle(record.getResourceType(), record.getResourceId()));

        vo.setDurationMinutes(0);
        vo.setProgressPercent(null);
        vo.setOccurredAt(record.getCreatedAt());
        vo.setTimeText(formatTimeText(record.getCreatedAt()));

        return vo;
    }

    private String actionTypeName(Integer actionType) {
        if (actionType == null) return null;
        return switch (actionType) {
            case 1 -> "START_COURSE";
            case 2 -> "CONTINUE_LEARNING";
            case 3 -> "COMPLETE_RESOURCE";
            case 4 -> "COMPLETE_POINT";
            case 5 -> "COMPLETE_COURSE";
            case 6 -> "REVIEW_COURSE";
            default -> null;
        };
    }

    private String actionDisplayName(Integer actionType) {
        if (actionType == null) return null;
        return switch (actionType) {
            case 1 -> "已开始";
            case 2 -> "学习中";
            case 3 -> "已完成";
            case 4 -> "已完成";
            case 5 -> "已完成";
            case 6 -> "复习中";
            default -> null;
        };
    }

    private String resourceTypeName(Integer resourceType) {
        if (resourceType == null) return null;
        return switch (resourceType) {
            case 1 -> "ARTICLE";
            case 2 -> "VIDEO";
            case 3 -> "PPT";
            default -> null;
        };
    }

    private String resourceTypeDisplayName(Integer resourceType) {
        if (resourceType == null) return null;
        return switch (resourceType) {
            case 1 -> "文章";
            case 2 -> "视频";
            case 3 -> "PPT";
            default -> null;
        };
    }

    private String lookupResourceTitle(Integer resourceType, Long resourceId) {
        if (resourceType == null || resourceId == null) return null;
        try {
            return switch (resourceType) {
                case 1 -> {
                    Article article = articleMapper.selectById(resourceId);
                    yield article != null ? article.getTitle() : null;
                }
                case 2 -> {
                    Video video = videoMapper.selectById(resourceId);
                    yield video != null ? video.getTitle() : null;
                }
                case 3 -> {
                    Ppt ppt = pptMapper.selectById(resourceId);
                    yield ppt != null ? ppt.getTitle() : null;
                }
                default -> null;
            };
        } catch (Exception e) {
            log.warn("查询课件标题失败, resourceType={}, resourceId={}", resourceType, resourceId, e);
            return null;
        }
    }

    private String formatTimeText(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return String.format("%02d:%02d", dateTime.getHour(), dateTime.getMinute());
    }

    private boolean matchKeyword(LearningRecordVO vo, String keyword) {
        if (vo.getCourseTitle() != null && vo.getCourseTitle().toLowerCase().contains(keyword)) return true;
        if (vo.getCoursePointTitle() != null && vo.getCoursePointTitle().toLowerCase().contains(keyword)) return true;
        if (vo.getResourceTitle() != null && vo.getResourceTitle().toLowerCase().contains(keyword)) return true;
        if (vo.getTitle() != null && vo.getTitle().toLowerCase().contains(keyword)) return true;
        return false;
    }

//    // ---------- 进度概览 ----------
//
//    private ProgressOverviewVO buildProgressOverview(List<Long> visibleCourseIds, Long userId) {
//        ProgressOverviewVO overview = new ProgressOverviewVO();
//        overview.setTotalCount(visibleCourseIds.size());
//        if (visibleCourseIds.isEmpty()) {
//            overview.setCompletedCount(0);
//            overview.setLearningCount(0);
//            overview.setNotStartedCount(0);
//            overview.setOverallProgressPercent(BigDecimal.ZERO);
//            return overview;
//        }
//
//        LambdaQueryWrapper<UserCourseProgress> wrapper = new LambdaQueryWrapper<>();
//        wrapper.eq(UserCourseProgress::getUserId, userId)
//                .in(UserCourseProgress::getCourseId, visibleCourseIds);
//        List<UserCourseProgress> progresses = userCourseProgressMapper.selectList(wrapper);
//
//        Map<Long, Integer> statusMap = progresses.stream()
//                .collect(Collectors.toMap(UserCourseProgress::getCourseId, UserCourseProgress::getStatus, (v1, v2) -> v1));
//
//        int completedCount = (int) statusMap.values().stream().filter(s -> s == 2).count();
//        int learningCount = (int) statusMap.values().stream().filter(s -> s == 1).count();
//        int notStartedCount = visibleCourseIds.size() - completedCount - learningCount;
//
//        overview.setCompletedCount(completedCount);
//        overview.setLearningCount(learningCount);
//        overview.setNotStartedCount(notStartedCount);
//
//        BigDecimal percent = BigDecimal.valueOf(completedCount)
//                .multiply(BigDecimal.valueOf(100))
//                .divide(BigDecimal.valueOf(visibleCourseIds.size()), 2, RoundingMode.HALF_UP);
//        overview.setOverallProgressPercent(percent);
//        return overview;
//    }

    // ---------- 学习日历（从 user_learning_record 查询） ----------

    private Set<LocalDate> getLearningDatesFromRecord(Long userId, YearMonth month) {
        LambdaQueryWrapper<UserLearningRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserLearningRecord::getUserId, userId);

        if (month != null) {
            LocalDateTime start = month.atDay(1).atStartOfDay();
            LocalDateTime end = month.atEndOfMonth().atTime(23, 59, 59);
            wrapper.between(UserLearningRecord::getCreatedAt, start, end);
        }

        return userLearningRecordMapper.selectList(wrapper).stream()
                .map(r -> r.getCreatedAt().toLocalDate())
                .collect(Collectors.toSet());
    }

    private int calculateConsecutiveDays(Set<LocalDate> learningDates) {
        if (learningDates.isEmpty()) return 0;
        LocalDate date = LocalDate.now();
        if (!learningDates.contains(date)) {
            date = date.minusDays(1);
            if (!learningDates.contains(date)) return 0;
        }
        int streak = 0;
        while (learningDates.contains(date)) {
            streak++;
            date = date.minusDays(1);
        }
        return streak;
    }

    // ---------- 学习记录写入 ----------

    private void insertLearningRecord(Long userId, Long courseId, Long coursePointId,
                                      int actionType, Integer resourceType, Long resourceId,
                                      String title, LocalDateTime time) {
        UserLearningRecord record = new UserLearningRecord();
        record.setUserId(userId);
        record.setCourseId(courseId);
        record.setCoursePointId(coursePointId);
        record.setActionType(actionType);
        record.setResourceType(resourceType);
        record.setResourceId(resourceId);
        record.setTitle(title);
        record.setCreatedAt(time);
        userLearningRecordMapper.insert(record);
    }

    private String buildResourceCompleteTitle(UserCourseResourceProgress progress) {
        String typeName = switch (progress.getResourceType() != null ? progress.getResourceType() : 0) {
            case 1 -> "文章";
            case 2 -> "视频";
            case 3 -> "PPT";
            default -> "课件";
        };
        Course course = courseMapper.selectById(progress.getCourseId());
        String courseTitle = course != null ? course.getTitle() : "未知课程";
        return "完成了《" + courseTitle + "》的" + typeName;
    }

    // ---------- ID 解析 ----------

    private Long parseRecordId(String id) {
        try {
            Long parsed = Long.parseLong(id);
            if (parsed <= 0) throw new BusinessException(ErrorCode.LEARNER_RECORD_INVALID_ID);
            return parsed;
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.LEARNER_RECORD_INVALID_ID);
        }
    }

    private record ParsedId(String type, Long dbId) {}

    private ParsedId parseId(String id) {
        if (id == null || !id.contains(":")) {
            throw new BusinessException(ErrorCode.LEARNER_RECORD_INVALID_ID);
        }
        String[] parts = id.split(":", 2);
        String type = parts[0].toUpperCase();
        try {
            Long dbId = Long.parseLong(parts[1]);
            if (dbId <= 0) throw new BusinessException(ErrorCode.LEARNER_RECORD_INVALID_ID);
            return new ParsedId(type, dbId);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.LEARNER_RECORD_INVALID_ID);
        }
    }

    private UserCourseResourceProgress findResourceProgress(Long userId, ParsedId parsed) {
        LambdaQueryWrapper<UserCourseResourceProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCourseResourceProgress::getId, parsed.dbId)
                .eq(UserCourseResourceProgress::getUserId, userId);
        return userCourseResourceProgressMapper.selectOne(wrapper);
    }

    // ---------- 进度级联计算 ----------

    private void recalculatePointProgress(Long userId, Long courseId, Long pointId, LocalDateTime now) {
        List<UserCourseResourceProgress> pointResources = userCourseResourceProgressMapper.selectList(
                new LambdaQueryWrapper<UserCourseResourceProgress>()
                        .eq(UserCourseResourceProgress::getUserId, userId)
                        .eq(UserCourseResourceProgress::getCoursePointId, pointId));

        boolean allCompleted = !pointResources.isEmpty() &&
                pointResources.stream().allMatch(r -> r.getStatus() == 2);

        UserCoursePointProgress pointProgress = findPointProgress(userId, pointId);
        if (pointProgress == null) {
            pointProgress = new UserCoursePointProgress();
            pointProgress.setUserId(userId);
            pointProgress.setCourseId(courseId);
            pointProgress.setCoursePointId(pointId);
            pointProgress.setStartedAt(now);
            pointProgress.setCreatedAt(now);
        }

        boolean wasCompleted = pointProgress.getStatus() != null && pointProgress.getStatus() == 2;

        if (allCompleted) {
            pointProgress.setStatus(2);
            pointProgress.setCompletedAt(now);
        } else {
            pointProgress.setStatus(1);
            if (pointProgress.getCompletedAt() != null) pointProgress.setCompletedAt(null);
        }
        pointProgress.setUpdatedAt(now);

        if (pointProgress.getId() == null) {
            userCoursePointProgressMapper.insert(pointProgress);
        } else {
            userCoursePointProgressMapper.updateById(pointProgress);
        }

        if (allCompleted && !wasCompleted) {
            try {
                CoursePoint point = coursePointMapper.selectById(pointId);
                String title = "完成了课程点《" + (point != null ? point.getTitle() : "") + "》";
                insertLearningRecord(userId, courseId, pointId, 4, null, null, title, now);
            } catch (Exception e) {
                log.error("写入课程点完成记录失败, userId={}, pointId={}", userId, pointId, e);
            }
        }
    }

    private void recalculatePointProgressAfterReset(Long userId, Long courseId, Long pointId, LocalDateTime now) {
        List<UserCourseResourceProgress> pointResources = userCourseResourceProgressMapper.selectList(
                new LambdaQueryWrapper<UserCourseResourceProgress>()
                        .eq(UserCourseResourceProgress::getUserId, userId)
                        .eq(UserCourseResourceProgress::getCoursePointId, pointId));

        UserCoursePointProgress pointProgress = findPointProgress(userId, pointId);
        if (pointProgress == null) return;

        if (pointResources.isEmpty()) {
            pointProgress.setStatus(0);
            pointProgress.setStartedAt(null);
            pointProgress.setCompletedAt(null);
        } else {
            boolean allCompleted = pointResources.stream().allMatch(r -> r.getStatus() == 2);
            boolean anyStarted = pointResources.stream().anyMatch(r -> r.getStatus() >= 1);
            if (allCompleted) {
                pointProgress.setStatus(2);
            } else if (anyStarted) {
                pointProgress.setStatus(1);
                pointProgress.setCompletedAt(null);
            } else {
                pointProgress.setStatus(0);
                pointProgress.setStartedAt(null);
                pointProgress.setCompletedAt(null);
            }
        }
        pointProgress.setUpdatedAt(now);
        userCoursePointProgressMapper.updateById(pointProgress);
    }

    private UserCoursePointProgress findPointProgress(Long userId, Long pointId) {
        return userCoursePointProgressMapper.selectOne(
                new LambdaQueryWrapper<UserCoursePointProgress>()
                        .eq(UserCoursePointProgress::getUserId, userId)
                        .eq(UserCoursePointProgress::getCoursePointId, pointId));
    }

    private void recalculateCourseProgress(Long userId, Long courseId, LocalDateTime now) {
        List<UserCoursePointProgress> allPointProgress = userCoursePointProgressMapper.selectList(
                new LambdaQueryWrapper<UserCoursePointProgress>()
                        .eq(UserCoursePointProgress::getUserId, userId)
                        .eq(UserCoursePointProgress::getCourseId, courseId));

        long totalPoints = coursePointMapper.selectCount(
                new LambdaQueryWrapper<CoursePoint>()
                        .eq(CoursePoint::getCourseId, courseId)
                        .eq(CoursePoint::getStatus, 1)
                        .isNull(CoursePoint::getDeletedAt));

        long completedPoints = allPointProgress.stream().filter(p -> p.getStatus() == 2).count();
        long startedPoints = allPointProgress.stream().filter(p -> p.getStatus() >= 1).count();

        UserCourseProgress courseProgress = userCourseProgressMapper.selectOne(
                new LambdaQueryWrapper<UserCourseProgress>()
                        .eq(UserCourseProgress::getUserId, userId)
                        .eq(UserCourseProgress::getCourseId, courseId));

        if (courseProgress == null) {
            if (startedPoints > 0) {
                courseProgress = new UserCourseProgress();
                courseProgress.setUserId(userId);
                courseProgress.setCourseId(courseId);
                courseProgress.setStartedAt(now);
                courseProgress.setCreatedAt(now);
            } else {
                return;
            }
        }

        boolean wasCompleted = courseProgress.getStatus() != null && courseProgress.getStatus() == 2;

        if (totalPoints > 0 && completedPoints == totalPoints) {
            courseProgress.setStatus(2);
            courseProgress.setCompletedAt(now);
            courseProgress.setProgressPercent(BigDecimal.valueOf(100));
        } else if (startedPoints > 0) {
            courseProgress.setStatus(1);
            if (totalPoints > 0) {
                BigDecimal percent = BigDecimal.valueOf(completedPoints)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalPoints), 2, RoundingMode.HALF_UP);
                courseProgress.setProgressPercent(percent);
            }
            if (courseProgress.getCompletedAt() != null) courseProgress.setCompletedAt(null);
        } else {
            courseProgress.setStatus(0);
            courseProgress.setProgressPercent(BigDecimal.ZERO);
            courseProgress.setStartedAt(null);
            courseProgress.setCompletedAt(null);
        }

        courseProgress.setUpdatedAt(now);
        if (courseProgress.getId() == null) {
            userCourseProgressMapper.insert(courseProgress);
        } else {
            userCourseProgressMapper.updateById(courseProgress);
        }

        if (totalPoints > 0 && completedPoints == totalPoints && !wasCompleted) {
            try {
                Course course = courseMapper.selectById(courseId);
                String title = "完成了课程《" + (course != null ? course.getTitle() : "") + "》";
                insertLearningRecord(userId, courseId, null, 5, null, null, title, now);
            } catch (Exception e) {
                log.error("写入课程完成记录失败, userId={}, courseId={}", userId, courseId, e);
            }
        }
    }


    // ---------- 概览构建 ----------

    private RecordOverviewVO.SummaryCards buildSummaryCards(List<UserLearningRecord> records, Long userId, LocalDateTime rangeStart) {
        RecordOverviewVO.SummaryCards cards = new RecordOverviewVO.SummaryCards();
        cards.setRecordCount(records.size());
        cards.setCompletedResourceCount((int) records.stream().filter(r -> r.getActionType() == 3).count());
        cards.setCompletedPointCount((int) records.stream().filter(r -> r.getActionType() == 4).count());

        Set<LocalDate> learningDates;
        if (rangeStart != null) {
            learningDates = records.stream()
                    .filter(r -> r.getCreatedAt() != null)
                    .map(r -> r.getCreatedAt().toLocalDate())
                    .collect(Collectors.toSet());
        } else {
            learningDates = getLearningDatesFromRecord(userId, null);
        }
        cards.setLearningDays(learningDates.size());
        return cards;
    }

    // ---------- 趋势计算 ----------

    private List<FrequencyTrendVO.TrendPoint> buildHourlyTrend(List<UserLearningRecord> records) {
        Map<Integer, Long> hourCounts = records.stream()
                .filter(r -> r.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getCreatedAt().getHour(), Collectors.counting()));

        List<FrequencyTrendVO.TrendPoint> points = new ArrayList<>();
        int currentHour = LocalTime.now().getHour();
        for (int h = 0; h <= currentHour; h++) {
            FrequencyTrendVO.TrendPoint point = new FrequencyTrendVO.TrendPoint();
            point.setLabel(String.format("%02d:00", h));
            point.setDate(null);
            point.setHour(h);
            point.setCount(hourCounts.getOrDefault(h, 0L).intValue());
            points.add(point);
        }
        return points;
    }

    private List<FrequencyTrendVO.TrendPoint> buildDailyTrend(List<UserLearningRecord> records, String range) {
        Map<LocalDate, Long> dateCounts = records.stream()
                .filter(r -> r.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getCreatedAt().toLocalDate(), Collectors.counting()));

        LocalDate today = LocalDate.now();
        int days = "LAST_7_DAYS".equals(range) ? 7 : 30;
        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("MM-dd");
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<FrequencyTrendVO.TrendPoint> points = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            FrequencyTrendVO.TrendPoint point = new FrequencyTrendVO.TrendPoint();
            point.setLabel(date.format(labelFmt));
            point.setDate(date.format(dateFmt));
            point.setHour(null);
            point.setCount(dateCounts.getOrDefault(date, 0L).intValue());
            points.add(point);
        }
        return points;
    }

    // ---------- 课件分布 ----------

    /**
     * 从 user_course_resource_progress 表直接查询已完成的课件分布，
     * 不再依赖 action_type=3 的学习记录，确保数据始终准确
     */
    private List<ResourceDistributionVO> buildResourceDistributionFromProgress(Long userId, LocalDateTime rangeStart) {
        LambdaQueryWrapper<UserCourseResourceProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCourseResourceProgress::getUserId, userId)
               .eq(UserCourseResourceProgress::getStatus, 2);
        if (rangeStart != null) {
            wrapper.ge(UserCourseResourceProgress::getCompletedAt, rangeStart);
        }
        List<UserCourseResourceProgress> completedResources = userCourseResourceProgressMapper.selectList(wrapper);

        Map<Integer, Long> grouped = completedResources.stream()
                .filter(r -> r.getResourceType() != null)
                .collect(Collectors.groupingBy(UserCourseResourceProgress::getResourceType, Collectors.counting()));

        long articleCount = grouped.getOrDefault(1, 0L);
        long videoCount = grouped.getOrDefault(2, 0L);
        long pptCount = grouped.getOrDefault(3, 0L);
        long total = videoCount + articleCount + pptCount;

        List<ResourceDistributionVO> result = new ArrayList<>();
        result.add(buildDistributionItem("VIDEO", "视频", videoCount, total));
        result.add(buildDistributionItem("ARTICLE", "文章", articleCount, total));
        result.add(buildDistributionItem("PPT", "PPT", pptCount, total));
        return result;
    }

    private List<ResourceDistributionVO> buildResourceDistribution(List<UserLearningRecord> completedRecords) {
        Map<Integer, Long> grouped = completedRecords.stream()
                .filter(r -> r.getActionType() == 3 && r.getResourceType() != null)
                .collect(Collectors.groupingBy(UserLearningRecord::getResourceType, Collectors.counting()));

        long videoCount = grouped.getOrDefault(2, 0L);
        long articleCount = grouped.getOrDefault(1, 0L);
        long pptCount = grouped.getOrDefault(3, 0L);
        long total = videoCount + articleCount + pptCount;

        List<ResourceDistributionVO> result = new ArrayList<>();
        result.add(buildDistributionItem("VIDEO", "视频", videoCount, total));
        result.add(buildDistributionItem("ARTICLE", "文章", articleCount, total));
        result.add(buildDistributionItem("PPT", "PPT", pptCount, total));
        return result;
    }

    private ResourceDistributionVO buildDistributionItem(String type, String typeName, long count, long total) {
        BigDecimal percent = total > 0
                ? BigDecimal.valueOf(count).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return new ResourceDistributionVO(type, typeName, (int) count, percent);
    }

    // ---------- 频率趋势 ----------

    private FrequencyTrendVO buildFrequencyTrend(List<UserLearningRecord> records, String effectiveRange) {
        List<FrequencyTrendVO.TrendPoint> points;
        String unit;
        if ("TODAY".equals(effectiveRange)) {
            points = buildHourlyTrend(records);
            unit = "HOUR";
        } else {
            points = buildDailyTrend(records, effectiveRange);
            unit = "DAY";
        }

        int totalCount = points.stream().mapToInt(FrequencyTrendVO.TrendPoint::getCount).sum();
        int pointCount = points.size();
        BigDecimal averageCount = pointCount > 0
                ? BigDecimal.valueOf(totalCount).divide(BigDecimal.valueOf(pointCount), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new FrequencyTrendVO(unit, averageCount, points);
    }

    // ---------- 最多课程 ----------

    private List<TopCourseVO> buildTopCourses(List<UserLearningRecord> records, int limit) {
        Map<Long, List<UserLearningRecord>> byCourse = records.stream()
                .collect(Collectors.groupingBy(UserLearningRecord::getCourseId));

        List<TopCourseVO> courses = byCourse.entrySet().stream()
                .map(entry -> {
                    Long courseId = entry.getKey();
                    List<UserLearningRecord> courseRecords = entry.getValue();
                    Course course = courseMapper.selectById(courseId);

                    TopCourseVO vo = new TopCourseVO();
                    vo.setCourseId(courseId);
                    vo.setCourseTitle(course != null ? course.getTitle() : "未知课程");
                    vo.setRecordCount(courseRecords.size());
                    vo.setTotalDurationMinutes(0);
                    vo.setTotalDurationHours(BigDecimal.ZERO);
                    vo.setLastLearnedAt(courseRecords.stream()
                            .map(UserLearningRecord::getCreatedAt)
                            .filter(Objects::nonNull)
                            .max(LocalDateTime::compareTo)
                            .orElse(null));
                    return vo;
                })
                .sorted((a, b) -> {
                    int cmp = b.getRecordCount().compareTo(a.getRecordCount());
                    if (cmp != 0) return cmp;
                    cmp = b.getTotalDurationMinutes().compareTo(a.getTotalDurationMinutes());
                    if (cmp != 0) return cmp;
                    if (a.getLastLearnedAt() != null && b.getLastLearnedAt() != null) {
                        return b.getLastLearnedAt().compareTo(a.getLastLearnedAt());
                    }
                    return 0;
                })
                .limit(limit)
                .collect(Collectors.toList());

        for (int i = 0; i < courses.size(); i++) {
            courses.get(i).setRank(i + 1);
        }

        if (!courses.isEmpty()) {
            int maxCount = courses.get(0).getRecordCount();
            for (TopCourseVO vo : courses) {
                if (maxCount > 0) {
                    vo.setBarPercent(BigDecimal.valueOf(vo.getRecordCount())
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(maxCount), 2, RoundingMode.HALF_UP));
                } else {
                    vo.setBarPercent(BigDecimal.ZERO);
                }
            }
        }

        return courses;
    }
}
