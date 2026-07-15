package org.example.nursingtrainingbackend.modules.learningreport.vo;

import org.example.nursingtrainingbackend.modules.learningreport.dto.GeneratedLearningReport;
import org.example.nursingtrainingbackend.modules.learningreport.dto.LearningReportSnapshot;

import java.time.LocalDateTime;
import java.util.List;

public record LearningReportDetailVO(
        Long reportId,
        String status,
        String stage,
        Integer progress,
        String reportType,
        String reportMode,
        String title,
        LearningReportSnapshot.ReportPeriod period,
        LearningReportSnapshot.ReportDataQuality dataQuality,
        ReportEligibilityVO.Capabilities capabilities,
        LearningReportSnapshot.ReportOverview overview,
        String summary,
        String performanceLevel,
        List<GeneratedLearningReport.Highlight> highlights,
        List<GeneratedLearningReport.KnowledgeAnalysis> strengths,
        List<GeneratedLearningReport.KnowledgeAnalysis> weaknesses,
        List<GeneratedLearningReport.StudyPlanItem> studyPlan,
        String encouragement,
        String disclaimer,
        Boolean generatedByAi,
        LocalDateTime generatedAt,
        LocalDateTime dataUpdatedAt,
        String promptVersion,
        LocalDateTime nextFullReportEligibleAt,
        Failure failure,
        Integer retryAfterSeconds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record Failure(String code, String message, boolean retryable) {}
}
