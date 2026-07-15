package org.example.nursingtrainingbackend.modules.learningreport.vo;

import org.example.nursingtrainingbackend.modules.learningreport.enums.DataQualityLevel;
import org.example.nursingtrainingbackend.modules.learningreport.enums.ReportMode;

import java.time.LocalDateTime;
import java.util.List;

public record ReportEligibilityVO(
        boolean eligible,
        ReportMode recommendedMode,
        long registeredDays,
        long validLearningEventCount,
        DataQuality dataQuality,
        Capabilities availableCapabilities,
        LocalDateTime nextFullReportEligibleAt,
        String reasonCode,
        String reasonMessage
) {
    public record DataQuality(
            DataQualityLevel level,
            int score,
            int activeDays,
            long studyMinutes,
            int completedPointCount,
            int answeredQuestionCount,
            int assessmentCount,
            List<String> limitations
    ) {}

    public record Capabilities(
            boolean canAnalyzeTrend,
            boolean canAnalyzeMastery,
            boolean canComparePreviousPeriod,
            boolean canDetectRepeatedWeakness
    ) {}
}
