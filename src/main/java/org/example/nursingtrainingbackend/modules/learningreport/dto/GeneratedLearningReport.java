package org.example.nursingtrainingbackend.modules.learningreport.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 已生成的学习报告内容。
 *
 * 规则报告和AI报告最终都转换成该结构。
 */
public record GeneratedLearningReport(

        /**
         * 报告标题。
         */
        String title,

        /**
         * 报告总结。
         */
        String summary,

        /**
         * 综合表现等级。
         */
        String performanceLevel,

        /**
         * 学习概览。
         */
        LearningReportSnapshot.ReportOverview overview,

        /**
         * 报告亮点。
         */
        List<Highlight> highlights,

        /**
         * 优势知识点。
         */
        List<KnowledgeAnalysis> strengths,

        /**
         * 薄弱知识点。
         */
        List<KnowledgeAnalysis> weaknesses,

        /**
         * 学习计划。
         */
        List<StudyPlanItem> studyPlan,

        /**
         * 鼓励语。
         */
        String encouragement,

        /**
         * 免责声明。
         */
        String disclaimer
) {

    /**
     * 报告亮点。
     */
    public record Highlight(
            String type,
            String title,
            String description,
            List<String> evidence
    ) {
    }

    /**
     * 知识点分析。
     */
    public record KnowledgeAnalysis(
            Long knowledgePointId,
            String name,
            BigDecimal masteryScore,
            String confidence,
            String status,
            String analysis,
            List<String> evidence
    ) {
    }

    /**
     * 学习计划项。
     */
    public record StudyPlanItem(
            Integer sequence,
            LocalDate planDate,
            String title,
            String action,
            Integer estimatedMinutes,
            String reason,
            Navigation navigation
    ) {
    }

    /**
     * 前端跳转信息。
     */
    public record Navigation(
            String actionType,
            Long courseId,
            Long chapterId,
            Long pointId
    ) {
    }
}
