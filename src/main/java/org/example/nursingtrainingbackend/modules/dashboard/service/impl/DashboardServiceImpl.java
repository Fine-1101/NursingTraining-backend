package org.example.nursingtrainingbackend.modules.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.modules.course.entity.Course;
import org.example.nursingtrainingbackend.modules.course.mapper.CourseMapper;
import org.example.nursingtrainingbackend.modules.dashboard.dto.CourseTrendRow;
import org.example.nursingtrainingbackend.modules.dashboard.dto.DepartmentRankRow;
import org.example.nursingtrainingbackend.modules.dashboard.dto.StatusCountRow;
import org.example.nursingtrainingbackend.modules.dashboard.dto.TrendRow;
import org.example.nursingtrainingbackend.modules.dashboard.mapper.DashboardMapper;
import org.example.nursingtrainingbackend.modules.dashboard.service.DashboardService;
import org.example.nursingtrainingbackend.modules.dashboard.vo.CourseLearningTrendVO;
import org.example.nursingtrainingbackend.modules.dashboard.vo.DashboardVO;
import org.example.nursingtrainingbackend.modules.dashboard.vo.LearningTrendDrillVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.baomidou.mybatisplus.extension.ddl.DdlScriptErrorHandler.PrintlnLogErrorHandler.log;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private static final String DASHBOARD_CACHE_PREFIX = "nursing:dashboard:v1:";
    private static final long CACHE_TTL_SECONDS = 120;

    private final DashboardMapper dashboardMapper;
    private final CourseMapper courseMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;


    @Override
    public DashboardVO getDashboard(String range, int departmentLimit) {
        String cacheKey=DASHBOARD_CACHE_PREFIX+range+":"+departmentLimit;
        try{
            String cached=redisTemplate.opsForValue().get(cacheKey);
            if(cached!=null){
                return objectMapper.readValue(cached, DashboardVO.class);
            }
        } catch (Exception e) {
            log.warn("读取仪表盘缓存失败, key={}", cacheKey, e);
        }
        DashboardVO vo = DashboardVO.builder()
                .summaryCards(buildSummaryCards())
                .learningStatusDistribution(buildLearningStatusDistribution())
                .learningTrend(buildLearningTrend(range))
                .courseLearningTrend(buildCourseLearningTrend())
                .departmentCompletionRanking(buildDepartmentRanking(departmentLimit))
                .quickEntries(buildQuickEntries())
                .build();

        try {
            String json = objectMapper.writeValueAsString(vo);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("写入仪表盘缓存失败, key={}", cacheKey, e);
        }

        return vo;
    }

    @Override
    public void evictDashboardCache() {
        try {
            var keys = redisTemplate.keys(DASHBOARD_CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("已清除仪表盘缓存, count={}", keys.size());
            }
        } catch (Exception e) {
            log.warn("清除仪表盘缓存失败");
        }
    }

    @Override
    public CourseLearningTrendVO getCourseLearningTrend(Long courseId, String range, String granularity) {
        String normalizedRange = range == null ? "" : range.trim().toUpperCase();
        String normalizedGranularity = granularity == null ? "" : granularity.trim().toUpperCase();
        validateCourseTrendParameters(courseId, normalizedRange, normalizedGranularity);
    
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
    
        LocalDate today = LocalDate.now();
        List<PeriodWindow> timeline = buildCourseTrendTimeline(today, normalizedGranularity);
        LocalDate start = timeline.getFirst().start();
        LocalDate endExclusive = timeline.getLast().endInclusive().plusDays(1);
    
        List<CourseTrendRow> rawData = dashboardMapper.selectCourseTrendByRange(
                courseId,
                start.format(DateTimeFormatter.ISO_LOCAL_DATE),
                endExclusive.format(DateTimeFormatter.ISO_LOCAL_DATE),
                normalizedGranularity);
    
        Map<String, CourseTrendRow> dataByPeriod = rawData.stream()
                .collect(Collectors.toMap(CourseTrendRow::getPeriodStart, row -> row, (left, right) -> left));
        List<CourseLearningTrendVO.Point> points = timeline.stream()
                .map(period -> toCourseTrendPoint(period, dataByPeriod.get(period.key())))
                .toList();
    
        return CourseLearningTrendVO.builder()
                .courseId(courseId)
                .courseTitle(course.getTitle())
                .range(normalizedRange)
                .granularity(normalizedGranularity)
                .points(points)
                .build();
    }

    @Override
    public LearningTrendDrillVO getLearningTrendDrill(int year, int month, Integer weekIndex) {
        if (month < 1 || month > 12) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "month 必须在 1-12 之间");
        }

        List<LearningTrendDrillVO.BreadcrumbItem> breadcrumbs = new ArrayList<>();
        breadcrumbs.add(LearningTrendDrillVO.BreadcrumbItem.builder()
                .label(year + "年" + month + "月")
                .level("MONTH").year(year).month(month).build());

        List<TrendRow> rawData;
        String level;

        if (weekIndex == null) {
            level = "WEEK";
            rawData = dashboardMapper.selectWeeklyTrendInMonth(year, month);
        } else {
            if (weekIndex < 1 || weekIndex > 5) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "weekIndex 必须在 1-5 之间");
            }
            level = "DAY";
            LocalDate monthStart = LocalDate.of(year, month, 1);
            LocalDate weekStart = monthStart.plusDays((long) (weekIndex - 1) * 7);
            LocalDate monthEnd = monthStart.plusMonths(1);
            LocalDate weekEnd = weekStart.plusDays(7);
            if (weekEnd.isAfter(monthEnd)) {
                weekEnd = monthEnd;
            }
            breadcrumbs.add(LearningTrendDrillVO.BreadcrumbItem.builder()
                    .label("第" + weekIndex + "周")
                    .level("WEEK").year(year).month(month).weekIndex(weekIndex).build());
            rawData = dashboardMapper.selectDailyTrendInWeek(
                    weekStart.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    weekEnd.format(DateTimeFormatter.ISO_LOCAL_DATE));
        }

        List<LearningTrendDrillVO.DrillTrendPoint> points = rawData.stream()
                .map(r -> LearningTrendDrillVO.DrillTrendPoint.builder()
                        .label(r.getLabel())
                        .period(r.getPeriod())
                        .learnerCount(nullToZero(r.getLearnerCount()))
                        .completedCourseCount(nullToZero(r.getCompletedCourseCount()))
                        .build())
                .toList();

        return LearningTrendDrillVO.builder()
                .level(level)
                .breadcrumbs(breadcrumbs)
                .points(points)
                .build();
    }

    private void validateCourseTrendParameters(Long courseId, String range, String granularity) {
        if (courseId == null || courseId <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "courseId 必须为正整数");
        }
        Map<String, String> expectedGranularity = Map.of(
                "LAST_1_WEEKS", "DAY",
                "LAST_1_MONTHS", "WEEK",
                "LAST_6_MONTHS", "MONTH");
        if (!expectedGranularity.containsKey(range)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "range 仅支持 LAST_1_WEEKS、LAST_1_MONTHS、LAST_6_MONTHS");
        }
        if (!expectedGranularity.get(range).equals(granularity)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "granularity 与 range 不匹配，" + range + " 应使用 " + expectedGranularity.get(range));
        }
    }

    static List<PeriodWindow> buildCourseTrendTimeline(LocalDate today, String granularity) {
        List<PeriodWindow> periods = new ArrayList<>();
        if ("DAY".equals(granularity)) {
            LocalDate start = today.minusDays(6);
            for (int i = 0; i < 7; i++) {
                LocalDate date = start.plusDays(i);
                periods.add(new PeriodWindow(date, date, date.format(DateTimeFormatter.ofPattern("MM-dd"))));
            }
        } else if ("WEEK".equals(granularity)) {
            LocalDate start = today.minusDays(27);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
            for (int i = 0; i < 4; i++) {
                LocalDate weekStart = start.plusWeeks(i);
                LocalDate weekEnd = weekStart.plusDays(6);
                periods.add(new PeriodWindow(weekStart, weekEnd,
                        weekStart.format(formatter) + "~" + weekEnd.format(formatter)));
            }
        } else {
            LocalDate start = today.withDayOfMonth(1).minusMonths(5);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
            for (int i = 0; i < 6; i++) {
                LocalDate monthStart = start.plusMonths(i);
                periods.add(new PeriodWindow(monthStart, monthStart.withDayOfMonth(monthStart.lengthOfMonth()),
                        monthStart.format(formatter)));
            }
        }
        return periods;
    }

    private CourseLearningTrendVO.Point toCourseTrendPoint(PeriodWindow period, CourseTrendRow row) {
        int learnerCount = row == null ? 0 : nullToZero(row.getLearnerCount());
        int completedCount = row == null ? 0 : nullToZero(row.getCompletedLearnerCount());
        BigDecimal completionRate = learnerCount == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(completedCount)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(learnerCount), 1, RoundingMode.HALF_UP);
        return CourseLearningTrendVO.Point.builder()
                .label(period.label())
                .date(period.key())
                .learnerCount(learnerCount)
                .completionRate(completionRate)
                .build();
    }

    record PeriodWindow(LocalDate start, LocalDate endInclusive, String label) {
        String key() {
            return start.format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
    }

    // ========== 顶部卡片 ==========

    private DashboardVO.SummaryCards buildSummaryCards() {
        int courseTotal = dashboardMapper.countCourses();
        int learnerTotal = dashboardMapper.countLearners();

        LocalDate today = LocalDate.now();
        LocalDate lastMonthEnd = today.withDayOfMonth(1).minusDays(1);
        String lastMonthEndStr = lastMonthEnd.format(DateTimeFormatter.ISO_LOCAL_DATE);

        DashboardVO.CardItem courseCard = buildCardItem(courseTotal,
                dashboardMapper.selectLastMonthEndCourseSnapshot(lastMonthEndStr));

        String nextMonthStartStr = lastMonthEnd.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        DashboardVO.CardItem learnerCard = buildCardItem(learnerTotal,
                dashboardMapper.selectLearnerCountBefore(nextMonthStartStr));

        return DashboardVO.SummaryCards.builder()
                .courseTotal(courseCard)
                .learnerTotal(learnerCard)
                .build();
    }

    private DashboardVO.CardItem buildCardItem(int currentValue, Integer previousValue) {
        if (previousValue == null || previousValue == 0) {
            return DashboardVO.CardItem.builder()
                    .value(currentValue)
                    .changeRate(null)
                    .changeDirection("NO_DATA")
                    .build();
        }
        BigDecimal changeRate = BigDecimal.valueOf(currentValue - previousValue)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(previousValue), 1, RoundingMode.HALF_UP);
        String direction;
        if (changeRate.compareTo(BigDecimal.ZERO) > 0) {
            direction = "UP";
        } else if (changeRate.compareTo(BigDecimal.ZERO) < 0) {
            direction = "DOWN";
            changeRate = changeRate.abs();
        } else {
            direction = "SAME";
        }
        return DashboardVO.CardItem.builder()
                .value(currentValue)
                .changeRate(changeRate)
                .changeDirection(direction)
                .build();
    }

    // ========== 学习状态分布 ==========

    private DashboardVO.LearningStatusDistribution buildLearningStatusDistribution() {
        int totalLearners = dashboardMapper.countLearners();
        List<StatusCountRow> statusCounts = dashboardMapper.countProgressByStatus();

        Map<Integer, Integer> countMap = statusCounts.stream()
                .collect(Collectors.toMap(
                        StatusCountRow::getStatus,
                        StatusCountRow::getCnt,
                        (a, b) -> a
                ));

        int startedCount = countMap.getOrDefault(1, 0) + countMap.getOrDefault(2, 0);
        int notStartedCount = totalLearners - startedCount;

        List<DashboardVO.StatusItem> items = new ArrayList<>();
        items.add(buildStatusItem("NOT_STARTED", "未开始", notStartedCount, totalLearners));
        items.add(buildStatusItem("LEARNING", "进行中", countMap.getOrDefault(1, 0), totalLearners));
        items.add(buildStatusItem("COMPLETED", "已完成", countMap.getOrDefault(2, 0), totalLearners));

        return DashboardVO.LearningStatusDistribution.builder()
                .totalLearners(totalLearners)
                .items(items)
                .build();
    }

    private DashboardVO.StatusItem buildStatusItem(String status, String statusName, int count, int total) {
        BigDecimal percent = total == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(count).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
        return DashboardVO.StatusItem.builder()
                .status(status)
                .statusName(statusName)
                .count(count)
                .percent(percent)
                .build();
    }

    // ========== 学习数据趋势 ==========

    private DashboardVO.LearningTrend buildLearningTrend(String range) {
        LocalDate today = LocalDate.now();
        String unit;
        String startDate;
        String endDate = today.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        List<TrendRow> rawData;

        if ("LAST_1_WEEKS".equals(range)) {
            unit = "WEEK";
            startDate = today.minusWeeks(1).with(DayOfWeek.MONDAY)
                    .format(DateTimeFormatter.ISO_LOCAL_DATE);
            rawData = dashboardMapper.selectWeeklyLearningTrend(startDate, endDate);
        } else if ("LAST_6_MONTHS".equals(range)) {
            unit = "MONTH";
            startDate = today.minusMonths(6).withDayOfMonth(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
            rawData = dashboardMapper.selectMonthlyLearningTrend(startDate, endDate);
        } else {
            unit = "MONTH";
            startDate = today.minusMonths(1).withDayOfMonth(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
            rawData = dashboardMapper.selectMonthlyLearningTrend(startDate, endDate);
        }

        List<DashboardVO.TrendPoint> points = rawData.stream()
                .map(r -> DashboardVO.TrendPoint.builder()
                        .label(r.getLabel())
                        .period(r.getPeriod())
                        .learnerCount(nullToZero(r.getLearnerCount()))
                        .completedCourseCount(nullToZero(r.getCompletedCourseCount()))
                        .build())
                .collect(Collectors.toList());

        return DashboardVO.LearningTrend.builder()
                .range(range)
                .unit(unit)
                .points(points)
                .build();
    }

    // ========== 课程学习趋势 ==========

    private DashboardVO.CourseLearningTrend buildCourseLearningTrend() {
        Long topCourseId = findTopCourseId();
        if (topCourseId == null) {
            return DashboardVO.CourseLearningTrend.builder()
                    .courseTitle("暂无数据")
                    .points(List.of())
                    .build();
        }

        Course course = courseMapper.selectById(topCourseId);
        String courseTitle = course != null ? course.getTitle() + " 的学习人数 vs 完成率" : "暂无数据";

        List<CourseTrendRow> rawData = dashboardMapper.selectCourseMonthlyTrend(topCourseId);
        List<DashboardVO.CourseTrendPoint> points = rawData.stream()
                .map(r -> DashboardVO.CourseTrendPoint.builder()
                        .label(r.getLabel())
                        .learnerCount(nullToZero(r.getLearnerCount()))
                        .completionRate(r.getCompletionRate() != null ? r.getCompletionRate() : BigDecimal.ZERO)
                        .build())
                .collect(Collectors.toList());

        return DashboardVO.CourseLearningTrend.builder()
                .courseTitle(courseTitle)
                .points(points)
                .build();
    }

    private Long findTopCourseId() {
        LambdaQueryWrapper<Course> qw = new LambdaQueryWrapper<>();
        qw.eq(Course::getStatus, 1)
          .orderByDesc(Course::getPublishedAt)
          .last("LIMIT 1");
        Course course = courseMapper.selectOne(qw);
        return course != null ? course.getId() : null;
    }

    // ========== 科室排行 ==========

    private List<DashboardVO.DepartmentRankingItem> buildDepartmentRanking(int limit) {
        List<DepartmentRankRow> rawData = dashboardMapper.selectDepartmentRanking(limit);
        return rawData.stream()
                .map(r -> DashboardVO.DepartmentRankingItem.builder()
                        .departmentId(r.getDepartmentId())
                        .departmentName(r.getDepartmentName())
                        .learnerCount(nullToZero(r.getLearnerCount()))
                        .completedLearnerCount(nullToZero(r.getCompletedLearnerCount()))
                        .completionRate(r.getCompletionRate() != null ? r.getCompletionRate() : BigDecimal.ZERO)
                        .build())
                .collect(Collectors.toList());
    }

    // ========== 快捷入口 ==========

    private List<DashboardVO.QuickEntry> buildQuickEntries() {
        return List.of(
                DashboardVO.QuickEntry.builder()
                        .code("CREATE_COURSE").title("新建课程").path("/admin/courses/create").build(),
                DashboardVO.QuickEntry.builder()
                        .code("CREATE_TAG").title("新建标签").path("/admin/tags").build(),
                DashboardVO.QuickEntry.builder()
                        .code("CREATE_CATEGORY").title("新建类别").path("/admin/categories").build(),
                DashboardVO.QuickEntry.builder()
                        .code("USER_MANAGEMENT").title("学员管理").path("/admin/users").build()
        );
    }

    // ========== 工具方法 ==========

    private int nullToZero(Integer v) {
        return v != null ? v : 0;
    }
}
