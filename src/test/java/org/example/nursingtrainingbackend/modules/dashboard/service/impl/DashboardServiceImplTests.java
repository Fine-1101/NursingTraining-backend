package org.example.nursingtrainingbackend.modules.dashboard.service.impl;

import org.example.nursingtrainingbackend.modules.course.entity.Course;
import org.example.nursingtrainingbackend.modules.course.mapper.CourseMapper;
import org.example.nursingtrainingbackend.modules.dashboard.dto.CourseTrendRow;
import org.example.nursingtrainingbackend.modules.dashboard.dto.StatusCountRow;
import org.example.nursingtrainingbackend.modules.dashboard.mapper.DashboardMapper;
import org.example.nursingtrainingbackend.modules.dashboard.vo.CourseLearningTrendVO;
import org.example.nursingtrainingbackend.modules.dashboard.vo.DashboardVO;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardServiceImplTests {

    @Test
    void learningStatusDistributionUsesMutuallyExclusiveStatusCounts() {
        DashboardMapper dashboardMapper = mock(DashboardMapper.class);
        DashboardServiceImpl service = new DashboardServiceImpl(
                dashboardMapper,
                mock(CourseMapper.class),
                mock(StringRedisTemplate.class),
                mock(ObjectMapper.class));

        when(dashboardMapper.countLearners()).thenReturn(48);
        when(dashboardMapper.countProgressByStatus()).thenReturn(List.of(
                statusCount(0, 5), statusCount(1, 20), statusCount(2, 23)));

        DashboardVO.LearningStatusDistribution result = ReflectionTestUtils.invokeMethod(
                service, "buildLearningStatusDistribution");

        assertEquals(48, result.getTotalLearners());
        assertEquals(5, result.getItems().getFirst().getCount());
        assertEquals(new BigDecimal("10.4"), result.getItems().getFirst().getPercent());
        assertEquals(48, result.getItems().stream().mapToInt(DashboardVO.StatusItem::getCount).sum());
    }

    private static StatusCountRow statusCount(int status, int count) {
        StatusCountRow row = new StatusCountRow();
        row.setStatus(status);
        row.setCnt(count);
        return row;
    }

    @Test
    void dailyTimelineContainsExactlyLastSevenDays() {
        LocalDate today = LocalDate.of(2026, 7, 14);

        List<DashboardServiceImpl.PeriodWindow> timeline =
                DashboardServiceImpl.buildCourseTrendTimeline(today, "DAY");

        assertEquals(7, timeline.size());
        assertEquals("2026-07-08", timeline.getFirst().key());
        assertEquals("07-08", timeline.getFirst().label());
        assertEquals("2026-07-14", timeline.getLast().key());
        assertEquals("07-14", timeline.getLast().label());
    }

    @Test
    void weeklyAndMonthlyTimelinesContainAllPeriods() {
        LocalDate today = LocalDate.of(2026, 7, 14);

        List<DashboardServiceImpl.PeriodWindow> weeks =
                DashboardServiceImpl.buildCourseTrendTimeline(today, "WEEK");
        List<DashboardServiceImpl.PeriodWindow> months =
                DashboardServiceImpl.buildCourseTrendTimeline(today, "MONTH");

        assertEquals(4, weeks.size());
        assertEquals("06-17~06-23", weeks.getFirst().label());
        assertEquals("07-08~07-14", weeks.getLast().label());
        assertEquals(6, months.size());
        assertEquals("2026-02", months.getFirst().label());
        assertEquals("2026-07", months.getLast().label());
    }

    @Test
    void courseTrendFillsPeriodsMissingFromDatabase() {
        DashboardMapper dashboardMapper = mock(DashboardMapper.class);
        CourseMapper courseMapper = mock(CourseMapper.class);
        DashboardServiceImpl service = new DashboardServiceImpl(
                dashboardMapper,
                courseMapper,
                mock(StringRedisTemplate.class),
                mock(ObjectMapper.class));

        Course course = new Course();
        course.setId(4L);
        course.setTitle("老年跌倒预防与安全管理");
        when(courseMapper.selectById(4L)).thenReturn(course);

        String today = LocalDate.now().toString();
        CourseTrendRow row = new CourseTrendRow();
        row.setPeriodStart(today);
        row.setLearnerCount(6);
        row.setCompletedLearnerCount(5);
        when(dashboardMapper.selectCourseTrendByRange(eq(4L), anyString(), anyString(), eq("DAY")))
                .thenReturn(List.of(row));

        CourseLearningTrendVO result = service.getCourseLearningTrend(4L, "LAST_1_WEEKS", "DAY");

        assertEquals("LAST_1_WEEKS", result.getRange());
        assertEquals("DAY", result.getGranularity());
        assertEquals(7, result.getPoints().size());
        assertEquals(0, result.getPoints().getFirst().getLearnerCount());
        assertEquals(BigDecimal.ZERO, result.getPoints().getFirst().getCompletionRate());
        assertEquals(6, result.getPoints().getLast().getLearnerCount());
        assertEquals(new BigDecimal("83.3"), result.getPoints().getLast().getCompletionRate());
    }
}
