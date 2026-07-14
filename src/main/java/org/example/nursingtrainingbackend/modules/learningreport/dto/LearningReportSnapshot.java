package org.example.nursingtrainingbackend.modules.learningreport.dto;

import org.example.nursingtrainingbackend.modules.learningreport.enums.DataQualityLevel;
import org.example.nursingtrainingbackend.modules.learningreport.enums.ReportMode;
import org.example.nursingtrainingbackend.modules.learningreport.enums.ReportType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 学习报告数据快照。
 *
 * 后端先将用户学习数据聚合成该对象，
 * 再序列化为 JSON 发送给 AI。
 *
 * 快照中不得包含用户密码、JWT、手机号、工号、
 * 身份证和真实患者信息。
 */
public record LearningReportSnapshot(

        /**
         * 快照结构版本。
         */
        String snapshotVersion,

        /**
         * 报告类型，例如周报。
         */
        ReportType reportType,

        /**
         * 报告分析模式。
         */
        ReportMode reportMode,

        /**
         * 报告统计周期。
         */
        ReportPeriod period,

        /**
         * 数据质量。
         */
        ReportDataQuality dataQuality,

        /**
         * 学习概览。
         */
        ReportOverview overview,

        /**
         * 优势知识点。
         */
        List<KnowledgeMastery> strengths,

        /**
         * 薄弱知识点。
         */
        List<KnowledgeMastery> weaknesses,

        /**
         * 可以推荐给用户的课程。
         */
        List<CandidateCourse> candidateCourses,

        /**
         * 当前报告的数据限制。
         */
        List<String> limitations
) {

    /**
     * 报告统计周期。
     */
    public record ReportPeriod(

            /**
             * 实际统计开始时间。
             */
            LocalDateTime start,

            /**
             * 实际统计结束时间。
             */
            LocalDateTime end,

            /**
             * 实际统计天数。
             */
            int actualDays,

            /**
             * 标准报告周期天数。
             * 周报通常为7天。
             */
            int expectedDays
    ) {
    }

    /**
     * 报告数据质量。
     */
    public record ReportDataQuality(

            /**
             * 数据质量等级。
             */
            DataQualityLevel level,

            /**
             * 数据质量分数，范围为0～100。
             */
            int score
    ) {
    }

    /**
     * 用户学习概览。
     */
    public record ReportOverview(

            /**
             * 实际产生学习记录的天数。
             */
            int activeDays,

            /**
             * 累计学习分钟数。
             */
            long studyMinutes,

            /**
             * 完成的知识点数量。
             */
            int completedPoints,

            /**
             * 参与考核的次数。
             */
            int assessmentCount,

            /**
             * 考核平均成绩。
             *
             * 没有考核数据时使用 null，
             * 不能使用0表示没有数据。
             */
            BigDecimal averageScore
    ) {
    }

    /**
     * 知识点掌握情况。
     */
    public record KnowledgeMastery(

            /**
             * 知识点ID。
             */
            Long knowledgePointId,

            /**
             * 知识点名称。
             */
            String name,

            /**
             * 掌握度，范围为0～100。
             */
            BigDecimal masteryScore,

            /**
             * 结论可信度：LOW、MEDIUM、HIGH。
             */
            String confidence,

            /**
             * 当前状态，例如 OBSERVING、MASTERED、WEAK。
             */
            String status,

            /**
             * 支持该结论的事实证据。
             */
            List<String> evidence
    ) {
    }

    /**
     * 允许AI推荐的候选课程。
     */
    public record CandidateCourse(

            /**
             * 课程ID。
             */
            Long courseId,

            /**
             * 课程名称。
             */
            String title,

            /**
             * 课程中的章节ID。
             */
            Long chapterId,

            /**
             * 课程知识点ID。
             */
            Long pointId,

            /**
             * 该课程匹配的薄弱知识点。
             */
            String matchedWeakness,

            /**
             * 预计学习分钟数。
             */
            int estimatedMinutes
    ) {
    }
}