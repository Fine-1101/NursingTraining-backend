package org.example.nursingtrainingbackend.modules.learningreport.ai;

import org.example.nursingtrainingbackend.modules.learningreport.dto.GeneratedLearningReport;
import org.example.nursingtrainingbackend.modules.learningreport.dto.LearningReportSnapshot;
import org.example.nursingtrainingbackend.modules.learningreport.enums.DataQualityLevel;
import org.example.nursingtrainingbackend.modules.learningreport.enums.ReportMode;
import org.example.nursingtrainingbackend.modules.learningreport.enums.ReportType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FakeAiReportClientTests {

    private final FakeAiReportClient client = new FakeAiReportClient();

    @Test
    void shouldReturnFixedStructuredReport() {
        LearningReportSnapshot snapshot = createSnapshot();
        ReportGenerationOptions options = new ReportGenerationOptions(
                "learning-report-v1.0",
                "1.0",
                2000,
                new BigDecimal("0.3")
        );

        AiReportResult result = client.generate(snapshot, options);

        assertNotNull(result);
        assertEquals("fake", result.provider());
        assertEquals("fake-model", result.model());
        assertEquals(0, result.totalTokens());
        assertNotNull(result.requestId());

        GeneratedLearningReport report = result.report();
        assertEquals("测试学习报告", report.title());
        assertEquals(snapshot.overview(), report.overview());
        assertFalse(report.highlights().isEmpty());
        assertNotNull(report.disclaimer());
    }

    @Test
    void shouldRejectNullSnapshot() {
        ReportGenerationOptions options = new ReportGenerationOptions(
                "learning-report-v1.0",
                "1.0",
                2000,
                new BigDecimal("0.3")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> client.generate(null, options)
        );
    }

    private LearningReportSnapshot createSnapshot() {
        return new LearningReportSnapshot(
                "1.0",
                ReportType.WEEKLY,
                ReportMode.ONBOARDING,
                new LearningReportSnapshot.ReportPeriod(
                        LocalDateTime.of(2026, 7, 11, 15, 20),
                        LocalDateTime.of(2026, 7, 14, 18, 0),
                        4,
                        7
                ),
                new LearningReportSnapshot.ReportDataQuality(
                        DataQualityLevel.LOW,
                        38
                ),
                new LearningReportSnapshot.ReportOverview(
                        3,
                        85,
                        4,
                        1,
                        new BigDecimal("82.5")
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of("注册不足7天", "考核样本较少")
        );
    }
}
