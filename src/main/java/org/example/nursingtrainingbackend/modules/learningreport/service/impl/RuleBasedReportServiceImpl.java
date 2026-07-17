package org.example.nursingtrainingbackend.modules.learningreport.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.config.properties.LearningReportProperties;
import org.example.nursingtrainingbackend.modules.learningreport.dto.GeneratedLearningReport;
import org.example.nursingtrainingbackend.modules.learningreport.dto.LearningReportSnapshot;
import org.example.nursingtrainingbackend.modules.learningreport.enums.DataQualityLevel;
import org.example.nursingtrainingbackend.modules.learningreport.enums.ReportMode;
import org.example.nursingtrainingbackend.modules.learningreport.service.RuleBasedReportService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 规则版学习报告生成服务实现。
 */
@Service
@RequiredArgsConstructor
public class RuleBasedReportServiceImpl
        implements RuleBasedReportService {

    private static final String DISCLAIMER =
            "本报告仅用于培训和复习辅助，"
                    + "不构成临床诊断、治疗或护理操作依据。";

    private final LearningReportProperties properties;
    /** 生成学习报告内容。 */

    @Override
    public GeneratedLearningReport generate(
            LearningReportSnapshot snapshot
    ) {
        if (snapshot == null) {
            throw new IllegalArgumentException(
                    "学习报告快照不能为空"
            );
        }

        LearningReportSnapshot.ReportOverview overview =
                snapshot.overview();

        if (overview == null) {
            throw new IllegalArgumentException(
                    "学习报告概览不能为空"
            );
        }

        String title = buildTitle(snapshot.reportMode());

        String summary = buildSummary(snapshot);

        String performanceLevel =
                resolvePerformanceLevel(snapshot);

        List<GeneratedLearningReport.Highlight> highlights =
                buildHighlights(snapshot);

        List<GeneratedLearningReport.KnowledgeAnalysis> strengths =
                buildStrengths(snapshot);

        List<GeneratedLearningReport.KnowledgeAnalysis> weaknesses =
                buildWeaknesses(snapshot);

        List<GeneratedLearningReport.StudyPlanItem> studyPlan =
                buildStudyPlan(snapshot);

        String encouragement =
                buildEncouragement(snapshot.reportMode());

        return new GeneratedLearningReport(
                title,
                summary,
                performanceLevel,
                overview,
                highlights,
                strengths,
                weaknesses,
                studyPlan,
                encouragement,
                DISCLAIMER
        );
    }

    /**
     * 根据报告模式生成标题。
     */
    private String buildTitle(ReportMode reportMode) {
        if (reportMode == ReportMode.GUIDANCE_ONLY) {
            return "学习起步报告";
        }

        if (reportMode == ReportMode.ONBOARDING) {
            return "入门学习报告";
        }

        return "本周个性化学习报告";
    }

    /**
     * 生成报告总结。
     */
    private String buildSummary(
            LearningReportSnapshot snapshot
    ) {
        LearningReportSnapshot.ReportOverview overview =
                snapshot.overview();

        StringBuilder summary = new StringBuilder();

        if (snapshot.reportMode() == ReportMode.GUIDANCE_ONLY) {
            summary.append("当前学习数据较少，");
            summary.append("完成更多课程学习后，");
            summary.append("系统将提供更完整的个性化分析。");

            return summary.toString();
        }

        summary.append(
                "本周期共有%d个活跃学习日，累计学习约%d分钟，完成%d个课程知识点"
                        .formatted(
                                overview.activeDays(),
                                overview.studyMinutes(),
                                overview.completedPoints()
                        )
        );

        if (overview.assessmentCount() > 0
                && overview.averageScore() != null) {
            summary.append(
                    "，完成%d次考核，平均成绩为%s分"
                            .formatted(
                                    overview.assessmentCount(),
                                    formatScore(
                                            overview.averageScore()
                                    )
                            )
            );
        } else {
            summary.append("，当前暂无已完成的考核数据");
        }

        summary.append("。");

        if (snapshot.reportMode() == ReportMode.ONBOARDING) {
            summary.append(
                    "由于当前处于入门学习阶段，"
                            + "报告结论属于初步分析。"
            );
        }

        return summary.toString();
    }

    /**
     * 计算综合表现等级。
     *
     * 这里依据的是数据质量和学习行为，
     * 不是对用户职业能力进行评价。
     */
    private String resolvePerformanceLevel(
            LearningReportSnapshot snapshot
    ) {
        if (snapshot.reportMode() == ReportMode.GUIDANCE_ONLY) {
            return "UNKNOWN";
        }

        if (snapshot.dataQuality() == null) {
            return "UNKNOWN";
        }

        DataQualityLevel level =
                snapshot.dataQuality().level();

        if (level == DataQualityLevel.HIGH) {
            return "GOOD";
        }

        if (level == DataQualityLevel.MEDIUM) {
            return "STABLE";
        }

        if (level == DataQualityLevel.LOW) {
            return "OBSERVING";
        }

        return "UNKNOWN";
    }

    /**
     * 生成学习亮点。
     */
    private List<GeneratedLearningReport.Highlight> buildHighlights(
            LearningReportSnapshot snapshot
    ) {
        LearningReportSnapshot.ReportOverview overview =
                snapshot.overview();

        List<GeneratedLearningReport.Highlight> highlights =
                new ArrayList<>();

        if (overview.activeDays() >= 3) {
            highlights.add(
                    new GeneratedLearningReport.Highlight(
                            "LEARNING_CONTINUITY",
                            "学习参与较积极",
                            "本周期已有%d个活跃学习日。"
                                    .formatted(
                                            overview.activeDays()
                                    ),
                            List.of(
                                    "活跃学习天数为%d天"
                                            .formatted(
                                                    overview.activeDays()
                                            )
                            )
                    )
            );
        }

        if (overview.completedPoints() > 0) {
            highlights.add(
                    new GeneratedLearningReport.Highlight(
                            "POINT_COMPLETION",
                            "学习内容持续推进",
                            "本周期已完成%d个课程知识点。"
                                    .formatted(
                                            overview.completedPoints()
                                    ),
                            List.of(
                                    "已完成课程知识点%d个"
                                            .formatted(
                                                    overview.completedPoints()
                                            )
                            )
                    )
            );
        }

        if (overview.assessmentCount() > 0
                && overview.averageScore() != null) {
            highlights.add(
                    new GeneratedLearningReport.Highlight(
                            "ASSESSMENT",
                            "已完成学习检验",
                            "本周期完成%d次考核，平均成绩%s分。"
                                    .formatted(
                                            overview.assessmentCount(),
                                            formatScore(
                                                    overview.averageScore()
                                            )
                                    ),
                            List.of(
                                    "考核次数为%d次"
                                            .formatted(
                                                    overview.assessmentCount()
                                            ),
                                    "平均成绩为%s分"
                                            .formatted(
                                                    formatScore(
                                                            overview.averageScore()
                                                    )
                                            )
                            )
                    )
            );
        }

        /*
         * 最多保留3个亮点。
         */
        return highlights.stream()
                .limit(3)
                .toList();
    }

    /**
     * 生成优势知识点。
     */
    private List<GeneratedLearningReport.KnowledgeAnalysis>
    buildStrengths(
            LearningReportSnapshot snapshot
    ) {
        List<LearningReportSnapshot.KnowledgeMastery> source =
                safeList(snapshot.strengths());

        int maxStrengths =
                properties.generation().maxStrengths();

        return source.stream()
                .sorted(
                        Comparator.comparing(
                                LearningReportSnapshot
                                        .KnowledgeMastery
                                        ::masteryScore,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )
                )
                .limit(maxStrengths)
                .map(this::toStrengthAnalysis)
                .toList();
    }

    /**
     * 将快照优势转换为报告优势。
     */
    private GeneratedLearningReport.KnowledgeAnalysis
    toStrengthAnalysis(
            LearningReportSnapshot.KnowledgeMastery mastery
    ) {
        String analysis;

        if (mastery.masteryScore() == null) {
            analysis =
                    "当前学习表现较好，建议继续保持并定期复习。";
        } else {
            analysis =
                    "当前掌握度为%s分，表现较好，建议通过周期复习保持学习效果。"
                            .formatted(
                                    formatScore(
                                            mastery.masteryScore()
                                    )
                            );
        }

        return new GeneratedLearningReport.KnowledgeAnalysis(
                mastery.knowledgePointId(),
                mastery.name(),
                mastery.masteryScore(),
                defaultString(
                        mastery.confidence(),
                        "LOW"
                ),
                defaultString(
                        mastery.status(),
                        "OBSERVING"
                ),
                analysis,
                safeList(mastery.evidence())
        );
    }

    /**
     * 生成薄弱知识点。
     */
    private List<GeneratedLearningReport.KnowledgeAnalysis>
    buildWeaknesses(
            LearningReportSnapshot snapshot
    ) {
        List<LearningReportSnapshot.KnowledgeMastery> source =
                safeList(snapshot.weaknesses());

        int maxWeaknesses =
                properties.generation().maxWeaknesses();

        return source.stream()
                .sorted(
                        Comparator.comparing(
                                LearningReportSnapshot
                                        .KnowledgeMastery
                                        ::masteryScore,
                                Comparator.nullsLast(
                                        Comparator.naturalOrder()
                                )
                        )
                )
                .limit(maxWeaknesses)
                .map(this::toWeaknessAnalysis)
                .toList();
    }

    /**
     * 将快照薄弱项转换为报告薄弱项。
     */
    private GeneratedLearningReport.KnowledgeAnalysis
    toWeaknessAnalysis(
            LearningReportSnapshot.KnowledgeMastery mastery
    ) {
        String analysis;

        if (mastery.masteryScore() == null) {
            analysis =
                    "当前相关学习数据有限，建议继续学习并完成对应练习。";
        } else {
            analysis =
                    "当前掌握度为%s分，建议优先复习相关内容并重新练习。"
                            .formatted(
                                    formatScore(
                                            mastery.masteryScore()
                                    )
                            );
        }

        return new GeneratedLearningReport.KnowledgeAnalysis(
                mastery.knowledgePointId(),
                mastery.name(),
                mastery.masteryScore(),
                defaultString(
                        mastery.confidence(),
                        "LOW"
                ),
                defaultString(
                        mastery.status(),
                        "OBSERVING"
                ),
                analysis,
                safeList(mastery.evidence())
        );
    }

    /**
     * 根据候选课程生成学习计划。
     */
    private List<GeneratedLearningReport.StudyPlanItem>
    buildStudyPlan(
            LearningReportSnapshot snapshot
    ) {
        List<LearningReportSnapshot.CandidateCourse> courses =
                safeList(snapshot.candidateCourses());

        int maxPlanItems =
                properties.generation().maxPlanItems();

        List<GeneratedLearningReport.StudyPlanItem> plan =
                new ArrayList<>();

        int sequence = 1;

        for (LearningReportSnapshot.CandidateCourse course
                : courses) {
            if (sequence > maxPlanItems) {
                break;
            }

            int estimatedMinutes =
                    course.estimatedMinutes() > 0
                            ? course.estimatedMinutes()
                            : 20;

            String reason;

            if (course.matchedWeakness() == null
                    || course.matchedWeakness().isBlank()) {
                reason = "用于巩固当前阶段的学习内容";
            } else {
                reason = "用于加强“%s”相关内容"
                        .formatted(
                                course.matchedWeakness()
                        );
            }

            plan.add(
                    new GeneratedLearningReport.StudyPlanItem(
                            sequence,
                            LocalDate.now().plusDays(sequence),
                            defaultString(
                                    course.title(),
                                    "继续课程学习"
                            ),
                            "学习对应课程内容，并完成相关练习",
                            estimatedMinutes,
                            reason,
                            new GeneratedLearningReport.Navigation(
                                    "OPEN_COURSE_POINT",
                                    course.courseId(),
                                    course.chapterId(),
                                    course.pointId()
                            )
                    )
            );

            sequence++;
        }

        /*
         * 没有候选课程时提供通用计划，
         * 但不编造课程ID。
         */
        if (plan.isEmpty()) {
            plan.add(
                    new GeneratedLearningReport.StudyPlanItem(
                            1,
                            LocalDate.now().plusDays(1),
                            "继续当前课程",
                            "继续完成正在学习的课程内容",
                            20,
                            "当前候选推荐课程较少，"
                                    + "建议先完成已有学习任务",
                            new GeneratedLearningReport.Navigation(
                                    "OPEN_CONTINUE_COURSES",
                                    null,
                                    null,
                                    null
                            )
                    )
            );

            if (snapshot.overview().assessmentCount() == 0
                    && maxPlanItems >= 2) {
                plan.add(
                        new GeneratedLearningReport.StudyPlanItem(
                                2,
                                LocalDate.now().plusDays(2),
                                "完成一次学习检测",
                                "完成当前课程对应的章节练习或考核",
                                15,
                                "增加有效练习数据，"
                                        + "帮助系统形成更准确的学习分析",
                                new GeneratedLearningReport.Navigation(
                                        "OPEN_AVAILABLE_ASSESSMENTS",
                                        null,
                                        null,
                                        null
                                )
                        )
                );
            }
        }

        return List.copyOf(plan);
    }

    /**
     * 生成鼓励语。
     */
    private String buildEncouragement(
            ReportMode reportMode
    ) {
        if (reportMode == ReportMode.GUIDANCE_ONLY) {
            return "完成第一次课程学习后，"
                    + "系统将逐步形成更适合你的学习建议。";
        }

        if (reportMode == ReportMode.ONBOARDING) {
            return "你已经迈出了学习的第一步，"
                    + "保持稳定节奏比一次学习很长时间更重要。";
        }

        return "继续保持当前学习节奏，"
                + "优先完成薄弱内容的复习和练习。";
    }

    /**
     * 格式化成绩，去除无意义的小数0。
     */
    private String formatScore(BigDecimal score) {
        if (score == null) {
            return "--";
        }

        return score.stripTrailingZeros().toPlainString();
    }

    /**
     * null集合转换为空集合。
     */
    private <T> List<T> safeList(List<T> source) {
        if (source == null) {
            return Collections.emptyList();
        }

        return source;
    }

    /**
     * 字符串为空时返回默认值。
     */
    private String defaultString(
            String value,
            String defaultValue
    ) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value;
    }
}
