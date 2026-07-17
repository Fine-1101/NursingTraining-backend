package org.example.nursingtrainingbackend.modules.learningreport.service;

import org.example.nursingtrainingbackend.modules.learningreport.entity.AiLearningReport;
import org.example.nursingtrainingbackend.modules.learningreport.enums.ReportType;
import org.example.nursingtrainingbackend.modules.learningreport.mapper.AiLearningReportMapper;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class LearningReportTransactionServiceTest {

    @Test
    void regenerateCurrentWeekReportExtendsPeriodEndToNow() {
        LearningReportTransactionService service = createService();
        AiLearningReport previous = previousReport(
                LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay(),
                LocalDateTime.now().minusHours(2)
        );
        LocalDateTime before = LocalDateTime.now();

        AiLearningReport regenerated = service.createRegeneratedReport(previous);

        LocalDateTime after = LocalDateTime.now();
        assertFalse(regenerated.getPeriodEnd().isBefore(before));
        assertFalse(regenerated.getPeriodEnd().isAfter(after));
        assertTrue(regenerated.getPeriodEnd().isAfter(previous.getPeriodEnd()));
    }

    @Test
    void regenerateHistoricalReportKeepsOriginalPeriodEnd() {
        LearningReportTransactionService service = createService();
        LocalDate historicalStart = LocalDate.now()
                .with(DayOfWeek.MONDAY)
                .minusWeeks(1);
        LocalDateTime historicalEnd = historicalStart.plusDays(6)
                .atTime(23, 59, 59);
        AiLearningReport previous = previousReport(
                historicalStart.atStartOfDay(),
                historicalEnd
        );

        AiLearningReport regenerated = service.createRegeneratedReport(previous);

        assertEquals(historicalEnd, regenerated.getPeriodEnd());
    }

    private LearningReportTransactionService createService() {
        AiLearningReportMapper mapper = mock(AiLearningReportMapper.class);
        doAnswer(invocation -> {
            AiLearningReport report = invocation.getArgument(0);
            report.setId(100L);
            return 1;
        }).when(mapper).insert(any(AiLearningReport.class));
        return new LearningReportTransactionService(mapper);
    }

    private AiLearningReport previousReport(
            LocalDateTime periodStart,
            LocalDateTime periodEnd
    ) {
        AiLearningReport previous = new AiLearningReport();
        previous.setId(19L);
        previous.setUserId(1L);
        previous.setReportType(ReportType.WEEKLY.name());
        previous.setPeriodStart(periodStart);
        previous.setPeriodEnd(periodEnd);
        previous.setReportVersion(1);
        return previous;
    }
}
