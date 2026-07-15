package org.example.nursingtrainingbackend.modules.learningreport.validator;

import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.config.properties.LearningReportProperties;
import org.example.nursingtrainingbackend.modules.learningreport.dto.GeneratedLearningReport;
import org.example.nursingtrainingbackend.modules.learningreport.dto.LearningReportSnapshot;
import org.example.nursingtrainingbackend.modules.learningreport.enums.ReportMode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 校验 AI 输出的字段、数量以及课程和知识点跳转 ID。
 */
@Component
@RequiredArgsConstructor
public class DefaultAiReportResponseValidator
        implements AiReportResponseValidator {

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_SUMMARY_LENGTH = 2000;
    private static final int MAX_ITEM_TEXT_LENGTH = 1000;
    private static final int MAX_TOTAL_PLAN_MINUTES = 420;

    private static final Pattern SCORE_OR_MINUTE_PATTERN =
            Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(分|分钟)");

    private static final List<String> FORBIDDEN_CLINICAL_PHRASES = List.of(
            "确诊为",
            "诊断为",
            "建议用药",
            "立即停药",
            "服用剂量",
            "开具处方",
            "调整医嘱",
            "替代医生",
            "直接进行临床操作"
    );

    private static final List<String> LONG_TERM_TREND_PHRASES = List.of(
            "长期上升",
            "长期下降",
            "长期学习趋势",
            "已经形成稳定习惯",
            "持续数月",
            "与上月相比",
            "与上周相比"
    );

    private final LearningReportProperties properties;

    @Override
    public void validate(
            GeneratedLearningReport report,
            LearningReportSnapshot snapshot
    ) {
        if (report == null) {
            invalid("AI返回的报告不能为空");
        }
        if (snapshot == null) {
            invalid("学习数据快照不能为空");
        }

        requireText(report.title(), "报告标题", MAX_TITLE_LENGTH);
        requireText(report.summary(), "报告总结", MAX_SUMMARY_LENGTH);
        requireText(report.encouragement(), "鼓励语", MAX_ITEM_TEXT_LENGTH);
        requireText(report.disclaimer(), "免责声明", MAX_ITEM_TEXT_LENGTH);

        if (!report.disclaimer().contains("培训")
                || !report.disclaimer().contains("临床")) {
            invalid("AI报告缺少非临床用途声明");
        }

        List<GeneratedLearningReport.KnowledgeAnalysis> strengths =
                safeList(report.strengths());
        List<GeneratedLearningReport.KnowledgeAnalysis> weaknesses =
                safeList(report.weaknesses());
        List<GeneratedLearningReport.StudyPlanItem> plans =
                safeList(report.studyPlan());

        if (strengths.size() > properties.generation().maxStrengths()) {
            invalid("AI返回的优势知识点数量超过限制");
        }
        if (weaknesses.size() > properties.generation().maxWeaknesses()) {
            invalid("AI返回的薄弱知识点数量超过限制");
        }
        if (plans.size() > properties.generation().maxPlanItems()) {
            invalid("AI返回的学习计划数量超过限制");
        }

        validateKnowledgeIds(strengths, snapshot);
        validateKnowledgeIds(weaknesses, snapshot);
        validateMasteryScores(strengths, snapshot);
        validateMasteryScores(weaknesses, snapshot);
        validatePlans(plans, snapshot);
        validateTotalPlanMinutes(plans);
        validateTextFacts(report, snapshot);
        validateMedicalSafety(report);
        validateOnboardingLanguage(report, snapshot);
    }

    private void validateKnowledgeIds(
            List<GeneratedLearningReport.KnowledgeAnalysis> items,
            LearningReportSnapshot snapshot
    ) {
        Set<Long> allowedPointIds = new HashSet<>();

        safeList(snapshot.strengths()).stream()
                .map(LearningReportSnapshot.KnowledgeMastery::knowledgePointId)
                .filter(Objects::nonNull)
                .forEach(allowedPointIds::add);

        safeList(snapshot.weaknesses()).stream()
                .map(LearningReportSnapshot.KnowledgeMastery::knowledgePointId)
                .filter(Objects::nonNull)
                .forEach(allowedPointIds::add);

        for (GeneratedLearningReport.KnowledgeAnalysis item : items) {
            if (item == null
                    || item.knowledgePointId() == null
                    || !allowedPointIds.contains(item.knowledgePointId())) {
                invalid("AI返回了快照中不存在的知识点ID");
            }
        }
    }

    private void validatePlans(
            List<GeneratedLearningReport.StudyPlanItem> plans,
            LearningReportSnapshot snapshot
    ) {
        List<LearningReportSnapshot.CandidateCourse> candidates =
                safeList(snapshot.candidateCourses());

        for (int index = 0; index < plans.size(); index++) {
            GeneratedLearningReport.StudyPlanItem plan = plans.get(index);

            if (plan == null) {
                invalid("学习计划项不能为空");
            }
            if (plan.sequence() == null || plan.sequence() != index + 1) {
                invalid("学习计划序号必须从1开始连续递增");
            }
            if (plan.planDate() == null
                    || plan.planDate().isBefore(LocalDate.now())) {
                invalid("学习计划日期不能为空或早于今天");
            }
            if (plan.estimatedMinutes() == null
                    || plan.estimatedMinutes() <= 0
                    || plan.estimatedMinutes() > 240) {
                invalid("单项学习计划时长必须在1到240分钟之间");
            }

            requireText(plan.title(), "学习计划标题", 200);
            requireText(plan.action(), "学习计划行动", MAX_ITEM_TEXT_LENGTH);
            requireText(plan.reason(), "学习计划原因", MAX_ITEM_TEXT_LENGTH);
            validateNavigation(plan.navigation(), candidates);
        }
    }

    /**
     * AI 只能复述后端计算的掌握度，不能修改分数。
     */
    private void validateMasteryScores(
            List<GeneratedLearningReport.KnowledgeAnalysis> items,
            LearningReportSnapshot snapshot
    ) {
        Map<Long, BigDecimal> allowedScores = new HashMap<>();

        for (LearningReportSnapshot.KnowledgeMastery mastery
                : safeList(snapshot.strengths())) {
            if (mastery.knowledgePointId() != null) {
                allowedScores.put(mastery.knowledgePointId(), mastery.masteryScore());
            }
        }
        for (LearningReportSnapshot.KnowledgeMastery mastery
                : safeList(snapshot.weaknesses())) {
            if (mastery.knowledgePointId() != null) {
                allowedScores.put(mastery.knowledgePointId(), mastery.masteryScore());
            }
        }

        for (GeneratedLearningReport.KnowledgeAnalysis item : items) {
            BigDecimal expected = allowedScores.get(item.knowledgePointId());
            BigDecimal actual = item.masteryScore();

            if (expected == null && actual != null) {
                invalid("AI返回了快照中不存在的掌握度");
            }
            if (expected != null
                    && (actual == null || expected.compareTo(actual) != 0)) {
                invalid("AI修改了知识点掌握度：" + item.knowledgePointId());
            }
        }
    }

    private void validateTotalPlanMinutes(
            List<GeneratedLearningReport.StudyPlanItem> plans
    ) {
        int totalMinutes = plans.stream()
                .filter(Objects::nonNull)
                .map(GeneratedLearningReport.StudyPlanItem::estimatedMinutes)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        if (totalMinutes > MAX_TOTAL_PLAN_MINUTES) {
            invalid("学习计划总时长不能超过" + MAX_TOTAL_PLAN_MINUTES + "分钟");
        }
    }

    /**
     * 检查报告文字中的“分”和“分钟”，避免 AI 编造新的成绩或时长。
     */
    private void validateTextFacts(
            GeneratedLearningReport report,
            LearningReportSnapshot snapshot
    ) {
        Set<String> allowedNumbers = new HashSet<>();

        allowedNumbers.add(normalizeNumber(
                BigDecimal.valueOf(snapshot.overview().studyMinutes())
        ));

        if (snapshot.overview().averageScore() != null) {
            allowedNumbers.add(normalizeNumber(snapshot.overview().averageScore()));
        }

        for (LearningReportSnapshot.KnowledgeMastery item
                : safeList(snapshot.strengths())) {
            if (item.masteryScore() != null) {
                allowedNumbers.add(normalizeNumber(item.masteryScore()));
            }
        }
        for (LearningReportSnapshot.KnowledgeMastery item
                : safeList(snapshot.weaknesses())) {
            if (item.masteryScore() != null) {
                allowedNumbers.add(normalizeNumber(item.masteryScore()));
            }
        }
        for (GeneratedLearningReport.StudyPlanItem plan
                : safeList(report.studyPlan())) {
            if (plan != null && plan.estimatedMinutes() != null) {
                allowedNumbers.add(normalizeNumber(
                        BigDecimal.valueOf(plan.estimatedMinutes())
                ));
            }
        }

        Matcher matcher = SCORE_OR_MINUTE_PATTERN.matcher(collectAllText(report));
        while (matcher.find()) {
            String number = normalizeNumber(new BigDecimal(matcher.group(1)));
            if (!allowedNumbers.contains(number)) {
                invalid("AI报告文字中出现了快照之外的成绩或时长：" + matcher.group());
            }
        }
    }

    private void validateMedicalSafety(GeneratedLearningReport report) {
        String text = collectAllText(report);

        for (String phrase : FORBIDDEN_CLINICAL_PHRASES) {
            if (text.contains(phrase)) {
                invalid("AI报告包含不允许的临床指导内容");
            }
        }
    }

    private void validateOnboardingLanguage(
            GeneratedLearningReport report,
            LearningReportSnapshot snapshot
    ) {
        if (snapshot.reportMode() != ReportMode.ONBOARDING) {
            return;
        }

        String text = collectAllText(report);
        for (String phrase : LONG_TERM_TREND_PHRASES) {
            if (text.contains(phrase)) {
                invalid("入门报告不能包含长期趋势结论");
            }
        }
    }

    private String collectAllText(GeneratedLearningReport report) {
        StringBuilder text = new StringBuilder();
        appendText(text, report.title());
        appendText(text, report.summary());
        appendText(text, report.encouragement());
        appendText(text, report.disclaimer());

        for (GeneratedLearningReport.Highlight item : safeList(report.highlights())) {
            if (item != null) {
                appendText(text, item.title());
                appendText(text, item.description());
            }
        }
        for (GeneratedLearningReport.KnowledgeAnalysis item
                : safeList(report.strengths())) {
            if (item != null) {
                appendText(text, item.name());
                appendText(text, item.analysis());
            }
        }
        for (GeneratedLearningReport.KnowledgeAnalysis item
                : safeList(report.weaknesses())) {
            if (item != null) {
                appendText(text, item.name());
                appendText(text, item.analysis());
            }
        }
        for (GeneratedLearningReport.StudyPlanItem item
                : safeList(report.studyPlan())) {
            if (item != null) {
                appendText(text, item.title());
                appendText(text, item.action());
                appendText(text, item.reason());
            }
        }

        return text.toString();
    }

    private void appendText(StringBuilder target, String value) {
        if (value != null && !value.isBlank()) {
            target.append(value).append('\n');
        }
    }

    private String normalizeNumber(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private void validateNavigation(
            GeneratedLearningReport.Navigation navigation,
            List<LearningReportSnapshot.CandidateCourse> candidates
    ) {
        if (navigation == null) {
            invalid("学习计划跳转信息不能为空");
        }

        requireText(navigation.actionType(), "跳转类型", 100);

        switch (navigation.actionType()) {
            case "OPEN_COURSE_POINT" -> {
                boolean matched = candidates.stream().anyMatch(candidate ->
                        Objects.equals(candidate.courseId(), navigation.courseId())
                                && Objects.equals(candidate.chapterId(), navigation.chapterId())
                                && Objects.equals(candidate.pointId(), navigation.pointId())
                );
                if (!matched) {
                    invalid("AI返回了候选集合中不存在的课程点跳转ID");
                }
            }
            case "OPEN_COURSE" -> {
                boolean matched = candidates.stream().anyMatch(candidate ->
                        Objects.equals(candidate.courseId(), navigation.courseId())
                );
                if (!matched) {
                    invalid("AI返回了候选集合中不存在的课程ID");
                }
            }
            case "OPEN_CONTINUE_COURSES", "OPEN_AVAILABLE_ASSESSMENTS" -> {
                if (navigation.courseId() != null
                        || navigation.chapterId() != null
                        || navigation.pointId() != null) {
                    invalid("通用跳转不允许携带课程或知识点ID");
                }
            }
            default -> invalid("AI返回了不支持的跳转类型");
        }
    }

    private void requireText(
            String value,
            String fieldName,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            invalid(fieldName + "不能为空");
        }
        if (value.length() > maxLength) {
            invalid(fieldName + "长度超过限制");
        }
    }

    private <T> List<T> safeList(List<T> source) {
        return source == null ? Collections.emptyList() : source;
    }

    private void invalid(String message) {
        throw new BusinessException(ErrorCode.AI_RESPONSE_INVALID, message);
    }
}
