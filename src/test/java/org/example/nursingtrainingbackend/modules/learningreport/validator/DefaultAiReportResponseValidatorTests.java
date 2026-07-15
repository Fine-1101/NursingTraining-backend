package org.example.nursingtrainingbackend.modules.learningreport.validator;

import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.config.properties.LearningReportProperties;
import org.example.nursingtrainingbackend.modules.learningreport.dto.GeneratedLearningReport;
import org.example.nursingtrainingbackend.modules.learningreport.dto.LearningReportSnapshot;
import org.example.nursingtrainingbackend.modules.learningreport.enums.DataQualityLevel;
import org.example.nursingtrainingbackend.modules.learningreport.enums.ReportMode;
import org.example.nursingtrainingbackend.modules.learningreport.enums.ReportType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultAiReportResponseValidatorTests {

    private final DefaultAiReportResponseValidator validator =
            new DefaultAiReportResponseValidator(createProperties());

    @Test
    void shouldAcceptCandidateCourseNavigation() {
        LearningReportSnapshot snapshot = createSnapshot();
        GeneratedLearningReport report = createReport(20L, 108L);

        assertDoesNotThrow(() -> validator.validate(report, snapshot));
    }

    @Test
    void shouldRejectInventedCourseNavigation() {
        LearningReportSnapshot snapshot = createSnapshot();
        GeneratedLearningReport report = createReport(999L, 108L);

        assertThrows(
                BusinessException.class,
                () -> validator.validate(report, snapshot)
        );
    }

    private GeneratedLearningReport createReport(
            Long courseId,
            Long pointId
    ) {
        return new GeneratedLearningReport(
                "测试学习报告",
                "这是用于测试导航校验的报告总结。",
                "STABLE",
                new LearningReportSnapshot.ReportOverview(
                        3, 85, 4, 1, null
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of(
                        new GeneratedLearningReport.StudyPlanItem(
                                1,
                                LocalDate.now().plusDays(1),
                                "复习课程知识点",
                                "观看课程并完成练习",
                                20,
                                "用于巩固薄弱内容",
                                new GeneratedLearningReport.Navigation(
                                        "OPEN_COURSE_POINT",
                                        courseId,
                                        10L,
                                        pointId
                                )
                        )
                ),
                "继续保持学习。",
                "本报告仅用于培训辅助，不构成临床操作依据。"
        );
    }

    private LearningReportSnapshot createSnapshot() {
        return new LearningReportSnapshot(
                "1.0",
                ReportType.WEEKLY,
                ReportMode.ONBOARDING,
                new LearningReportSnapshot.ReportPeriod(
                        LocalDateTime.now().minusDays(3),
                        LocalDateTime.now(),
                        4,
                        7
                ),
                new LearningReportSnapshot.ReportDataQuality(
                        DataQualityLevel.LOW,
                        38
                ),
                new LearningReportSnapshot.ReportOverview(
                        3, 85, 4, 1, null
                ),
                List.of(),
                List.of(),
                List.of(
                        new LearningReportSnapshot.CandidateCourse(
                                20L,
                                "无菌技术基础",
                                10L,
                                108L,
                                "无菌技术操作",
                                20
                        )
                ),
                List.of("注册不足7天")
        );
    }

    private LearningReportProperties createProperties() {
        return new LearningReportProperties(
                true,
                null,
                new LearningReportProperties.Generation(
                        3,
                        2,
                        3,
                        3,
                        7,
                        true
                ),
                null,
                null,
                null
        );
    }
}
