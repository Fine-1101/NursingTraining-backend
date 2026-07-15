package org.example.nursingtrainingbackend.modules.learningreport.vo;

import java.time.LocalDateTime;

public record LearningReportListItemVO(
        Long reportId,
        String reportType,
        String reportMode,
        String status,
        String title,
        String summary,
        LocalDateTime periodStart,
        LocalDateTime periodEnd,
        String dataQualityLevel,
        LocalDateTime generatedAt
) {
}
